CREATE TABLE organization (
  organization_id SERIAL PRIMARY KEY,
  short_name VARCHAR(255) NOT NULL UNIQUE,
  long_name VARCHAR(255) NOT NULL
  );

CREATE TABLE auth_user (
  user_id SERIAL PRIMARY KEY,
  user_name VARCHAR(255) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255)  NOT NULL,
  organization_ref integer REFERENCES organization (organization_id)
  );

CREATE TABLE session (
  token VARCHAR(255) NOT NULL PRIMARY KEY,
  token_valid_until timestamp,
  auth_user_ref integer REFERENCES auth_user (user_id)
  );
