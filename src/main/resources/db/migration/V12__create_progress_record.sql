CREATE TABLE progress_records 
(
    id                  UUID                     NOT NULL DEFAULT uuidv7(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    user_id             UUID                     NOT NULL,
    recorded_at         DATE                     NOT NULL,
    weight_kg           DOUBLE PRECISION         NOT NULL,
    body_fat_percentage DOUBLE PRECISION,
    
    CONSTRAINT pk_progress_records PRIMARY KEY(id),
    CONSTRAINT fk_progress_records_user FOREIGN KEY(user_id) REFERENCES users(id),
    CONSTRAINT uk_progress_records_user_id_recorded_at UNIQUE (user_id, recorded_at)
)
