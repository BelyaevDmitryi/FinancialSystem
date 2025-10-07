package com.fs.repository;

import org.springframework.data.keyvalue.repository.KeyValueRepository;
import com.fs.domain.FigiWithPrice;

public interface StockRepository extends KeyValueRepository<FigiWithPrice, String> {

}
