-- Existing credentials carry no evidence of approval or creation date.
ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'EN_ATTENTE' NOT NULL;
ALTER TABLE users ADD COLUMN first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN last_name VARCHAR(255);
ALTER TABLE users ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN cne VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN filiere VARCHAR(255);
ALTER TABLE users ADD COLUMN matricule VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN departement VARCHAR(255);

UPDATE users SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));
ALTER TABLE users ADD CONSTRAINT ck_users_email_normalized CHECK (email = LOWER(TRIM(email)));
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'STUDENT', 'TEACHER'));
ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('EN_ATTENTE', 'ACTIF', 'REFUSE', 'BLOQUE'));
ALTER TABLE users ADD CONSTRAINT ck_users_active_profile CHECK (
    status <> 'ACTIF' OR role = 'ADMIN'
    OR (role = 'STUDENT' AND cne IS NOT NULL)
    OR (role = 'TEACHER' AND matricule IS NOT NULL)
);
ALTER TABLE users ADD CONSTRAINT ck_users_student_profile CHECK (
    (cne IS NULL AND filiere IS NULL) OR
    (role = 'STUDENT' AND cne IS NOT NULL AND filiere IS NOT NULL AND TRIM(cne) <> '' AND TRIM(filiere) <> '')
);
ALTER TABLE users ADD CONSTRAINT ck_users_teacher_profile CHECK (
    (matricule IS NULL AND departement IS NULL) OR
    (role = 'TEACHER' AND matricule IS NOT NULL AND departement IS NOT NULL
        AND TRIM(matricule) <> '' AND TRIM(departement) <> '')
);
