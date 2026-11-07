-- Issue keys are numbered from one counter row per project, keyed by the project's id. The counter only goes up, so a
-- deleted issue's number is never given out again, and renaming a project's identifier doesn't touch it.
-- Keys are unique within a project (the same key can exist in two projects, for example after a rename).

-- rows for projects that no longer exist, then all but the highest counter of each project
DELETE FROM issue_key_sequence s
WHERE s.project_id IS NULL
   OR NOT EXISTS (SELECT 1 FROM project p WHERE p.id = s.project_id);
DELETE FROM issue_key_sequence s
    USING issue_key_sequence t
WHERE s.project_id = t.project_id
  AND (s.issue_key_counter < t.issue_key_counter
   OR (s.issue_key_counter = t.issue_key_counter AND s.id < t.id));

ALTER TABLE issue_key_sequence DROP COLUMN id;
ALTER TABLE issue_key_sequence DROP COLUMN project_identifier;
ALTER TABLE issue_key_sequence ADD PRIMARY KEY (project_id);
ALTER TABLE issue_key_sequence ADD CONSTRAINT issue_key_sequence_project_id_fkey
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE;
DROP SEQUENCE issue_key_sequence_seq;

-- fails if a project already holds two issues with the same key (none in a new database)
ALTER TABLE issue ADD CONSTRAINT issue_project_id_issue_key_key UNIQUE (project_id, issue_key);