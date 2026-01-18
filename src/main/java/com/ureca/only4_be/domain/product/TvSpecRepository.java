package com.ureca.only4_be.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TvSpecRepository extends JpaRepository<TvSpec, Long> {
    List<TvSpec> findAllByProductIn(List<Product> products);
}
