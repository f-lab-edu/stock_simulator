package com.portfolio2025.first.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public abstract class BaseRepositoryImpl<T, ID> implements BaseRepository<T, ID> {
    protected final EntityManager em;
    private final Class<T> clazz;

    public BaseRepositoryImpl(EntityManager em, Class<T> clazz) {
        this.em = em;
        this.clazz = clazz;
    }

    @Override
    public T save(T entity) {
        em.persist(entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(em.find(clazz, id));
    }

    @Override
    public List<T> findAll() {
        String qlString = "SELECT e FROM " + clazz.getSimpleName() + " e";
        return em.createQuery(qlString, clazz).getResultList();
    }

    @Override
    public void delete(T entity) {
        em.remove(entity);
    }

    @Override
    public void flush() {
        em.flush();
    }
}
