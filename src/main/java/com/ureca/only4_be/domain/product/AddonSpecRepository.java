package com.ureca.only4_be.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddonSpecRepository extends JpaRepository<AddonSpec, Long> {
    // 상품 리스트를 주면, 해당되는 부가서비스 스펙을 다 찾아옴
    List<AddonSpec> findAllByProductIn(List<Product> products);
}
