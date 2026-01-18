package com.ureca.only4_be.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TvSpecRepository extends JpaRepository<TvSpec, Long> {
    List<TvSpec> findAllByProductIn(List<Product> products);
}
