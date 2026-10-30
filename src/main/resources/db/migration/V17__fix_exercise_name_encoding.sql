UPDATE exercises
SET name = replace(replace(name, E'\u00d0\u00b2\u00c2\u00b0', E'\u00b0'), E'\u00c2\u00b0', E'\u00b0'),
    description = replace(replace(description, E'\u00d0\u00b2\u00c2\u00b0', E'\u00b0'), E'\u00c2\u00b0', E'\u00b0'),
    updated_at = NOW()
WHERE name LIKE E'%\u00c2\u00b0%'
   OR description LIKE E'%\u00c2\u00b0%'
   OR name LIKE E'%\u00d0\u00b2%'
   OR description LIKE E'%\u00d0\u00b2%';
