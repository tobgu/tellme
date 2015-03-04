CREATE TABLE auth_user (
  name VARCHAR(255),
  token VARCHAR(255) NOT NULL PRIMARY KEY,
  token_valid_until timestamp
);
