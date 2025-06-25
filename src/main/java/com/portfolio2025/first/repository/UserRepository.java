package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.User;
import java.util.Optional;

public interface UserRepository extends BaseRepository<User, Long> {
    // 추가 조회 방식
    Optional<User> findByEmail(String email);
}

