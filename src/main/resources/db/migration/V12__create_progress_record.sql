CREATE TABLE progress_records
(
    id                  UUID                     NOT NULL DEFAULT uuidv7(),
    user_id             UUID                     NOT NULL,
    recorded_at         DATE                     NOT NULL,
    weight_kg           DOUBLE PRECISION         NOT NULL,
    body_fat_percentage DOUBLE PRECISION,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_progress_records PRIMARY KEY (id),
    CONSTRAINT fk_progress_records_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_progress_records_user_recorded_at UNIQUE (user_id, recorded_at),
    CONSTRAINT ck_progress_records_weight_kg CHECK (weight_kg > 0),
    CONSTRAINT ck_progress_records_body_fat_percentage CHECK (body_fat_percentage IS NULL
        OR (body_fat_percentage >= 0 AND body_fat_percentage <= 100))
);
