package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl extends BaseRepositoryImpl<User, Long> implements UserRepository {

    public UserRepositoryImpl(EntityManager em) {
        super(em, User.class);
    }


    // 추가 조회 방식
    @Override
    public Optional<User> findByEmail(String email) {
        String ql = "SELECT u FROM User u WHERE u.email = :email";
        List<User> result = em.createQuery(ql, User.class)
                .setParameter("email", email)
                .getResultList();
        return result.stream().findFirst();
    }
}

