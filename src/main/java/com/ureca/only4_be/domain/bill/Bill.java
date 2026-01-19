package com.ureca.only4_be.domain.bill;

import com.ureca.only4_be.domain.common.BaseEntity;
import com.ureca.only4_be.domain.member.Member;
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
@Table(name = "bill")
public class Bill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_to_member"))
    private Member member;

    @Column(name = "billing_year_month", nullable = false)
    private LocalDate billingYearMonth;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "vat", nullable = false, precision = 18, scale = 0)
    private BigDecimal vat;

    @Column(name = "unpaid_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal unpaidAmount;

    @Column(name = "total_discount_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalDiscountAmount;

    @Column(name = "total_billed_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalBilledAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "approval_expected_date")
    private LocalDate approvalExpectedDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "bill_send_status", columnDefinition = "bill_send_status_enum")
    private BillSendStatus billSendStatus;

    @Column(name = "payment_owner_name_snapshot", nullable = false, length = 100)
    private String paymentOwnerNameSnapshot;

    @Column(name = "payment_name_snapshot", nullable = false, length = 100)
    private String paymentNameSnapshot;

    @Column(name = "payment_number_snapshot", nullable = false, length = 100)
    private String paymentNumberSnapshot;

    @PrePersist
    private void prePersist() {
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (vat == null) vat = BigDecimal.ZERO;
        if (unpaidAmount == null) unpaidAmount = BigDecimal.ZERO;
        if (totalDiscountAmount == null) totalDiscountAmount = BigDecimal.ZERO;
        if (totalBilledAmount == null) totalBilledAmount = BigDecimal.ZERO;
    }
    public void changeStatus(BillSendStatus status) {
        this.billSendStatus = status;
    }
}
