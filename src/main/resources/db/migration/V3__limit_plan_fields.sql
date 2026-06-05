UPDATE plan
SET description = LEFT(description, 100)
WHERE description IS NOT NULL
  AND LENGTH(description) > 100;

ALTER TABLE plan
    ALTER COLUMN description TYPE VARCHAR(100);

ALTER TABLE plan
    ADD CONSTRAINT chk_plan_duration_months_range
        CHECK (duration_months BETWEEN 1 AND 24);

ALTER TABLE plan
    ADD CONSTRAINT chk_plan_monthly_price_range
        CHECK (monthly_price BETWEEN 0.01 AND 500.00);
