CREATE TABLE muscles
(
    id          UUID                        NOT NULL DEFAULT uuidv7(),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    name        VARCHAR(255)                NOT NULL,
    slug        VARCHAR(255)                NOT NULL,
    body_region VARCHAR(255)                NOT NULL,

    CONSTRAINT pk_muscles PRIMARY KEY (id),
    CONSTRAINT uc_muscles_name UNIQUE (name),
    CONSTRAINT uc_muscles_slug UNIQUE (slug)
);

CREATE TABLE muscle_assets
(
    id           UUID                        NOT NULL DEFAULT uuidv7(),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    object_key   VARCHAR(255)                NOT NULL,
    variant      VARCHAR(255)                NOT NULL,
    view         VARCHAR(255)                NOT NULL,
    content_type VARCHAR(255),

    CONSTRAINT pk_muscle_assets PRIMARY KEY (id),
    CONSTRAINT uk_muscle_assets_object_key UNIQUE (object_key)
);

CREATE TABLE muscle_asset_mappings
(
    muscle_id       UUID NOT NULL,
    muscle_asset_id UUID NOT NULL,

    CONSTRAINT pk_muscle_asset_mappings PRIMARY KEY (muscle_id, muscle_asset_id),
    CONSTRAINT fk_muscle_asset_mappings_muscle
        FOREIGN KEY (muscle_id) REFERENCES muscles (id) ON DELETE CASCADE,
    CONSTRAINT fk_muscle_asset_mappings_asset
        FOREIGN KEY (muscle_asset_id) REFERENCES muscle_assets (id) ON DELETE CASCADE
);
