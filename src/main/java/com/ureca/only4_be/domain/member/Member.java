package com.ureca.only4_be.domain.member;

import com.ureca.only4_be.domain.receipt.PaymentMethod;
import com.ureca.only4_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 255)
    private String phoneNumber;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "member_grade", nullable = false, columnDefinition = "member_grade_enum")
    private MemberGrade memberGrade;

    @Column(name = "payment_owner_name", nullable = false, length = 100)
    private String paymentOwnerName;

    @Column(name = "payment_name", nullable = false, length = 100)
    private String paymentName;

    @Column(name = "payment_number", nullable = false, length = 100)
    private String paymentNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_method", nullable = false, columnDefinition = "payment_method_enum")
    private PaymentMethod paymentMethod;

    @Column(name = "notification_day_of_month")
    private Short notificationDayOfMonth;

    @Column(name = "do_not_disturb_start_time")
    private LocalTime doNotDisturbStartTime;

    @Column(name = "do_not_disturb_end_time")
    private LocalTime doNotDisturbEndTime;
}