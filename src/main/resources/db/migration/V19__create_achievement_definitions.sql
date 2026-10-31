CREATE TABLE achievement_definitions
(
    id          UUID                     NOT NULL DEFAULT uuidv7(),
    code        VARCHAR(64)              NOT NULL,
    name        VARCHAR(128)             NOT NULL,
    description TEXT,
    category    VARCHAR(32)              NOT NULL,
    tier        VARCHAR(16)              NOT NULL DEFAULT 'BRONZE',
    rule_type   VARCHAR(24)              NOT NULL,
    metric      VARCHAR(48)              NOT NULL,
    operator    VARCHAR(8)               NOT NULL DEFAULT 'GTE',
    threshold   DOUBLE PRECISION,
    scope       VARCHAR(16)              NOT NULL DEFAULT 'GLOBAL',
    icon_key    VARCHAR(128),
    sort_order  INT                      NOT NULL DEFAULT 0,
    active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_achievement_definitions PRIMARY KEY (id),
    CONSTRAINT uk_achievement_definitions_code UNIQUE (code),

    CONSTRAINT ck_achievement_definitions_category
        CHECK (category IN ('MILESTONE', 'CONSISTENCY', 'VOLUME', 'EXPLORATION', 'PERSONAL_RECORD')),
    CONSTRAINT ck_achievement_definitions_tier
        CHECK (tier IN ('BRONZE', 'SILVER', 'GOLD')),
    CONSTRAINT ck_achievement_definitions_rule_type
        CHECK (rule_type IN ('THRESHOLD', 'PERSONAL_RECORD')),
    CONSTRAINT ck_achievement_definitions_scope
        CHECK (scope IN ('GLOBAL', 'PER_EXERCISE')),
    CONSTRAINT ck_achievement_definitions_operator
        CHECK (operator IN ('GTE', 'GT')),

    -- A threshold rule needs a threshold; a personal record rule does not use one
    CONSTRAINT ck_achievement_definitions_threshold
        CHECK ((rule_type = 'THRESHOLD' AND threshold IS NOT NULL)
            OR (rule_type = 'PERSONAL_RECORD' AND threshold IS NULL)),

    -- The metric has to belong to the kind of rule it is used with
    CONSTRAINT ck_achievement_definitions_metric
        CHECK ((rule_type = 'THRESHOLD' AND metric IN (
                    'TOTAL_WORKOUTS', 'LONGEST_STREAK', 'TOTAL_SETS', 'TOTAL_REPS',
                    'TOTAL_VOLUME_KG', 'TOTAL_TRAINING_SECONDS', 'TOTAL_DISTANCE_KM',
                    'DISTINCT_EXERCISES'))
            OR (rule_type = 'PERSONAL_RECORD' AND metric IN (
                    'MAX_WEIGHT', 'MAX_ESTIMATED_1RM', 'MAX_REPS_AT_WEIGHT',
                    'MAX_DISTANCE', 'MAX_DURATION'))),

    -- Only a personal record achievement can be scoped to an exercise
    CONSTRAINT ck_achievement_definitions_scope_rule
        CHECK ((rule_type = 'THRESHOLD' AND scope = 'GLOBAL')
            OR (rule_type = 'PERSONAL_RECORD'))
);

CREATE INDEX idx_achievement_definitions_active
    ON achievement_definitions (active, category, sort_order);
