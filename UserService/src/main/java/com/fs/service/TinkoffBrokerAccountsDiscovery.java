package com.fs.service;

import com.fs.dto.DiscoveredBrokerAccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.AccountStatus;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Получение списка счетов Tinkoff Invest по API-токену (ответ API; токен нигде не сохраняется).
 */
@Component
@Slf4j
public class TinkoffBrokerAccountsDiscovery {

    /**
     * @param token   секрет пользователя для Invest API (readonly или sandbox).
     * @param sandbox true — только песочница ({@link InvestApi#createSandbox}).
     */
    public List<DiscoveredBrokerAccountDto> discover(String token, boolean sandbox) {
        String trimmed = token == null ? "" : token.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("API-токен не может быть пустым");
        }
        InvestApi api = sandbox ? InvestApi.createSandbox(trimmed) : InvestApi.createReadonly(trimmed);
        try {
            List<Account> accounts = api.getUserService().getAccountsSync();
            List<DiscoveredBrokerAccountDto> out = new ArrayList<>();
            for (Account a : accounts) {
                if (a.getStatus() == AccountStatus.ACCOUNT_STATUS_CLOSED) {
                    continue;
                }
                String label = (a.getName() != null && !a.getName().isBlank()) ? a.getName().trim() : null;
                out.add(new DiscoveredBrokerAccountDto(
                        a.getId(),
                        label,
                        a.getType().name(),
                        a.getStatus().name()
                ));
            }
            return out;
        } catch (Exception e) {
            log.warn("Не удалось получить счета Tinkoff Invest: {}", e.toString());
            throw new IllegalArgumentException(
                    "Не удалось получить список счетов. Проверьте токен, права доступа и режим (биржа или песочница).", e);
        } finally {
            try {
                api.destroy(3);
            } catch (Exception closeEx) {
                log.debug("InvestApi.destroy: {}", closeEx.toString());
            }
        }
    }
}
