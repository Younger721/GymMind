-- GymMind course description migration (MySQL 8+).
-- Course descriptions are optional so existing schedules remain valid.
ALTER TABLE course_schedule
    ADD COLUMN IF NOT EXISTS description VARCHAR(2000) NULL;
