# 房间预约员工通知（WxPusher）

顾客成功提交预约（PENDING）时，后端在预约事务内写入 `t_reservation_notice`。事务失败则预约和通知一起回滚。后台每 5 秒取出通知，从数据库读取预约所属门店的员工 UID 分别发送。员工代客创建预约不触发此通知。

## 启用

1. 在 [WxPusher 管理后台](https://wxpusher.zjiecode.com/admin/)创建应用，取得 `AT_` 开头的 AppToken。
2. 让接收员工关注该应用，取得各自 `UID_` 开头的 UID，并确认员工已配置可用的接收渠道。[官方接入文档](https://wxpusher.zjiecode.com/docs/)。
3. 把下面配置合并到后端**私有** `application-local.yml` 的现有 `bama` 节点（不要重复写两个 `bama`），或生产环境对应的外部配置。AppToken 可以直接填写在该私有文件，或使用环境变量。不要把真实 Token 提交到 Git。

```yaml
bama:
  wxpusher:
    enabled: true
    app-token: ${WXPUSHER_APP_TOKEN}
```

4. 使用 `local` 配置档重新启动后端（生产使用对应外部配置）。默认关闭外部发送；无配置时顾客预约仍保存通知，启用后将补发积压预约，内容带有原预约日期，请先检查积压记录。私有配置不打包进 JAR，部署时需同步服务器的外部配置。
5. 管理后台切换到目标门店，进入 **员工与权限 → 对应员工的「预约通知」**，填写员工 UID 并保存。UID 存在 `t_staff_wxpusher`，保存后立即生效，无需重启。清空 UID 可关闭该员工在当前门店的通知。入口需要 `staff:manage` 权限，并沿用员工管理的门店范围及角色权限检查。
6. 以顾客身份实际预约一个可用房间，检查该门店每位配置员工是否收到通知。内容包括预约单号、房间、日期、开始时间、时长、人数、联系人、联系电话和备注。取消测试预约。

每个门店分别设置接收员工；没有全局收件人。员工停用、删除或失去该门店权限后，发送时自动排除，尚未发送的通知标记 REMOVED。一个 UID 被同门店多名员工配置时，每笔预约只推送一次。原 YAML `store-uids` 配置不再作为收件人来源，升级后需在后台重新设置。

## 重试与排查

服务启动时自动幂等创建通知表及员工 UID 配置表（MySQL/H2）。数据库账号需有建表权限；若生产由 DBA 管理 DDL，请先执行 `ReservationNoticeSchema` 中等价建表语句。

```sql
SELECT event_key, store_id, receiver_uid, status, attempts,
       next_attempt, message_id, error_code, create_time
FROM t_reservation_notice
ORDER BY create_time DESC;
```

状态：NEW 等待展开收件人；EXPANDED 已生成各员工通知；PENDING 待发送/待重试；SENDING 正在发送；ACCEPTED 为平台接受；REMOVED 为收件人已从配置移除。`NO_STORE_RECIPIENTS` 表示缺少本门店有效 UID，配置补齐后会继续发送。AppToken 不可用时任务保留，并输出配置错误日志。

每名员工单独记录结果；某名员工失败不会重发给已经成功的员工。失败退避重试，最长间隔 1 小时，任务持久化且不会因进程重启丢失。数据库租约避免多个实例同时领取同一条通知，过期 60 秒后可以恢复。

`message_id` 保存官方返回的 `sendRecordId`（兼容旧版 `messageId`）。ACCEPTED 只表示平台接收，不能证明设备展示或员工阅读。网络超时或发送后进程崩溃时可能重复通知（至少一次投递），请按订单号辨认；第三方渠道不可用时无法保证即时送达。当前不接入送达回调。

自动化验证使用 H2 和模拟 WxPusher，不向真实员工发送。真实端到端收件验证需要部署并填写本方应用凭据。
