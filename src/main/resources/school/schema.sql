-- Tables of the School module (prefix sch_ so they never touch other tables in the same database).
-- Run automatically, once, on the first School request. Safe to run again.
CREATE TABLE IF NOT EXISTS sch_users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(60) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  user_role VARCHAR(20) NOT NULL,
  full_name VARCHAR(120) NULL,
  class_level INT NULL,
  roll_no VARCHAR(20) NULL,
  active BIT(1) NOT NULL,
  must_change_password BIT(1) NOT NULL,
  created_at DATETIME(6) NULL,
  UNIQUE KEY uk_sch_users (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_settings (
  id BIGINT NOT NULL PRIMARY KEY,
  school_name VARCHAR(160) NOT NULL,
  updated_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_materials (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  class_level INT NOT NULL,
  subject VARCHAR(60) NOT NULL,
  title VARCHAR(160) NOT NULL,
  file_name VARCHAR(200) NOT NULL,
  content_type VARCHAR(100) NULL,
  size_bytes BIGINT NULL,
  target_student_id BIGINT NULL,
  uploaded_by VARCHAR(60) NULL,
  uploaded_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_material_data (
  id BIGINT NOT NULL PRIMARY KEY,
  data LONGBLOB NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_tests (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(160) NOT NULL,
  class_level INT NOT NULL,
  subject VARCHAR(60) NOT NULL,
  test_date DATE NOT NULL,
  duration_minutes INT NOT NULL,
  material_id BIGINT NULL,
  active BIT(1) NOT NULL,
  properties_file VARCHAR(80) NOT NULL,
  properties_text LONGTEXT NOT NULL,
  created_by VARCHAR(60) NULL,
  created_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_test_assignments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  test_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  UNIQUE KEY uk_sch_test_assignments (test_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_test_attempts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  test_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  started_at DATETIME(6) NULL,
  submitted_at DATETIME(6) NULL,
  answers_json LONGTEXT NULL,
  score INT NULL,
  total INT NULL,
  UNIQUE KEY uk_sch_test_attempts (test_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_live_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  class_level INT NOT NULL,
  material_id BIGINT NOT NULL,
  title VARCHAR(160) NULL,
  current_page INT NOT NULL,
  active BIT(1) NOT NULL,
  started_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_live_messages (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  author_name VARCHAR(120) NULL,
  author_role VARCHAR(10) NULL,
  message_text VARCHAR(600) NOT NULL,
  created_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sch_game_scores (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  game VARCHAR(40) NOT NULL,
  score INT NOT NULL,
  played_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
