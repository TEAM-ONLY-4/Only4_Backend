package com.ureca.only4_be.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MobilePlanSpecRepository extends JpaRepository<MobilePlanSpec, Long> {
    // 상품 리스트를 주면, 해당되는 모바일 스펙을 다 찾아옴
    List<MobilePlanSpec> findAllByProductIn(List<Product> products);
}