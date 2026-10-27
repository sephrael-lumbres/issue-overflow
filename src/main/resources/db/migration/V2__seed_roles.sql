-- The four application roles. WebSecurityConfig and the controllers refer to them by name.
INSERT INTO role (id, name) VALUES (nextval('role_seq'), 'Project Manager');
INSERT INTO role (id, name) VALUES (nextval('role_seq'), 'Admin');
INSERT INTO role (id, name) VALUES (nextval('role_seq'), 'Developer');
INSERT INTO role (id, name) VALUES (nextval('role_seq'), 'Guest');