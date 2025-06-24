package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Long save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByNameAndLocation(String name, String location);
    List<User> findAll();
}
