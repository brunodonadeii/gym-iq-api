UPDATE instructor
SET specialty = 'Nao informada'
WHERE specialty IS NULL
   OR TRIM(specialty) = '';

ALTER TABLE instructor
    ALTER COLUMN specialty SET NOT NULL;
