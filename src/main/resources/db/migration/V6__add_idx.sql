CREATE INDEX idx_bill_notification_bill_id ON bill_notification(bill_id);
CREATE INDEX idx_bill_item_bill_id ON bill_item(bill_id);
CREATE INDEX idx_bill_member_billing ON bill(member_id, billing_year_month);