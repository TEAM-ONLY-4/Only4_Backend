ALTER TABLE bill DROP COLUMN billing_yyyymm;
ALTER TABLE bill ADD COLUMN billing_year_month DATE NOT NULL;

ALTER TABLE Subscription_usage DROP COLUMN usage_yyyymm;
ALTER TABLE Subscription_usage ADD COLUMN usage_year_month DATE NOT NULL;

ALTER TABLE Reservation_notification DROP COLUMN target_billing_yyyymm;
ALTER TABLE Reservation_notification ADD COLUMN target_billing_year_month DATE NOT NULL;
