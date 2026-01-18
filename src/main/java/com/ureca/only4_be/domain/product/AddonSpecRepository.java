package com.ureca.only4_be.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddonSpecRepository extends JpaRepository<AddonSpec, Long> {
    // 상품 리스트를 주면, 해당되는 부가서비스 스펙을 다 찾아옴
    List<AddonSpec> findAllByProductIn(List<Product> products);
}
