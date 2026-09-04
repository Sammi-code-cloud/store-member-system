# 八马门店后台管理系统（后端）

基于 **Spring Boot 3 + MySQL + MyBatis-Plus + Maven** 的门店后台服务，含 RBAC 权限、员工管理、会员储值、货品录入、扫码扣费、茶室预定、数据概览。

## 一、技术栈
- Spring Boot 3.2 / Java 17
- Spring Security 6（JWT 无状态认证 + 方法级权限 `@PreAuthorize`）
- MyBatis-Plus 3.5（分页、乐观锁、逻辑删除、字段自动填充）
- MySQL 8
- Maven

## 二、运行前准备
1. 安装 JDK 17、Maven 3.8+、MySQL 8。
2. 创建数据库（表结构和初始化数据由程序启动时自动执行）：
   ```sql
   CREATE DATABASE bama_store DEFAULT CHARACTER SET utf8mb4;
   ```
3. 配置数据库账号密码（**不要直接改 `application.yml`，该文件已脱敏并会提交到 Git**）：
   ```bash
   cd src/main/resources
   cp application-local.yml.example application-local.yml
   # 编辑 application-local.yml，填入真实的数据库地址、账号、密码、JWT 密钥
   ```
   `application-local.yml` 已在 `.gitignore` 中忽略，仅存在于本地，不会被提交。

## 三、启动
```bash
# 使用本地配置档启动（推荐，读取 application-local.yml 中的真实数据库）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 或打包后运行
mvn clean package
java -jar target/bama-store-admin-1.0.0.jar --spring.profiles.active=local

# 无需 MySQL 的快速演示（H2 内存库）
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
> 若不加 `--spring.profiles.active=local`，将使用 `application.yml` 中的默认占位值
> （`localhost:3306`、空密码），通常无法连接测试库。
> 也可改用环境变量注入：`DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` / `JWT_SECRET`。
启动成功后：
- 服务地址：`http://localhost:8080`
- 建表脚本 `db/schema.sql`、初始化数据 `db/data.sql` 自动执行（幂等，可重复启动）。
- 自动创建两个账号：

| 角色 | 手机号（账号） | 密码 | 权限 |
|------|----------------|------|------|
| 店长 | 13800000000 | admin123 | 全部 |
| 收银员 | 13800000001 | 123456 | 扫码收款、核销、储值、查看 |

## 四、核心接口

> 除登录外，所有接口需在请求头携带 `Authorization: Bearer {token}`。

### 认证
- `POST /api/auth/login` 登录，body：`{"phone":"13800000000","password":"admin123"}`
- `GET  /api/auth/me` 当前登录员工信息与权限

### 数据概览
- `GET /api/dashboard` 概览统计（需 `dashboard:view`）

### 员工与权限（RBAC）
- `GET  /api/staff` 员工分页（`staff:view`）
- `POST /api/staff` 录入员工并分配角色（`staff:manage`）
- `PUT  /api/staff/{id}/status?status=0|1` 启用/停用
- `PUT  /api/staff/{id}/password?password=xxx` 重置密码
- `GET  /api/roles` 角色列表
- `GET  /api/permissions` 权限列表

### 会员与储值
- `GET  /api/members` 会员分页（`member:view`）
- `GET  /api/members/{id}/account` 会员账户余额
- `POST /api/account/recharge` 储值（`account:recharge`），body：`{"memberId":1,"amount":3000,"giftAmount":500}`

### 扫码扣费（核心）
1. 生成付款码（正式环境由顾客端小程序调用，此处供联调）：
   `POST /api/paycode/generate?memberId=1` → 返回 `payCode`
2. 员工扫码解析：`POST /api/charge/resolve?payCode=xxx`（`charge:scan`）→ 返回会员信息与余额
3. 确认扣款：`POST /api/charge/confirm`（`charge:scan`）
   ```json
   {
     "memberId": 1,
     "items": [
       {"itemType":"ROOM","itemName":"观山茶室 2小时","price":188,"quantity":2},
       {"itemType":"PRODUCT","itemName":"大红袍 125g","price":288,"quantity":1}
     ],
     "remark": "到店消费"
   }
   ```
   返回扣款回执（原价、折后应扣、扣后余额、收款员工）。

### 货品录入
- `GET  /api/products` 分页（`product:view`）
- `POST /api/products` 录入/编辑（`product:manage`）
- `PUT  /api/products/{id}/status?status=0|1` 上下架
- `DELETE /api/products/{id}` 删除

### 茶室与预定
- `GET  /api/rooms` 茶室列表
- `POST /api/rooms` 新增茶室（`reservation:manage`）
- `GET  /api/reservations` 预定分页
- `POST /api/reservations` 创建预定（body：`{"memberId":1,"roomId":1,"reserveDate":"2026-08-30","startTime":"15:00","hours":2}`）
- `POST /api/reservations/{id}/verify` 核销（`reservation:verify`）

## 五、资金安全设计（重点）
- **会员余额与流水分离**：`t_member_account` 记余额，`t_wallet_txn` 记流水（只增不改）。
- **扣款一致性**：扣减在数据库事务内，账户表 `version` 字段乐观锁防并发超扣。
- **幂等**：流水表 `biz_no` 唯一索引，重复提交被拦截，防重复扣款。
- **付款码安全**：一次性、60 秒失效（`PayCodeService`，演示为内存实现，生产建议换 Redis）。
- **权限控制**：后端逐接口 `@PreAuthorize` 校验，前端隐藏按钮不作为安全边界。

## 六、目录结构
```
src/main/java/com/bama/store/
├── common/      统一返回体、异常、工具
├── config/      MyBatis-Plus、Security、初始化
├── security/    JWT、登录态
├── entity/      实体
├── mapper/      数据访问
├── service/     业务逻辑（AccountService 为扣款核心）
├── controller/  REST 接口
├── dto/ vo/     入参与出参
└── resources/db 建表与初始化脚本
```

## 七、生产注意事项
- `application.yml` 中 `spring.sql.init.mode` 改为 `never`，脚本交由 DBA 执行。
- JWT `secret` 用环境变量注入随机长密钥。
- 付款码存储改为 Redis；接入微信支付完成储值真实收款与回调对账。
