package com.ureca.only4_be.domain.product;

import com.ureca.only4_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "mobile_plan_spec")
public class MobilePlanSpec extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mobile_plan_spec_to_product"))
    private Product product;

    @Column(name = "data_gb", precision = 10, scale = 3)
    private BigDecimal dataGb;

    @Column(name = "voice_minutes")
    private Integer voiceMinutes;

    @Column(name = "sms_count")
    private Integer smsCount;
}
