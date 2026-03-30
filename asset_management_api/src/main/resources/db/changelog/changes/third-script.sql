CREATE TABLE refresh_token (
    refresh_token_id  BIGINT PRIMARY KEY ,
    token VARCHAR(255) NOT NULL UNIQUE,
    admin_id BIGINT NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT FK_refresh_token_admin
                           FOREIGN KEY (admin_id)
                           REFERENCES admin(admin_id)
                           ON DELETE CASCADE
);