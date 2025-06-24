package com.portfolio2025.first.repository;

import com.portfolio2025.first.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final EntityManager em;

    @Override
    public Long save(User user) {
        em.persist(user);
        return user.getId();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public Optional<User> findByNameAndLocation(String name, String location) {
        String jpql = "SELECT u FROM User u WHERE u.name = :name AND u.location = :location";
        List<User> users = em.createQuery(jpql, User.class)
                .setParameter("name", name)
                .setParameter("location", location)
                .getResultList();
        return users.stream().findFirst();
    }

    @Override
    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }
}
