package com.fs.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class StockPriceMapper {

    public BigDecimal toBigDecimal(Quotation quotation) {
        return quotation.getUnits() == 0 && quotation.getNano() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(quotation.getUnits()).add(BigDecimal.valueOf(quotation.getNano(), 9));
    }

    public Quotation toQuotation(BigDecimal bigDecimal) {
        return Quotation.newBuilder()
                .setUnits(bigDecimal != null ? bigDecimal.longValue() : 0)
                .setNano(bigDecimal != null ? bigDecimal.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).intValue() : 0)
                .build();
    }
}
