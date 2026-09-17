-- 创建挑战参与者表（包含外键约束）
-- 注意：需要先确保 challenges 表已经存在

CREATE TABLE IF NOT EXISTS challenge_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    progress INT DEFAULT 0 COMMENT '当前进度值',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '参与状态: ACTIVE-进行中, COMPLETED-已完成, ABANDONED-已放弃',
    rank INT DEFAULT NULL COMMENT '当前排名',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 唯一约束：每个用户只能加入一个挑战一次
    UNIQUE KEY uk_challenge_user (challenge_id, user_id),

    -- 索引
    INDEX idx_challenge_id (challenge_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_rank (rank),

    -- 外键约束：关联到 challenges 表
    CONSTRAINT fk_participant_challenge
        FOREIGN KEY (challenge_id)
        REFERENCES challenges(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    -- 外键约束：关联到 users 表（如果 users 表存在）
    CONSTRAINT fk_participant_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='挑战参与者表';

-- 验证表是否创建成功
SELECT
    TABLE_NAME,
    TABLE_COMMENT,
    CREATE_TIME
FROM
    INFORMATION_SCHEMA.TABLES
WHERE
    TABLE_SCHEMA = 'gymmind'
    AND TABLE_NAME = 'challenge_participants';

-- 查看表结构
DESCRIBE challenge_participants;

-- 查看所有外键约束
SELECT
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE
    TABLE_SCHEMA = 'gymmind'
    AND TABLE_NAME = 'challenge_participants'
    AND REFERENCED_TABLE_NAME IS NOT NULL;
