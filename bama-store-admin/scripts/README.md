# 公共审计字段迁移

MySQL 表统一使用：

```sql
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标识'
```

`schema.sql` 和启动时创建的新表采用上述定义。H2 测试环境的删除标记使用 INTEGER 0/1。`CREATE TABLE IF NOT EXISTS` 不会修改已有表；已有 MySQL 数据库使用 `migrate_audit_columns.py`。

由运维代码通过外部配置建立 PyMySQL 连接后调用：

```python
from migrate_audit_columns import migrate
print(migrate(connection, private_backup_directory, apply=False))
# 核对生成的 SQL 清单并验证备份可恢复后执行：
print(migrate(connection, private_backup_directory, apply=True))
```

备份目录必须位于不提交 Git 的私有目录。脚本在一致性快照中备份全库项目表结构和数据，保存校验值及迁移 SQL；不包含数据库连接凭据。备份 SQL 用于恢复到空的隔离数据库，不应直接导入正在使用的生产库。MySQL DDL 会隐式提交，无法用事务整体回滚；失败时检查已执行的语句、按需恢复，脚本可重新运行并跳过已匹配的字段。备份和迁移期间应避免并发发布或表结构变更。

已有非空创建时间、更新时间和删除标记保持原值；原创建时间为空或新增创建时间字段的历史记录回填迁移时间，该值不代表真实业务创建时间。删除标记包含 NULL 或 0/1 之外的值时迁移拒绝执行，不自行改写。转换 TIMESTAMP 使用 Asia/Shanghai 对应的 +08:00 会话时区。

本次统一字段结构，不全局改变删除业务语义：已有 BaseEntity 的 MyBatis-Plus 逻辑删除继续生效；关联替换、微信凭证消费及过期清理等原有物理删除仍保持。新增 deleted 字段不自动让原生 SQL 过滤记录，不应通过手工设置这些表的 deleted 停用数据。资金流水、消费回执和扣款幂等记录仍按原规则保存，不能因字段存在而随意删除。
