CREATE TABLE workout_block (
    id_workout_block UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_sheet_id UUID NOT NULL,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(255),
    execution_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_workout_block_sheet FOREIGN KEY (workout_sheet_id) REFERENCES workout_sheet (id_workout_sheet),
    CONSTRAINT uk_workout_block_name UNIQUE (workout_sheet_id, name),
    CONSTRAINT uk_workout_block_order UNIQUE (workout_sheet_id, execution_order)
);

INSERT INTO workout_block (workout_sheet_id, name, execution_order, active, created_at, updated_at)
SELECT
    sections.workout_sheet_id,
    sections.training_section,
    ROW_NUMBER() OVER (
        PARTITION BY sections.workout_sheet_id
        ORDER BY sections.training_section
    ),
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT workout_sheet_id, training_section
    FROM workout_sheet_exercise
) sections;

ALTER TABLE workout_sheet_exercise
    ADD COLUMN workout_block_id UUID;

UPDATE workout_sheet_exercise item
SET workout_block_id = block.id_workout_block
FROM workout_block block
WHERE block.workout_sheet_id = item.workout_sheet_id
  AND LOWER(block.name) = LOWER(item.training_section);

ALTER TABLE workout_sheet_exercise
    ALTER COLUMN workout_block_id SET NOT NULL;

ALTER TABLE workout_sheet_exercise
    DROP CONSTRAINT IF EXISTS uk_workout_sheet_exercise_order;

ALTER TABLE workout_sheet_exercise
    DROP CONSTRAINT IF EXISTS fk_workout_sheet_exercise_sheet;

ALTER TABLE workout_sheet_exercise
    ADD CONSTRAINT fk_workout_sheet_exercise_block
        FOREIGN KEY (workout_block_id) REFERENCES workout_block (id_workout_block);

ALTER TABLE workout_sheet_exercise
    ADD CONSTRAINT uk_workout_sheet_exercise_order
        UNIQUE (workout_block_id, execution_order);

ALTER TABLE workout_sheet_exercise
    DROP COLUMN workout_sheet_id,
    DROP COLUMN training_section;

CREATE INDEX idx_workout_block_sheet_id ON workout_block (workout_sheet_id);
CREATE INDEX idx_workout_sheet_exercise_block_id ON workout_sheet_exercise (workout_block_id);
