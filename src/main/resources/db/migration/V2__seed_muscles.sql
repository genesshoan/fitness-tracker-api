INSERT INTO muscles (id, name, slug, body_region) VALUES

-- CHEST
(uuidv7(), 'Clavicular Pectoralis Major', 'chest-upper', 'CHEST'),
(uuidv7(), 'Sternal Pectoralis Major',    'chest-lower', 'CHEST'),

-- SHOULDERS
(uuidv7(), 'Anterior Deltoid', 'shoulder-front', 'SHOULDERS'),
(uuidv7(), 'Lateral Deltoid',  'shoulder-side',  'SHOULDERS'),

-- ARMS
(uuidv7(), 'Biceps Brachii',  'biceps',  'ARMS'),
(uuidv7(), 'Forearm Flexors', 'forearm', 'ARMS'),

-- CORE
(uuidv7(), 'Upper Rectus Abdominis', 'abs-upper',          'CORE'),
(uuidv7(), 'Lower Rectus Abdominis', 'abs-lower',          'CORE'),
(uuidv7(), 'External Oblique',       'obliques',           'CORE'),
(uuidv7(), 'Serratus Anterior',      'serratus-anterior',  'CORE'),

-- LEGS
(uuidv7(), 'Quadriceps',        'quads',       'LEGS'),
(uuidv7(), 'Adductors',         'adductors',   'LEGS'),
(uuidv7(), 'Hip Flexors',       'hip-flexor',  'LEGS'),
(uuidv7(), 'Tibialis Anterior', 'tibialis-anterior', 'LEGS'),

-- HEAD & NECK
(uuidv7(), 'Head', 'head', 'OTHER'),
(uuidv7(), 'Face', 'face', 'OTHER'),
(uuidv7(), 'Neck', 'neck', 'OTHER');
