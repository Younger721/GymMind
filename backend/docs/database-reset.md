# GymMind 数据库安全重置

`reset-gymmind-db.ps1` 和 `reset-gymmind-db.sh` 只允许操作精确小写的 `gymmind` 数据库，不使用 Flyway，也不会拼接用户输入作为 SQL 标识符。

默认执行是 dry-run，只打印固定的 DROP/CREATE SQL，不连接数据库。真正执行必须同时提供执行开关和完整确认短语 `DROP gymmind`。mysql CLI 会交互提示密码，密码不会出现在命令行参数、日志或脚本文件中。

PowerShell：

```powershell
backend\scripts\reset-gymmind-db.ps1 -HostName localhost -Port 3306 -UserName $env:MYSQL_USER
backend\scripts\reset-gymmind-db.ps1 -HostName localhost -Port 3306 -UserName $env:MYSQL_USER -Execute -Confirmation 'DROP gymmind'
```

bash：

```bash
backend/scripts/reset-gymmind-db.sh --host localhost --port 3306 --user "$MYSQL_USER"
backend/scripts/reset-gymmind-db.sh --host localhost --port 3306 --user "$MYSQL_USER" \
  --execute --confirmation "DROP gymmind"
```

执行前应先用只读查询确认目标存在且名称精确：

```sql
SELECT SCHEMA_NAME
FROM INFORMATION_SCHEMA.SCHEMATA
WHERE SCHEMA_NAME = 'gymmind';
```

重置只删除并重建 `gymmind`，其他 schema 不在固定 SQL 的作用范围内。重建后使用开发配置的 Hibernate `ddl-auto=update` 让实体模型建表，并由系统目录初始化器幂等补齐角色、权限及关联。
