package com.ureca.only4_be.api.dto.notification;

public interface NotificationStatDto {
    String getBillingMonth(); // "2025-02" 형태
    Long getPublishCount();
    Long getSendCount();
}
