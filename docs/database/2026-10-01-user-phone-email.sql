-- 用户邮箱可选，并保存联系电话。
ALTER TABLE sys_user
    MODIFY COLUMN normalized_email VARCHAR(320) NULL;

ALTER TABLE sys_user
    ADD COLUMN phone VARCHAR(32) NULL;
