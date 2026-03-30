ALTER TABLE asset_image ADD COLUMN sort_order integer not null;

ALTER TABLE refresh_token DROP COLUMN token;
ALTER TABLE refresh_token ADD COLUMN hash_token VARCHAR(44) NOT NULL;
