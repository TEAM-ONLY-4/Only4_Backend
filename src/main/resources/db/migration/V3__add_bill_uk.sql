ALTER TABLE bill
    ADD CONSTRAINT uk_bill_member_date UNIQUE (member_id, billing_year_month);