package com.fs.service;

import com.fs.domain.Account;
import com.fs.domain.Broker;
import com.fs.domain.User;
import com.fs.domain.UserBroker;
import com.fs.domain.UserBrokerAccount;
import com.fs.dto.*;
import com.fs.exception.BrokerAccountNotFoundException;
import com.fs.exception.UserNotFoundException;
import com.fs.repository.AccountRepository;
import com.fs.repository.BrokerRepository;
import com.fs.repository.UserBrokerAccountRepository;
import com.fs.repository.UserBrokerRepository;
import com.fs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerAccountService {

    private final UserBrokerAccountRepository userBrokerAccountRepository;
    private final UserBrokerRepository userBrokerRepository;
    private final AccountRepository accountRepository;
    private final BrokerRepository brokerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BrokerDto> getAllBrokers() {
        return brokerRepository.findAll().stream()
                .map(this::toBrokerDto)
                .toList();
    }

    /**
     * Список счетов пользователя у брокеров. Счета сгруппированы по связке UserBroker (список в сущности у брокера).
     */
    @Transactional(readOnly = true)
    public List<UserBrokerAccountDto> getUserBrokerAccounts(String userId) {
        Long userIdLong = parseUserId(userId);
        List<UserBroker> userBrokers = userBrokerRepository.findByUserIdOrderByCreatedAt(userIdLong);
        List<UserBrokerAccountDto> result = new ArrayList<>();
        for (UserBroker ub : userBrokers) {
            for (UserBrokerAccount uba : ub.getAccounts()) {
                result.add(toUserBrokerAccountDto(ub, uba));
            }
        }
        result.sort((a, b) -> Boolean.compare(b.isDefault(), a.isDefault()));
        return result;
    }

    /**
     * Счёт по умолчанию для пользователя и брокера (поле defaultAccount в UserBroker).
     */
    @Transactional(readOnly = true)
    public DefaultBrokerAccountDto getDefaultAccountForUserAndBroker(String userId, String brokerCode) {
        Long userIdLong = parseUserId(userId);
        UserBroker userBroker = userBrokerRepository.findByUserIdAndBroker_Code(userIdLong, brokerCode)
                .orElseThrow(() -> new BrokerAccountNotFoundException(
                        "Брокер " + brokerCode + " не подключён или счета не добавлены."));
        Account defaultAccount = userBroker.getDefaultAccount();
        if (defaultAccount == null) {
            throw new BrokerAccountNotFoundException(
                    "Счёт по умолчанию для брокера " + brokerCode + " не выбран. Выберите счёт в профиле.");
        }
        return new DefaultBrokerAccountDto(defaultAccount.getExternalAccountId());
    }

    @Transactional
    public UserBrokerAccountDto addAccount(String userId, CreateUserBrokerAccountDto dto) {
        Long userIdLong = parseUserId(userId);
        User user = userRepository.findById(userIdLong)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        Broker broker = brokerRepository.findByCode(dto.getBrokerCode())
                .orElseThrow(() -> new IllegalArgumentException("Брокер с кодом " + dto.getBrokerCode() + " не найден"));

        UserBroker userBroker = userBrokerRepository.findByUserIdAndBrokerId(userIdLong, broker.getId())
                .orElseGet(() -> {
                    UserBroker ub = new UserBroker();
                    ub.setUser(user);
                    ub.setBroker(broker);
                    return userBrokerRepository.save(ub);
                });

        Account account = accountRepository.findByBrokerIdAndExternalAccountId(broker.getId(), dto.getExternalAccountId())
                .orElseGet(() -> {
                    Account newAccount = new Account();
                    newAccount.setBroker(broker);
                    newAccount.setExternalAccountId(dto.getExternalAccountId().trim());
                    newAccount.setDisplayName(dto.getDisplayName() != null ? dto.getDisplayName().trim() : null);
                    return accountRepository.save(newAccount);
                });

        if (userBrokerAccountRepository.existsByUserBrokerIdAndAccountId(userBroker.getId(), account.getId())) {
            throw new IllegalArgumentException("Этот счёт уже привязан к брокеру");
        }

        if (dto.isDefault()) {
            userBrokerAccountRepository.clearDefaultForUserBroker(userBroker.getId());
            userBroker.setDefaultAccount(account);
            userBrokerRepository.save(userBroker);
        }

        UserBrokerAccount uba = new UserBrokerAccount();
        uba.setUserBroker(userBroker);
        uba.setAccount(account);
        uba.setDefault(dto.isDefault());
        uba = userBrokerAccountRepository.save(uba);
        log.info("Добавлен счёт {} у брокера {} для пользователя {}", dto.getExternalAccountId(), dto.getBrokerCode(), userId);
        return toUserBrokerAccountDto(userBroker, uba);
    }

    @Transactional
    public UserBrokerAccountDto updateAccount(String userId, Long userBrokerAccountId, UpdateUserBrokerAccountDto dto) {
        Long userIdLong = parseUserId(userId);
        UserBrokerAccount uba = userBrokerAccountRepository.findById(userBrokerAccountId)
                .orElseThrow(() -> new BrokerAccountNotFoundException("Счёт не найден"));
        if (!uba.getUserBroker().getUser().getId().equals(userIdLong)) {
            throw new BrokerAccountNotFoundException("Счёт не принадлежит пользователю");
        }
        UserBroker userBroker = uba.getUserBroker();
        Account account = uba.getAccount();

        if (dto.getDisplayName() != null) {
            account.setDisplayName(dto.getDisplayName().trim());
            accountRepository.save(account);
        }
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            userBrokerAccountRepository.clearDefaultForUserBroker(userBroker.getId());
            uba.setDefault(true);
            userBroker.setDefaultAccount(account);
            userBrokerRepository.save(userBroker);
        }

        uba = userBrokerAccountRepository.save(uba);
        return toUserBrokerAccountDto(userBroker, uba);
    }

    @Transactional
    public void deleteAccount(String userId, Long userBrokerAccountId) {
        Long userIdLong = parseUserId(userId);
        UserBrokerAccount uba = userBrokerAccountRepository.findById(userBrokerAccountId)
                .orElseThrow(() -> new BrokerAccountNotFoundException("Счёт не найден"));
        if (!uba.getUserBroker().getUser().getId().equals(userIdLong)) {
            throw new BrokerAccountNotFoundException("Счёт не принадлежит пользователю");
        }
        UserBroker userBroker = uba.getUserBroker();
        if (userBroker.getDefaultAccount() != null && userBroker.getDefaultAccount().getId().equals(uba.getAccount().getId())) {
            userBroker.setDefaultAccount(null);
            userBrokerRepository.save(userBroker);
        }
        userBrokerAccountRepository.delete(uba);
        log.info("Отвязан счёт {} для пользователя {}", userBrokerAccountId, userId);
    }

    private BrokerDto toBrokerDto(Broker broker) {
        return new BrokerDto(broker.getId(), broker.getCode(), broker.getName());
    }

    private UserBrokerAccountDto toUserBrokerAccountDto(UserBroker ub, UserBrokerAccount uba) {
        Account a = uba.getAccount();
        return new UserBrokerAccountDto(
                uba.getId(),
                ub.getUser().getId(),
                toBrokerDto(ub.getBroker()),
                a.getExternalAccountId(),
                a.getDisplayName(),
                uba.isDefault(),
                uba.getCreatedAt()
        );
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный ID пользователя: " + userId);
        }
    }
}
