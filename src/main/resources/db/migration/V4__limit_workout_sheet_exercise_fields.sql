ALTER TABLE workout_sheet_exercise
    ADD CONSTRAINT chk_workout_sheet_exercise_sets_range
        CHECK (sets BETWEEN 1 AND 100);

ALTER TABLE workout_sheet_exercise
    ADD CONSTRAINT chk_workout_sheet_exercise_rest_seconds_range
        CHECK (rest_seconds IS NULL OR rest_seconds BETWEEN 0 AND 1200);

ALTER TABLE workout_sheet_exercise
    ADD CONSTRAINT chk_workout_sheet_exercise_execution_order_range
        CHECK (execution_order BETWEEN 1 AND 100);
