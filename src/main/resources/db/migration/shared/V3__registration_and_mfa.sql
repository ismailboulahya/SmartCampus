ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN mfa_activated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD CONSTRAINT ck_users_mfa_verified CHECK (mfa_activated_at IS NULL OR email_verified_at IS NOT NULL);

CREATE TABLE inscriptions (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_token_hash VARCHAR(64) UNIQUE,
    email_expires_at TIMESTAMP WITH TIME ZONE,
    encrypted_mail_token VARCHAR(255),
    next_mail_attempt_at TIMESTAMP WITH TIME ZONE,
    last_link_issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    setup_token_hash VARCHAR(64) UNIQUE,
    setup_expires_at TIMESTAMP WITH TIME ZONE,
    encrypted_totp_secret VARCHAR(255),
    failed_proofs INTEGER DEFAULT 0 NOT NULL,
    blocked_until TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_inscriptions_mail_due ON inscriptions(next_mail_attempt_at);
