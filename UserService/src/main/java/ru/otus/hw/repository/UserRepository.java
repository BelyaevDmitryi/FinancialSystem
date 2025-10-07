package ru.otus.hw.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.otus.hw.domain.User;

public interface UserRepository extends MongoRepository<User, String> {
}
