--liquibase formatted sql

--changeset radenko:add-oauth2-to-admin
ALTER TABLE admin
    ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS oauth_subject  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email          VARCHAR(255);

-- password is no longer mandatory for OAuth2 users, allow null
ALTER TABLE admin
    ALTER COLUMN password DROP NOT NULL;

-- unique index so a provider+subject pair maps to exactly one admin
CREATE UNIQUE INDEX IF NOT EXISTS UQ_admin_oauth
    ON admin (oauth_provider, oauth_subject)
    WHERE oauth_provider IS NOT NULL;

