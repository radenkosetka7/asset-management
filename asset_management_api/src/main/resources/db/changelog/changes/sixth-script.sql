CREATE SEQUENCE refresh_token_refresh_token_id_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE refresh_token (
 refresh_token_id BIGINT PRIMARY KEY
 DEFAULT nextval('refresh_token_refresh_token_id_seq'),
 hash_token VARCHAR(44) NOT NULL UNIQUE,
 admin_id BIGINT NOT NULL,
 expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,

 CONSTRAINT FK_refresh_token_admin
 FOREIGN KEY (admin_id) REFERENCES admin(admin_id) ON DELETE CASCADE
);