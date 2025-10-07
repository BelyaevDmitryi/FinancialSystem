package com.fs.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.fs.domain.User;

public interface UserRepository extends MongoRepository<User, String> {
}
