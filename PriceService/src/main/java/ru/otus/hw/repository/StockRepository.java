package ru.otus.hw.repository;

import org.springframework.data.keyvalue.repository.KeyValueRepository;
import ru.otus.hw.domain.FigiWithPrice;

public interface StockRepository extends KeyValueRepository<FigiWithPrice, String> {

}
