package com.ureca.only4_be.domain.subscription_discount;

import com.ureca.only4_be.domain.common.BaseEntity;
import com.ureca.only4_be.domain.discount_policy.DiscountPolicy;
import com.ureca.only4_be.domain.subscription.Subscription;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "subscription_discount")
public class SubscriptionDiscount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discount_policy_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_discount_to_discount_policy"))
    private DiscountPolicy discountPolicy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_discount_to_subscription"))
    private Subscription subscription;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "contract_status", nullable = false, columnDefinition = "contract_status_enum")
    private ContractStatus contractStatus;
}
