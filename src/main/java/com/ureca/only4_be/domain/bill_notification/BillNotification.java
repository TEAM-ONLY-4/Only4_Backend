package com.ureca.only4_be.domain.bill_notification;

import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.common.BaseEntity;
import com.ureca.only4_be.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "bill_notification")
public class BillNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_notification_to_bill"))
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel", nullable = false, columnDefinition = "bill_channel_enum")
    private BillChannel channel;

//    @Enumerated(EnumType.STRING)
//    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
//    @Column(name = "send_status", nullable = false, columnDefinition = "bill_notification_status_enum")
//    private BillNotificationStatus sendStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "publish_status", nullable = false, columnDefinition = "publish_status_enum")
    private PublishStatus publishStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "send_status", nullable = false, columnDefinition = "send_status_enum")
    private SendStatus sendStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_notification_to_member"))
    private Member member;
}