-- 创建挑战参与者表
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
    INDEX idx_rank (rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挑战参与者表';

-- 验证表是否创建成功
SHOW CREATE TABLE challenge_participants;

SELECT
    TABLE_NAME,
    TABLE_COMMENT
FROM
    INFORMATION_SCHEMA.TABLES
WHERE
    TABLE_SCHEMA = 'gymmind'
    AND TABLE_NAME = 'challenge_participants';
