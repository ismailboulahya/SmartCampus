-- =============================================================
-- Comptes de développement (profil "dev" uniquement).
-- Chargé seulement si spring.flyway.locations contient db/seed.
-- Mots de passe (BCrypt) :
--   etudiant@usms.ac.ma   / Passw0rd!2026
--   professeur@usms.ac.ma / Prof!2026
--   admin@usms.ac.ma      / Admin!2026
-- =============================================================
INSERT INTO users (email, password_hash, first_name, last_name, role)
VALUES
    ('etudiant@usms.ac.ma',
     '$2a$10$SdVay4xyN.ei/OqgTb2c0.WuOv32r5cLyUZsHqqF5QFMiNr6YTkwC',
     'Etudiant', 'Test', 'STUDENT'),
    ('professeur@usms.ac.ma',
     '$2a$10$2Dle9h9s1e2U4wKN/QLQ8.eRhOhQ74/6uLFczJLiBfd4sCli0qJ8W',
     'Professeur', 'Test', 'PROFESSOR'),
    ('admin@usms.ac.ma',
     '$2a$10$MdfODaubUg7.lIM4fU699u9EdlNwhXhJjhXe/DBM9h9ln5FpT9p96',
     'Admin', 'Test', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
