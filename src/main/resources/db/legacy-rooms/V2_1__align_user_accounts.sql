-- Keep IDs, hashes, academic records and the columns specific to the earlier model.
ALTER TABLE users RENAME COLUMN type_utilisateur TO role;
ALTER TABLE users RENAME COLUMN mdp_hash TO password;
ALTER TABLE users RENAME COLUMN nom TO last_name;
ALTER TABLE users RENAME COLUMN prenom TO first_name;
ALTER TABLE users RENAME COLUMN date_creation TO created_at;
ALTER TABLE users RENAME COLUMN nbr_connexion_echouee TO failed_logins;
ALTER TABLE users RENAME COLUMN fin_verrouillage TO login_locked_until;
ALTER TABLE users ALTER COLUMN created_at SET DATA TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ALTER COLUMN created_at DROP NOT NULL;
ALTER TABLE users ALTER COLUMN first_name DROP NOT NULL;
ALTER TABLE users ALTER COLUMN last_name DROP NOT NULL;
ALTER TABLE users ALTER COLUMN login_locked_until SET DATA TYPE TIMESTAMP WITH TIME ZONE;
UPDATE users SET login_locked_until = NULL WHERE est_verrouille = FALSE;
ALTER TABLE users ALTER COLUMN email SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN password SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN first_name SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN last_name SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN cne SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN filiere SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN matricule SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN departement SET DATA TYPE VARCHAR(255);

UPDATE users SET email = LOWER(TRIM(email)), role = CASE role
    WHEN 'ETUDIANT' THEN 'STUDENT' WHEN 'PROFESSEUR' THEN 'TEACHER'
    WHEN 'SUPERADMIN' THEN 'ADMIN' ELSE role END;
ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'EN_ATTENTE' NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_matricule UNIQUE (matricule);
ALTER TABLE users ADD CONSTRAINT ck_users_email_normalized CHECK (email = LOWER(TRIM(email)));
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'STUDENT', 'TEACHER'));
ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('EN_ATTENTE', 'ACTIF', 'REFUSE', 'BLOQUE'));
ALTER TABLE users ADD CONSTRAINT ck_users_active_profile CHECK (
    status <> 'ACTIF' OR role = 'ADMIN' OR (role = 'STUDENT' AND cne IS NOT NULL)
    OR (role = 'TEACHER' AND matricule IS NOT NULL)
);
ALTER TABLE users ADD CONSTRAINT ck_users_student_profile CHECK (
    status = 'EN_ATTENTE' OR
    (cne IS NULL AND filiere IS NULL) OR
    (role = 'STUDENT' AND cne IS NOT NULL AND filiere IS NOT NULL AND TRIM(cne) <> '' AND TRIM(filiere) <> '')
);
ALTER TABLE users ADD CONSTRAINT ck_users_teacher_profile CHECK (
    status = 'EN_ATTENTE' OR
    (matricule IS NULL AND departement IS NULL) OR
    (role = 'TEACHER' AND matricule IS NOT NULL AND departement IS NOT NULL
        AND TRIM(matricule) <> '' AND TRIM(departement) <> '')
);
