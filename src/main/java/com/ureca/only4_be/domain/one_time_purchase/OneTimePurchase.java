package com.ureca.only4_be.domain.one_time_purchase;

import com.ureca.only4_be.domain.common.BaseEntity;
import com.ureca.only4_be.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "one_time_purchase")
public class OneTimePurchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_one_time_purchase_to_member"))
    private Member member;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "charge_type", nullable = false, columnDefinition = "charge_type_enum")
    private ChargeType chargeType;

    @Column(name = "charged_at", nullable = false)
    private LocalDateTime chargedAt;
}
