-- GymMind 数据库建表脚本
-- 执行前请确保已连接到 gymmind 数据库

-- 1. 训练计划表
DROP TABLE IF EXISTS workout_plans;
CREATE TABLE workout_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    goal VARCHAR(50) NOT NULL COMMENT 'MUSCLE_GAIN, WEIGHT_LOSS, STRENGTH, ENDURANCE',
    difficulty VARCHAR(50) NOT NULL COMMENT 'BEGINNER, INTERMEDIATE, ADVANCED',
    duration_weeks INT NOT NULL,
    workouts_per_week INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    plan_content TEXT NOT NULL COMMENT 'JSON格式的完整计划内容',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, COMPLETED, PAUSED',
    is_ai_generated BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练计划主表';

-- 2. 训练计划天详情表
DROP TABLE IF EXISTS workout_plan_days;
CREATE TABLE workout_plan_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    week_number INT NOT NULL,
    day_number INT NOT NULL,
    day_name VARCHAR(100) NOT NULL,
    exercises TEXT COMMENT 'JSON格式的动作列表',
    notes VARCHAR(500),
    is_rest_day BOOLEAN NOT NULL DEFAULT FALSE,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_plan_id (plan_id),
    FOREIGN KEY (plan_id) REFERENCES workout_plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练计划天详情表';

-- 3. 知识库图片表
DROP TABLE IF EXISTS knowledge_images;
CREATE TABLE knowledge_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_id BIGINT DEFAULT NULL,
    image_name VARCHAR(200) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    description TEXT COMMENT 'AI生成的图片描述',
    category VARCHAR(100) COMMENT 'EXERCISE_DEMO, BODY_MEASUREMENT, MEAL, PROGRESS',
    detected_action VARCHAR(100) COMMENT 'AI识别的动作名称',
    muscle_group VARCHAR(100) COMMENT 'CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE',
    difficulty VARCHAR(50) COMMENT 'BEGINNER, INTERMEDIATE, ADVANCED',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PROCESSING, COMPLETED, FAILED',
    error_message VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vector_id VARCHAR(100) COMMENT 'Milvus向量ID',
    es_id VARCHAR(100) COMMENT 'Elasticsearch文档ID',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库图片表';

-- 4. 社交动态表
DROP TABLE IF EXISTS posts;
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT,
    image_urls TEXT COMMENT 'JSON数组格式的图片URL列表',
    post_type VARCHAR(50) DEFAULT 'GENERAL' COMMENT 'WORKOUT, PROGRESS, MEAL, GENERAL',
    metadata TEXT COMMENT 'JSON格式的额外元数据',
    likes INT DEFAULT 0,
    comments INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_post_type (post_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交动态表';

-- 5. 评论表
DROP TABLE IF EXISTS comments;
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 6. 点赞表
DROP TABLE IF EXISTS post_likes;
CREATE TABLE post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 7. 挑战表
DROP TABLE IF EXISTS challenges;
CREATE TABLE challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    goal_type VARCHAR(50) NOT NULL COMMENT 'WORKOUT_COUNT, TOTAL_TIME, TOTAL_CALORIES, DISTANCE',
    goal_value INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPCOMING' COMMENT 'UPCOMING, ACTIVE, COMPLETED, CANCELLED',
    participant_count INT DEFAULT 0,
    image_url VARCHAR(500),
    difficulty VARCHAR(50) COMMENT 'EASY, MEDIUM, HARD',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挑战表';

-- 8. 挑战参与者表
DROP TABLE IF EXISTS challenge_participants;
CREATE TABLE challenge_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    progress INT DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, COMPLETED, ABANDONED',
    rank INT DEFAULT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_challenge_user (challenge_id, user_id),
    INDEX idx_challenge_id (challenge_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挑战参与者表';

-- 查看创建的表
SHOW TABLES LIKE '%workout%';
SHOW TABLES LIKE '%post%';
SHOW TABLES LIKE '%challenge%';
SHOW TABLES LIKE '%knowledge%';

-- 验证表结构
SELECT
    TABLE_NAME,
    TABLE_COMMENT,
    TABLE_ROWS
FROM
    INFORMATION_SCHEMA.TABLES
WHERE
    TABLE_SCHEMA = 'gymmind'
    AND TABLE_NAME IN (
        'workout_plans',
        'workout_plan_days',
        'knowledge_images',
        'posts',
        'comments',
        'post_likes',
        'challenges',
        'challenge_participants'
    )
ORDER BY TABLE_NAME;
