-- name: create-organization<!
INSERT INTO organization (short_name, long_name)
VALUES (:short_name, :long_name)

-- name: create-user<!
INSERT INTO auth_user (user_name, password, email, first_name, last_name, organization_ref)
VALUES (:user_name, :password, :email, :first_name, :last_name,
        (SELECT organization_id from organization WHERE short_name = :org_short_name))