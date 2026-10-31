-- ACHIEVEMENT CATALOG
-- Threshold rules are pure data: a new one is added by inserting a row.
-- Personal record rules are evaluated in code, because comparing against the
-- previous best needs semantics that a threshold cannot express.

INSERT INTO achievement_definitions (
    code, name, description, category, tier, rule_type, metric, operator, threshold, scope, sort_order)
VALUES
-- Milestones
('FIRST_WORKOUT', 'First workout', 'Complete your first workout',
 'MILESTONE', 'BRONZE', 'THRESHOLD', 'TOTAL_WORKOUTS', 'GTE', 1, 'GLOBAL', 0),
('WORKOUTS_10', 'Ten workouts', 'Complete 10 workouts',
 'MILESTONE', 'BRONZE', 'THRESHOLD', 'TOTAL_WORKOUTS', 'GTE', 10, 'GLOBAL', 1),
('WORKOUTS_50', 'Fifty workouts', 'Complete 50 workouts',
 'MILESTONE', 'SILVER', 'THRESHOLD', 'TOTAL_WORKOUTS', 'GTE', 50, 'GLOBAL', 2),
('WORKOUTS_100', 'One hundred workouts', 'Complete 100 workouts',
 'MILESTONE', 'GOLD', 'THRESHOLD', 'TOTAL_WORKOUTS', 'GTE', 100, 'GLOBAL', 3),
('WORKOUTS_365', 'A year of workouts', 'Complete 365 workouts',
 'MILESTONE', 'GOLD', 'THRESHOLD', 'TOTAL_WORKOUTS', 'GTE', 365, 'GLOBAL', 4),

-- Consistency
('STREAK_3', 'Three day streak', 'Train 3 days in a row',
 'CONSISTENCY', 'BRONZE', 'THRESHOLD', 'LONGEST_STREAK', 'GTE', 3, 'GLOBAL', 5),
('STREAK_7', 'One week streak', 'Train 7 days in a row',
 'CONSISTENCY', 'SILVER', 'THRESHOLD', 'LONGEST_STREAK', 'GTE', 7, 'GLOBAL', 6),
('STREAK_30', 'One month streak', 'Train 30 days in a row',
 'CONSISTENCY', 'GOLD', 'THRESHOLD', 'LONGEST_STREAK', 'GTE', 30, 'GLOBAL', 7),
('STREAK_100', 'Hundred day streak', 'Train 100 days in a row',
 'CONSISTENCY', 'GOLD', 'THRESHOLD', 'LONGEST_STREAK', 'GTE', 100, 'GLOBAL', 8),

-- Volume
('SETS_100', 'One hundred sets', 'Complete 100 sets',
 'VOLUME', 'BRONZE', 'THRESHOLD', 'TOTAL_SETS', 'GTE', 100, 'GLOBAL', 9),
('SETS_1000', 'One thousand sets', 'Complete 1,000 sets',
 'VOLUME', 'SILVER', 'THRESHOLD', 'TOTAL_SETS', 'GTE', 1000, 'GLOBAL', 10),
('REPS_1K', 'One thousand reps', 'Perform 1,000 repetitions',
 'VOLUME', 'BRONZE', 'THRESHOLD', 'TOTAL_REPS', 'GTE', 1000, 'GLOBAL', 11),
('REPS_10K', 'Ten thousand reps', 'Perform 10,000 repetitions',
 'VOLUME', 'SILVER', 'THRESHOLD', 'TOTAL_REPS', 'GTE', 10000, 'GLOBAL', 12),
('VOLUME_10K_KG', 'Ten thousand kilos', 'Lift 10,000 kg in total',
 'VOLUME', 'BRONZE', 'THRESHOLD', 'TOTAL_VOLUME_KG', 'GTE', 10000, 'GLOBAL', 13),
('VOLUME_100K_KG', 'One hundred thousand kilos', 'Lift 100,000 kg in total',
 'VOLUME', 'GOLD', 'THRESHOLD', 'TOTAL_VOLUME_KG', 'GTE', 100000, 'GLOBAL', 14),
('TRAINING_10H', 'Ten hours of training', 'Accumulate 10 hours of training',
 'VOLUME', 'SILVER', 'THRESHOLD', 'TOTAL_TRAINING_SECONDS', 'GTE', 36000, 'GLOBAL', 15),
('DISTANCE_100_KM', 'One hundred kilometers', 'Cover 100 km in total',
 'VOLUME', 'SILVER', 'THRESHOLD', 'TOTAL_DISTANCE_KM', 'GTE', 100, 'GLOBAL', 16),

-- Exploration
('EXERCISES_10', 'Ten exercises', 'Train 10 different exercises',
 'EXPLORATION', 'BRONZE', 'THRESHOLD', 'DISTINCT_EXERCISES', 'GTE', 10, 'GLOBAL', 17),
('EXERCISES_50', 'Fifty exercises', 'Train 50 different exercises',
 'EXPLORATION', 'SILVER', 'THRESHOLD', 'DISTINCT_EXERCISES', 'GTE', 50, 'GLOBAL', 18),

-- First personal record, whatever the exercise
('PR_FIRST_MAX_WEIGHT', 'First weight record', 'Set your first weight record',
 'PERSONAL_RECORD', 'BRONZE', 'PERSONAL_RECORD', 'MAX_WEIGHT', 'GTE', NULL, 'GLOBAL', 19),
('PR_FIRST_MAX_1RM', 'First estimated 1RM record',
 'Set your first estimated one repetition maximum record',
 'PERSONAL_RECORD', 'BRONZE', 'PERSONAL_RECORD', 'MAX_ESTIMATED_1RM', 'GTE', NULL, 'GLOBAL', 20),
('PR_FIRST_MAX_REPS', 'First reps record',
 'Perform more repetitions than ever before at the same weight',
 'PERSONAL_RECORD', 'BRONZE', 'PERSONAL_RECORD', 'MAX_REPS_AT_WEIGHT', 'GTE', NULL, 'GLOBAL', 21),
('PR_FIRST_MAX_DISTANCE', 'First distance record', 'Set your first distance record',
 'PERSONAL_RECORD', 'BRONZE', 'PERSONAL_RECORD', 'MAX_DISTANCE', 'GTE', NULL, 'GLOBAL', 22),
('PR_FIRST_MAX_DURATION', 'First duration record', 'Set your first duration record',
 'PERSONAL_RECORD', 'BRONZE', 'PERSONAL_RECORD', 'MAX_DURATION', 'GTE', NULL, 'GLOBAL', 23),

-- Personal record per exercise
('PR_MAX_WEIGHT_PER_EXERCISE', 'Weight record', 'Set a weight record on an exercise',
 'PERSONAL_RECORD', 'SILVER', 'PERSONAL_RECORD', 'MAX_WEIGHT', 'GTE', NULL, 'PER_EXERCISE', 24),
('PR_MAX_1RM_PER_EXERCISE', 'Estimated 1RM record',
 'Set an estimated one repetition maximum record on an exercise',
 'PERSONAL_RECORD', 'SILVER', 'PERSONAL_RECORD', 'MAX_ESTIMATED_1RM', 'GTE', NULL, 'PER_EXERCISE', 25),
('PR_MAX_REPS_PER_EXERCISE', 'Reps record', 'Set a repetitions record on an exercise',
 'PERSONAL_RECORD', 'SILVER', 'PERSONAL_RECORD', 'MAX_REPS_AT_WEIGHT', 'GTE', NULL, 'PER_EXERCISE', 26),
('PR_MAX_DISTANCE_PER_EXERCISE', 'Distance record', 'Set a distance record on an exercise',
 'PERSONAL_RECORD', 'SILVER', 'PERSONAL_RECORD', 'MAX_DISTANCE', 'GTE', NULL, 'PER_EXERCISE', 27),
('PR_MAX_DURATION_PER_EXERCISE', 'Duration record', 'Set a duration record on an exercise',
 'PERSONAL_RECORD', 'SILVER', 'PERSONAL_RECORD', 'MAX_DURATION', 'GTE', NULL, 'PER_EXERCISE', 28);
