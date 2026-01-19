package com.ureca.only4_be.kafka.event;

public record EmailSendRequestEvent (
        Long memberId,
        Long billId
)
{}
