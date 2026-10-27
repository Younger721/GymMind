package com.gymmind.platform.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 兼容旧库：新排期只写 workout_plan_id，member_plan_id 须允许为空。
 */
@Component
@Order(50)
public class MemberWorkoutScheduleSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public MemberWorkoutScheduleSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE member_workout_schedule ADD COLUMN workout_plan_id BIGINT NULL");
        } catch (DataAccessException ignored) {
            // 列已存在或非 MySQL 环境时忽略
        }
        try {
            jdbc.execute("ALTER TABLE member_workout_schedule MODIFY COLUMN member_plan_id BIGINT NULL");
        } catch (DataAccessException ignored) {
            // 测试库或已迁移时忽略
        }
    }
}
