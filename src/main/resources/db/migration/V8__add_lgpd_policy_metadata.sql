ALTER TABLE users
    ADD COLUMN IF NOT EXISTS lgpd_policy_version VARCHAR(20),
    ADD COLUMN IF NOT EXISTS lgpd_consent_source VARCHAR(40);

UPDATE users
SET lgpd_policy_version = '1.0'
WHERE lgpd_policy_version IS NULL
  AND lgpd_accepted = true;

UPDATE users
SET lgpd_accepted_at = COALESCE(lgpd_accepted_at, created_at, NOW())
WHERE lgpd_accepted = true
  AND lgpd_accepted_at IS NULL;

UPDATE users
SET lgpd_consent_source = CASE
    WHEN role = 'STUDENT' THEN 'RECEPTION_REGISTRATION'
    ELSE 'ADMIN_REGISTRATION'
END
WHERE lgpd_accepted = true
  AND lgpd_consent_source IS NULL;
