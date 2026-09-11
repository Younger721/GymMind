-- 动作库表（Exercise Library）
-- 用于存储健身动作的视频和详细信息

DROP TABLE IF EXISTS exercise_library;
CREATE TABLE exercise_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '动作名称（中文）',
    name_en VARCHAR(200) COMMENT '动作名称（英文）',
    category VARCHAR(100) NOT NULL COMMENT '动作类别：CHEST/BACK/LEGS/SHOULDERS/ARMS/CORE/CARDIO',
    difficulty VARCHAR(50) NOT NULL COMMENT '难度：BEGINNER/INTERMEDIATE/ADVANCED',
    equipment VARCHAR(100) COMMENT '所需器械：BARBELL/DUMBBELL/MACHINE/BODYWEIGHT/CABLE',

    -- 视频相关
    video_url VARCHAR(500) NOT NULL COMMENT '视频文件URL',
    video_thumbnail VARCHAR(500) COMMENT '视频缩略图URL',
    video_duration INT COMMENT '视频时长（秒）',
    video_source VARCHAR(50) COMMENT '视频来源：DOUYIN/TIKTOK/BILIBILI/YOUTUBE/UPLOAD',

    -- 动作描述
    description TEXT COMMENT '动作描述',
    target_muscles VARCHAR(200) COMMENT '目标肌群',
    instructions TEXT COMMENT '动作要领（JSON格式：["步骤1", "步骤2", ...]）',
    tips TEXT COMMENT '注意事项（JSON格式：["提示1", "提示2", ...]）',

    -- 统计数据
    view_count INT DEFAULT 0 COMMENT '观看次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',

    -- 索引和状态
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    is_verified BOOLEAN DEFAULT FALSE COMMENT '是否已验证（专业认证）',
    created_by BIGINT COMMENT '创建者用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_category (category),
    INDEX idx_difficulty (difficulty),
    INDEX idx_equipment (equipment),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),
    FULLTEXT INDEX ft_name (name, name_en, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作库表';

-- 用户收藏动作表
DROP TABLE IF EXISTS user_favorite_exercises;
CREATE TABLE user_favorite_exercises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_exercise (user_id, exercise_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (exercise_id) REFERENCES exercise_library(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏动作表';

-- 插入一些示例数据
INSERT INTO exercise_library (name, name_en, category, difficulty, equipment, video_url, video_thumbnail, description, target_muscles, instructions, tips, is_verified) VALUES
('杠铃卧推', 'Barbell Bench Press', 'CHEST', 'INTERMEDIATE', 'BARBELL', '/videos/bench-press.mp4', '/thumbnails/bench-press.jpg', '经典的胸部训练动作，主要锻炼胸大肌', '胸大肌、三角肌前束、肱三头肌', '["躺在平板凳上，双脚平放地面", "握距略宽于肩，杠铃下放至胸部", "推起杠铃至手臂伸直，保持控制"]', '["保持肩胛骨后缩下沉", "避免弹震式动作", "呼气推起，吸气下放"]', TRUE),
('引体向上', 'Pull-up', 'BACK', 'INTERMEDIATE', 'BODYWEIGHT', '/videos/pullup.mp4', '/thumbnails/pullup.jpg', '经典的背部训练动作，锻炼背阔肌和二头肌', '背阔肌、斜方肌、肱二头肌', '["双手正握单杠，握距略宽于肩", "启动背部肌肉，将身体拉向单杠", "下巴超过单杠后缓慢下放"]', '["避免借助惯性摆动", "保持核心收紧", "全程控制动作速度"]', TRUE),
('深蹲', 'Barbell Squat', 'LEGS', 'INTERMEDIATE', 'BARBELL', '/videos/squat.mp4', '/thumbnails/squat.jpg', '下肢训练之王，全面锻炼腿部肌群', '股四头肌、臀大肌、腘绳肌', '["杠铃置于斜方肌上部，双脚与肩同宽", "膝盖与脚尖方向一致，下蹲至大腿平行地面", "推动脚跟，站起至起始位置"]', '["保持腰背挺直", "膝盖不要内扣", "重心在脚跟"]', TRUE),
('哑铃肩推', 'Dumbbell Shoulder Press', 'SHOULDERS', 'BEGINNER', 'DUMBBELL', '/videos/shoulder-press.mp4', '/thumbnails/shoulder-press.jpg', '肩部训练的基础动作，锻炼三角肌', '三角肌、肱三头肌', '["坐姿，哑铃举至肩部两侧", "向上推举哑铃至手臂伸直", "缓慢下放至起始位置"]', '["避免过度后仰", "保持核心稳定", "控制动作轨迹"]', TRUE),
('平板支撑', 'Plank', 'CORE', 'BEGINNER', 'BODYWEIGHT', '/videos/plank.mp4', '/thumbnails/plank.jpg', '核心力量训练的经典动作', '腹直肌、腹横肌、腹斜肌', '["前臂和脚尖支撑身体", "保持身体呈一条直线", "收紧核心，保持姿势"]', '["避免塌腰或抬臀", "保持自然呼吸", "循序渐进增加时间"]', TRUE);
