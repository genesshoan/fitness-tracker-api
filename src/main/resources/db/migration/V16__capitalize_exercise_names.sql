WITH parsed AS (
    SELECT id,
           regexp_match(name, '^([^a-zA-Z]*)([a-zA-Z])(.*)$') AS parts
    FROM exercises
)
UPDATE exercises e
SET name = p.parts[1] || upper(p.parts[2]) || p.parts[3],
    updated_at = NOW()
FROM parsed p
WHERE e.id = p.id
  AND p.parts IS NOT NULL
  AND p.parts[2] = lower(p.parts[2]);
