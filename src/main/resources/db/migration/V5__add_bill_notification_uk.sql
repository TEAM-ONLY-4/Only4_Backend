ALTER TABLE bill_notification
    ADD CONSTRAINT uk_bill_notification_bill UNIQUE (bill_id);