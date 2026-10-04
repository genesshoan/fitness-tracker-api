CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS ix_exercises_name_trgm
    ON exercises USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_exercises_slug_trgm
    ON exercises USING gin (slug gin_trgm_ops);
