package com.ureca.only4_be.domain.member_device;

import com.ureca.only4_be.domain.common.BaseEntity;
import com.ureca.only4_be.domain.device.Device;
import com.ureca.only4_be.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "member_device")
public class MemberDevice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_device_to_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_device_to_device"))
    private Device device;

    @Column(name = "purchase_price", nullable = false, precision = 18, scale = 0)
    private BigDecimal purchasePrice;

    @Column(name = "installment_months", nullable = false)
    private Short installmentMonths;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @PrePersist
    private void prePersist() {
        if (installmentMonths == null) {
            installmentMonths = 1; // DDL DEFAULT 1
        }
    }
}