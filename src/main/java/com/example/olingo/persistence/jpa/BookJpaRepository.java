package com.example.olingo.persistence.jpa;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookJpaRepository extends JpaRepository<BookEntity, Long> {
    List<BookEntity> findByCategory(CategoryEntity category);
}
