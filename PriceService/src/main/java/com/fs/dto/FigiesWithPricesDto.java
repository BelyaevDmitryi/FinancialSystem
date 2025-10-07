package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fs.domain.FigiWithPrice;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FigiesWithPricesDto {
    List<FigiWithPrice> figiWithPrices;
}
