package com.fs.service;

import com.fs.domain.BrokerOrderType;
import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.AmendOrderDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.exception.OrderNotFoundException;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.dto.CreateOrderDto;
import com.fs.dto.OrderDto;
import com.fs.dto.OrderStatsDto;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.UserServiceClient;
import com.fs.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final BrokerIntegrationServiceClient brokerIntegrationServiceClient;
    private final UserServiceClient userServiceClient;
    private final JournalFillPublisher journalFillPublisher;
    private final PaperFillSimulator paperFillSimulator;

    /** Только для тестов/локальной разработки. В продакшене оставьте пустым — счёт берётся из UserService по userId (свой счёт для каждого пользователя). */
    @Value("${broker.default-account-id:}")
    private String defaultAccountId;

    @Value("${broker.default-broker:TINKOFF}")
    private String defaultBrokerCode;

    @Value("${broker.integration.enabled:true}")
    private boolean brokerIntegrationEnabled;

    @Transactional
    public OrderDto createOrder(String userId, CreateOrderDto createOrderDto) {
        log.info("Создание ордера: userId={}, figi={}, type={}, brokerEnabled={}",
                userId, createOrderDto.getFigi(), createOrderDto.getType(), brokerIntegrationEnabled);
        
        Long userIdLong = parseUserId(userId);
        
        Order order = new Order();
        order.setUserId(userIdLong);
        order.setFigi(createOrderDto.getFigi());
        order.setType(createOrderDto.getType());
        order.setQuantity(createOrderDto.getQuantity());
        order.setPrice(createOrderDto.getPrice());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setComment(createOrderDto.getComment());
        order.setOrderType(resolveBrokerOrderType(createOrderDto.getOrderType()));
        order.setStopPrice(createOrderDto.getStopPrice());
        order.setPaper(Boolean.TRUE.equals(createOrderDto.getPaper()));

        Order savedOrder = orderRepository.save(order);
        log.info("Ордер создан с ID: {}, paper={}", savedOrder.getId(), savedOrder.isPaper());

        if (savedOrder.isPaper()) {
            OrderStatus statusBefore = savedOrder.getStatus();
            paperFillSimulator.applyPaperFill(savedOrder);
            orderRepository.save(savedOrder);
            publishFillIfNewlyExecuted(savedOrder, statusBefore);
            return convertToDto(savedOrder);
        }
        
        // Если включена интеграция с брокером, отправляем заявку на биржу
        if (brokerIntegrationEnabled) {
            try {
                String accountId = getAccountIdForUser(userId);
                log.debug("Отправка заявки на биржу для accountId: {}, FIGI: {}", accountId, createOrderDto.getFigi());
                BrokerOrderDto brokerOrder = sendOrderToBroker(accountId, createOrderDto);
                
                // Обновляем статус заявки на основе ответа брокера
                if (brokerOrder != null && brokerOrder.getOrderId() != null) {
                    savedOrder.setBrokerOrderId(brokerOrder.getOrderId());
                    savedOrder.setBrokerCode(defaultBrokerCode);
                    OrderStatus statusBeforeBroker = savedOrder.getStatus();
                    BrokerOrderStatusMapper.applyBrokerStatus(savedOrder, brokerOrder);
                    orderRepository.save(savedOrder);
                    publishFillIfNewlyExecuted(savedOrder, statusBeforeBroker);
                    log.info("Заявка отправлена на биржу. brokerOrderId={}, localStatus={}",
                            brokerOrder.getOrderId(), savedOrder.getStatus());
                } else {
                    log.warn("Брокерский сервис вернул пустой ответ или orderId отсутствует");
                }
            } catch (Exception e) {
                log.error("Ошибка при отправке заявки на биржу: {}", e.getMessage(), e);
                // Не прерываем создание заявки в системе, но логируем ошибку
                // Можно добавить статус "PENDING_BROKER_ERROR" для таких случаев
            }
        }
        
        return convertToDto(savedOrder);
    }
    
    /**
     * Отправить заявку на биржу через брокера
     */
    private BrokerOrderDto sendOrderToBroker(String accountId, CreateOrderDto createOrderDto) {
        try {
            CreateBrokerOrderDto brokerOrderDto = new CreateBrokerOrderDto();
            brokerOrderDto.setFigi(createOrderDto.getFigi());
            // Преобразуем BigDecimal в Long (количество лотов)
            // Округляем до ближайшего целого числа
            brokerOrderDto.setQuantity(createOrderDto.getQuantity().longValue());
            brokerOrderDto.setPrice(createOrderDto.getPrice());
            brokerOrderDto.setDirection(createOrderDto.getType().name());
            BrokerOrderType brokerOrderType = resolveBrokerOrderType(createOrderDto.getOrderType());
            brokerOrderDto.setOrderType(brokerOrderType.name());
            brokerOrderDto.setStopPrice(createOrderDto.getStopPrice());
            brokerOrderDto.setComment(createOrderDto.getComment());
            
            log.debug("Вызов brokerIntegrationServiceClient.placeOrder с accountId: {}, FIGI: {}, quantity: {}, price: {}", 
                    accountId, brokerOrderDto.getFigi(), brokerOrderDto.getQuantity(), brokerOrderDto.getPrice());
            
            return brokerIntegrationServiceClient.placeOrder(accountId, brokerOrderDto, null);
        } catch (Exception e) {
            log.error("Исключение при вызове brokerIntegrationServiceClient.placeOrder: {}", e.getMessage(), e);
            throw e; // Пробрасываем исключение дальше для обработки в createOrder
        }
    }
    
    /**
     * Получить accountId (счёт у брокера) для пользователя.
     * В продакшене всегда берётся из UserService по userId (свой счёт для каждого пользователя).
     * Для тестов/локальной разработки можно задать broker.default-account-id в конфиге.
     */
    private String getAccountIdForUser(String userId) {
        if (defaultAccountId != null && !defaultAccountId.isEmpty()) {
            log.debug("Используется счёт из конфига (broker.default-account-id): {}", defaultAccountId);
            return defaultAccountId;
        }
        var dto = userServiceClient.getDefaultBrokerAccount(userId, defaultBrokerCode);
        if (dto == null || dto.getExternalAccountId() == null || dto.getExternalAccountId().isBlank()) {
            throw new IllegalStateException(
                    "Счёт по умолчанию для брокера " + defaultBrokerCode + " не привязан. Добавьте счёт в профиле.");
        }
        log.debug("Счёт для пользователя {} у брокера {}: {}", userId, defaultBrokerCode, dto.getExternalAccountId());
        return dto.getExternalAccountId();
    }

    public List<OrderDto> getUserOrders(String userId) {
        log.info("Получение ордеров для пользователя: {}", userId);
        Long userIdLong = parseUserId(userId);
        return orderRepository.findByUserId(userIdLong).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getUserOrdersByStatus(String userId, OrderStatus status) {
        log.info("Получение ордеров для пользователя: {} со статусом: {}", userId, status);
        Long userIdLong = parseUserId(userId);
        return orderRepository.findByUserIdAndStatus(userIdLong, status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto executeOrder(String orderId, String userId) {
        log.info("Исполнение ордера: {} для пользователя: {}", orderId, userId);
        Long orderIdLong = parseOrderId(orderId);
        Long userIdLong = parseUserId(userId);
        Order order = orderRepository.findById(orderIdLong)
                .orElseThrow(() -> new OrderNotFoundException("Ордер не найден"));
        if (!order.getUserId().equals(userIdLong)) {
            throw new OrderNotFoundException("Ордер не найден");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Ордер уже обработан. Текущий статус: " + order.getStatus());
        }

        // Если включена интеграция с брокером, проверяем статус заявки на бирже
        if (brokerIntegrationEnabled) {
            try {
                String brokerOrderId = BrokerOrderIdResolver.resolve(order);
                if (brokerOrderId != null) {
                    String accountId = getAccountIdForUser(String.valueOf(order.getUserId()));
                    BrokerOrderDto brokerOrder = brokerIntegrationServiceClient.getOrderStatus(accountId, brokerOrderId, null);
                    
                    if (brokerOrder != null) {
                        BrokerOrderStatusMapper.applyBrokerStatus(order, brokerOrder);
                        if (order.getStatus() == OrderStatus.EXECUTED) {
                            log.info("Заявка на бирже исполнена. brokerOrderId={}", brokerOrderId);
                        } else {
                            log.warn("Заявка на бирже еще не исполнена. Статус: {}", brokerOrder.getStatus());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке статуса заявки на бирже: {}", e.getMessage(), e);
                // Продолжаем выполнение, устанавливая статус вручную
                order.setStatus(OrderStatus.EXECUTED);
                order.setExecutedAt(LocalDateTime.now());
            }
        } else {
            order.setStatus(OrderStatus.EXECUTED);
            order.setExecutedAt(LocalDateTime.now());
        }
        
        Order savedOrder = orderRepository.save(order);
        publishFillIfNewlyExecuted(savedOrder, OrderStatus.PENDING);
        log.info("Ордер исполнен: {}", orderId);
        
        return convertToDto(savedOrder);
    }

    private void publishFillIfNewlyExecuted(Order order, OrderStatus previousStatus) {
        if (order.getStatus() == OrderStatus.EXECUTED && previousStatus != OrderStatus.EXECUTED) {
            journalFillPublisher.publishFill(order);
        }
    }
    
    private BrokerOrderType resolveBrokerOrderType(BrokerOrderType orderType) {
        return orderType != null ? orderType : BrokerOrderType.LIMIT;
    }

    @Transactional
    public OrderDto cancelOrder(String orderId, String userId) {
        log.info("Отмена ордера: {} для пользователя: {}", orderId, userId);
        Long orderIdLong = parseOrderId(orderId);
        Long userIdLong = parseUserId(userId);
        Order order = orderRepository.findById(orderIdLong)
                .orElseThrow(() -> new OrderNotFoundException("Ордер не найден"));
        if (!order.getUserId().equals(userIdLong)) {
            throw new OrderNotFoundException("Ордер не найден");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Невозможно отменить ордер. Текущий статус: " + order.getStatus());
        }

        if (order.isPaper()) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setBrokerStatus("CANCELLED");
        } else if (brokerIntegrationEnabled) {
            String brokerOrderId = BrokerOrderIdResolver.resolve(order);
            if (brokerOrderId != null) {
                String accountId = getAccountIdForUser(String.valueOf(order.getUserId()));
                String brokerCode = resolveBrokerCode(order);
                brokerIntegrationServiceClient.cancelOrder(accountId, brokerOrderId, brokerCode);
                BrokerOrderDto brokerOrder = brokerIntegrationServiceClient.getOrderStatus(
                        accountId, brokerOrderId, brokerCode);
                BrokerOrderStatusMapper.applyBrokerStatus(order, brokerOrder);
                if (order.getStatus() == OrderStatus.PENDING) {
                    throw new IllegalStateException("Брокер не подтвердил отмену заявки");
                }
                log.info("Заявка отменена на бирже. brokerOrderId={}", brokerOrderId);
            } else {
                order.setStatus(OrderStatus.CANCELLED);
                order.setBrokerStatus("CANCELLED");
            }
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            order.setBrokerStatus("CANCELLED");
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Ордер отменен: {}", orderId);

        return convertToDto(savedOrder);
    }

    private String resolveBrokerCode(Order order) {
        if (order.getBrokerCode() != null && !order.getBrokerCode().isBlank()) {
            return order.getBrokerCode();
        }
        return defaultBrokerCode;
    }

    @Transactional
    public OrderDto amendOrder(String orderId, String userId, AmendOrderDto amendOrderDto) {
        log.info("Изменение ордера: {} для пользователя: {}", orderId, userId);
        Long orderIdLong = parseOrderId(orderId);
        Long userIdLong = parseUserId(userId);
        Order order = orderRepository.findById(orderIdLong)
                .orElseThrow(() -> new OrderNotFoundException("Ордер не найден"));
        if (!order.getUserId().equals(userIdLong)) {
            throw new OrderNotFoundException("Ордер не найден");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Невозможно изменить ордер. Текущий статус: " + order.getStatus());
        }
        BrokerOrderType orderType = resolveBrokerOrderType(order.getOrderType());
        if (orderType != BrokerOrderType.LIMIT) {
            throw new IllegalStateException("Изменение цены поддерживается только для LIMIT-заявок");
        }

        order.setPrice(amendOrderDto.getPrice());

        if (brokerIntegrationEnabled) {
            String brokerOrderId = BrokerOrderIdResolver.resolve(order);
            if (brokerOrderId != null) {
                String accountId = getAccountIdForUser(userId);
                AmendBrokerOrderDto brokerAmend = new AmendBrokerOrderDto(amendOrderDto.getPrice(), null);
                BrokerOrderDto brokerOrder = brokerIntegrationServiceClient.amendOrder(
                        accountId, brokerOrderId, brokerAmend, null);
                if (brokerOrder != null) {
                    if (brokerOrder.getPrice() != null) {
                        order.setPrice(brokerOrder.getPrice());
                    }
                    if (brokerOrder.getStatus() != null) {
                        order.setBrokerStatus(brokerOrder.getStatus());
                    }
                }
            }
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Ордер изменён: {}", orderId);
        return convertToDto(savedOrder);
    }

    public OrderDto getOrder(String orderId, String userId) {
        Long orderIdLong = parseOrderId(orderId);
        Long userIdLong = parseUserId(userId);
        Order order = orderRepository.findById(orderIdLong)
                .orElseThrow(() -> new OrderNotFoundException("Ордер не найден"));
        if (!order.getUserId().equals(userIdLong)) {
            throw new OrderNotFoundException("Ордер не найден");
        }
        return convertToDto(order);
    }
    
    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат userId: " + userId);
        }
    }
    
    private Long parseOrderId(String orderId) {
        try {
            return Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат orderId: " + orderId);
        }
    }

    /**
     * Статистика ордеров только для текущего пользователя (по X-User-Id).
     */
    public OrderStatsDto getOrderStatsForUser(String userId) {
        Long userIdLong = parseUserId(userId);
        List<Order> userOrders = orderRepository.findByUserId(userIdLong);
        long totalOrders = userOrders.size();
        Map<String, Long> ordersByStatus = userOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getStatus().name(),
                        Collectors.counting()
                ));
        return new OrderStatsDto(totalOrders, ordersByStatus);
    }

    /**
     * Глобальная статистика ордеров (для админ-панели). В продакшене вызывать только с ролью ROLE_ADMIN.
     */
    public OrderStatsDto getOrderStats(String rolesHeader) {
        if (rolesHeader == null || !rolesHeader.contains("ROLE_ADMIN")) {
            throw new IllegalStateException("Доступ запрещён. Требуется роль администратора.");
        }
        List<Order> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();
        Map<String, Long> ordersByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getStatus().name(),
                        Collectors.counting()
                ));
        return new OrderStatsDto(totalOrders, ordersByStatus);
    }

    private OrderDto convertToDto(Order order) {
        return new OrderDto(
                String.valueOf(order.getId()),
                String.valueOf(order.getUserId()),
                order.getFigi(),
                order.getType(),
                order.getQuantity(),
                order.getPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getExecutedAt(),
                order.getComment(),
                order.getBrokerOrderId(),
                order.getBrokerCode(),
                order.getOrderType(),
                order.getStopPrice(),
                order.getBrokerStatus(),
                order.isPaper()
        );
    }
}
