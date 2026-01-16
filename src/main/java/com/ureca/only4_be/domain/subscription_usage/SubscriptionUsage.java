package com.ureca.only4_be.domain.subscription_usage;

import com.ureca.only4_be.domain.product.UnitType;
import com.ureca.only4_be.domain.common.BaseEntity;
import com.ureca.only4_be.domain.subscription.Subscription;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "subscription_usage")
public class SubscriptionUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_usage_to_subscription"))
    private Subscription subscription;

    @Column(name = "usage_year_month", nullable = false)
    private LocalDate usageYearMonth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "usage_type", nullable = false, columnDefinition = "usage_type_enum")
    private UsageType usageType;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "unit_type", nullable = false, columnDefinition = "unit_type_enum")
    private UnitType unitType;

    @PrePersist
    private void prePersist() {
        if (quantity == null) {
            quantity = BigDecimal.ZERO; // DDL DEFAULT 0
        }
    }
}
