package com.fs.service;

import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.dto.BrokerOrderDto;
import com.fs.feignclient.BrokerIntegrationServiceClient;
import com.fs.feignclient.UserServiceClient;
import com.fs.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Периодическая синхронизация PENDING-ордеров со статусом заявки у брокера.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSyncService {

    private final OrderRepository orderRepository;
    private final BrokerIntegrationServiceClient brokerIntegrationServiceClient;
    private final UserServiceClient userServiceClient;
    private final JournalFillPublisher journalFillPublisher;

    @Value("${broker.default-account-id:}")
    private String defaultAccountId;

    @Value("${broker.default-broker:TINKOFF}")
    private String defaultBrokerCode;

    @Value("${broker.integration.enabled:true}")
    private boolean brokerIntegrationEnabled;

    @Scheduled(fixedDelayString = "${broker.order-sync.fixed-delay-ms:5000}")
    @Transactional
    public void syncPendingOrders() {
        if (!brokerIntegrationEnabled) {
            return;
        }
        List<Order> pendingOrders = orderRepository.findByStatusAndBrokerOrderIdIsNotNull(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            return;
        }
        log.debug("Синхронизация {} PENDING-ордеров с брокером", pendingOrders.size());
        for (Order order : pendingOrders) {
            try {
                syncOrder(order);
            } catch (Exception e) {
                log.error("Ошибка синхронизации ордера id={}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    void syncOrder(Order order) {
        String brokerOrderId = BrokerOrderIdResolver.resolve(order);
        if (brokerOrderId == null) {
            return;
        }
        OrderStatus previousStatus = order.getStatus();
        String accountId = getAccountIdForUser(String.valueOf(order.getUserId()));
        BrokerOrderDto brokerOrder = brokerIntegrationServiceClient.getOrderStatus(
                accountId, brokerOrderId, null);
        if (brokerOrder == null || brokerOrder.getStatus() == null) {
            return;
        }
        BrokerOrderStatusMapper.applyBrokerStatus(order, brokerOrder);
        orderRepository.save(order);
        if (order.getStatus() == OrderStatus.EXECUTED && previousStatus != OrderStatus.EXECUTED) {
            journalFillPublisher.publishFill(order);
        }
        log.info("Ордер id={} синхронизирован: brokerStatus={}, localStatus={}",
                order.getId(), order.getBrokerStatus(), order.getStatus());
    }

    private String getAccountIdForUser(String userId) {
        if (defaultAccountId != null && !defaultAccountId.isEmpty()) {
            return defaultAccountId;
        }
        var dto = userServiceClient.getDefaultBrokerAccount(userId, defaultBrokerCode);
        if (dto == null || dto.getExternalAccountId() == null || dto.getExternalAccountId().isBlank()) {
            throw new IllegalStateException(
                    "Счёт по умолчанию для брокера " + defaultBrokerCode + " не привязан.");
        }
        return dto.getExternalAccountId();
    }
}
