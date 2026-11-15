-- Email addresses are compared without case. The app now stores them in lower case; this lowers any stored in
-- another case and refuses two addresses that differ only in case, whichever code path writes them.
UPDATE users SET email = lower(email) WHERE email <> lower(email);

CREATE UNIQUE INDEX users_email_lower_key ON users (lower(email));
