--liquibase formatted sql

--changeset radenko:create-initial-schema
-- Create sequences
CREATE SEQUENCE IF NOT EXISTS admin_admin_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS asset_category_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS asset_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS attribute_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS image_image_id_seq START WITH 1 INCREMENT BY 1;

-- Create admin table
CREATE TABLE IF NOT EXISTS admin (
    admin_id BIGINT PRIMARY KEY DEFAULT nextval('admin_admin_id_seq'),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Create category table
CREATE TABLE IF NOT EXISTS category (
    category_id BIGINT PRIMARY KEY DEFAULT nextval('asset_category_id_seq'),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_category_id BIGINT,
    CONSTRAINT FK_category_parent FOREIGN KEY (parent_category_id)
        REFERENCES category(category_id) ON DELETE CASCADE
);

-- Create asset table
CREATE TABLE IF NOT EXISTS asset (
    asset_id BIGINT PRIMARY KEY DEFAULT nextval('asset_id_seq'),
    asset_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    category_id BIGINT NOT NULL,
    CONSTRAINT FK_asset_category FOREIGN KEY (category_id)
        REFERENCES category(category_id) ON DELETE CASCADE
);

-- Create attribute table
CREATE TABLE IF NOT EXISTS attribute (
    attribute_id BIGINT PRIMARY KEY DEFAULT nextval('attribute_id_seq'),
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Create category_attribute table
CREATE TABLE IF NOT EXISTS category_attribute (
    category_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    PRIMARY KEY (category_id, attribute_id),
    CONSTRAINT FK_category_attribute_category FOREIGN KEY (category_id)
        REFERENCES category(category_id) ON DELETE CASCADE,
    CONSTRAINT FK_category_attribute_attribute FOREIGN KEY (attribute_id)
        REFERENCES attribute(attribute_id) ON DELETE CASCADE
);

-- Create image table
CREATE TABLE IF NOT EXISTS image (
    image_id BIGINT PRIMARY KEY DEFAULT nextval('image_image_id_seq'),
    file_name VARCHAR(255) NOT NULL
);

-- Create asset_image table
CREATE TABLE IF NOT EXISTS asset_image (
    asset_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    PRIMARY KEY (asset_id, image_id),
    CONSTRAINT FK_asset_image_asset FOREIGN KEY (asset_id)
        REFERENCES asset(asset_id) ON DELETE CASCADE,
    CONSTRAINT FK_asset_image_image FOREIGN KEY (image_id)
        REFERENCES image(image_id) ON DELETE CASCADE
);

-- Create asset_attribute_value table
CREATE TABLE IF NOT EXISTS asset_attribute_value (
    asset_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    value_string TEXT,
    value_number DECIMAL(18, 2),
    value_date DATE,
    value_bool BOOLEAN,
    PRIMARY KEY (asset_id, attribute_id),
    CONSTRAINT FK_asset_attribute_value_asset FOREIGN KEY (asset_id)
        REFERENCES asset(asset_id) ON DELETE CASCADE,
    CONSTRAINT FK_asset_attribute_value_attribute FOREIGN KEY (attribute_id)
        REFERENCES attribute(attribute_id) ON DELETE CASCADE
);

