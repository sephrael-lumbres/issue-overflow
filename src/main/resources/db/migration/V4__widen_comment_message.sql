-- Comments were VARCHAR(255), so a longer comment failed to save. The comment forms now allow up to 20,000 characters.
-- File names stay VARCHAR(255); longer names are shortened when a file is uploaded.
ALTER TABLE comment ALTER COLUMN message TYPE TEXT;