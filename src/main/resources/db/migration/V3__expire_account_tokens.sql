-- Password reset and email verification links expire, and their tokens are stored as SHA-256 hashes.
-- Tokens issued before this migration were stored as given, so they are cleared.
ALTER TABLE users ADD COLUMN reset_password_token_expires_at TIMESTAMP(6);
ALTER TABLE users ADD COLUMN email_verification_token_expires_at TIMESTAMP(6);
UPDATE users SET reset_password_token = NULL, email_verification_token = NULL;