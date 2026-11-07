-- Columns that the app always fills, made NOT NULL. V1 left them nullable because the entities marked them with
-- @NotNull, which Hibernate doesn't turn into DDL. The forms are validated in their handlers, so a missing value goes
-- back to the form instead of reaching these constraints. The audit tables stay nullable: Envers writes deleted rows
-- with empty columns.
-- A database that already holds a NULL in one of these columns fails here, naming the column.
ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ALTER COLUMN password SET NOT NULL;
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE project ALTER COLUMN name SET NOT NULL;
ALTER TABLE project ALTER COLUMN identifier SET NOT NULL;
ALTER TABLE project ALTER COLUMN color SET NOT NULL;
ALTER TABLE issue ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE issue ALTER COLUMN project_id SET NOT NULL;
ALTER TABLE comment ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE comment ALTER COLUMN issue_id SET NOT NULL;