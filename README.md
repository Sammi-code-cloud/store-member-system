# 八马门店会员系统（store-member-system）

面向茶叶门店的会员储值与扫码收款系统，覆盖**顾客端**、**员工端**、**管理后台**三端，
核心能力为：会员储值 → 顾客出示动态付款码 → 员工扫码扣费 → 实时回执。

---

## 一、系统架构

```
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  顾客端小程序  │   │  员工端小程序  │   │  Web 管理后台 │
│  储值/预定/    │   │  扫码收款/     │   │  会员/货品/   │
│  付款码/我的   │   │  代客储值/核销 │   │  员工/权限    │
└───────┬──────┘   └───────┬──────┘   └───────┬──────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │  HTTP + JWT
                  ┌────────┴────────┐
                  │  bama-store-admin │  Spring Boot 3.2.5
                  │  （后端服务）      │  MyBatis-Plus + Security
                  └────────┬────────┘
                           │
                     ┌─────┴─────┐
                     │  MySQL 8   │  14 张业务表
                     └───────────┘
```

## 二、三端与仓库目录对应

| 端 | 目录 | 说明 | 状态 |
|---|---|---|---|
| 顾客端 | `bama-store-miniapp/pages/customer/` | 首页、茶室预定、付款码、我的 | ✅ 已完成 |
| 员工端 | `bama-store-miniapp/pages/staff/` | 登录、工作台、扫码、收款、储值、概览 | ✅ 已完成 |
| 演示页 | `demo-h5/` | 纯 HTML 实机演示（免装小程序环境，浏览器直接跑） | ✅ 已完成 |
| 后端服务 | `bama-store-admin/` | Spring Boot，11 个 Controller | ✅ 已完成 |
| 管理后台 | `bama-store-web/` | 数据概览、代客储值、预定核销、会员、货品、茶室、员工权限、门店设置 | ✅ 已完成 |

> **命名提示**：`bama-store-admin` 是**后端服务**工程；`bama-store-web` 才是管理后台前端。
>
> 管理后台除自身的资料管理职能外，还整合了**员工端的代客储值、数据概览、预定核销**三项功能，
> 店长无需切换到小程序即可在 PC 上完成日常运营。扫码收款依赖摄像头，仍保留在员工手机端。

## 三、技术栈

| 层 | 技术选型 |
|---|---|
| 后端 | JDK 17、Spring Boot 3.2.5、Spring Security、MyBatis-Plus 3.5.5、JJWT 0.12.5 |
| 数据库 | MySQL 8（另提供 H2 内存库启动档，免安装快速演示） |
| 小程序 | uni-app（Vue3），一套代码编译到微信小程序 / H5 / App |
| 演示页 | 原生 HTML + Node 原生 http 静态服务器（零依赖） |
| 管理后台 | Vue3.5 + Vite 5 + Element Plus 2 + Pinia + Vue Router 4 + axios |

## 四、目录结构

```
store-member-system/
├── bama-store-admin/                后端服务（Spring Boot）
│   ├── src/main/java/com/bama/store/
│   │   ├── controller/              11 个 Controller
│   │   ├── service/                 业务逻辑（扣费事务、付款码、权限）
│   │   ├── entity/ mapper/          14 张表的实体与 Mapper
│   │   ├── security/                JWT 认证与鉴权
│   │   └── config/                  MyBatis-Plus、Security、初始化数据
│   └── src/main/resources/
│       ├── application.yml          主配置（已脱敏，占位符注入）
│       ├── application-h2.yml       H2 内存库启动档
│       ├── application-local.yml.example   本地配置模板 ← 复制它
│       └── db/                      建表与初始化脚本（幂等）
├── bama-store-web/                  Web 管理后台（Vue3 + Vite + Element Plus）
│   ├── src/api/                     接口封装
│   ├── src/router/                  路由表（含权限码）与登录守卫
│   ├── src/layout/                  侧边栏布局，菜单按权限过滤
│   ├── src/store/                   Pinia 登录态与权限集合
│   └── src/views/                   9 个业务页面
├── bama-store-miniapp/              uni-app 小程序（顾客端 + 员工端）
├── demo-h5/                         纯 HTML 演示页
│   ├── index.html                   员工端
│   ├── customer.html                顾客端
│   └── server.js                    静态服务器（端口 8081）
└── README.md
```

## 五、快速开始

### 1. 环境要求
JDK 17、Maven 3.8+、MySQL 8、Node 16+（仅演示页与小程序需要）

### 2. 配置数据库（重要）

**不要直接修改 `application.yml`**——该文件已脱敏并会提交到 Git。正确做法：

```bash
cd bama-store-admin/src/main/resources
cp application-local.yml.example application-local.yml
# 编辑 application-local.yml，填入真实数据库地址、账号、密码、JWT 密钥
```

`application-local.yml` 已在 `.gitignore` 中忽略，仅存在于本地，不会被提交。
数据库连接信息请向项目负责人或 DBA 索取。

### 3. 启动后端

```bash
cd bama-store-admin
mvn spring-boot:run -Dspring-boot.run.profiles=local     # 连真实数据库
mvn spring-boot:run -Dspring-boot.run.profiles=h2        # 免装 MySQL，H2 内存库
```
首次启动自动建表并写入初始化数据（脚本幂等，可重复启动）。

### 4. 启动演示页（另开终端）

```bash
cd demo-h5
node server.js
```

### 5. 启动管理后台

```bash
cd bama-store-web
npm install
npm run dev              # http://localhost:5174，自动打开浏览器
```
开发期请求经 Vite 代理转发到后端，前端不硬编码后端地址。
后端不在默认地址时，用环境变量指定，无需改代码：
```bash
# Windows PowerShell
$env:VITE_API_TARGET="http://192.168.1.100:8080"; npm run dev
```

### 6. 启动小程序（可选）

```bash
cd bama-store-miniapp
npm install
npm run dev:h5           # H5 预览
npm run dev:mp-weixin    # 微信小程序，产物在 dist/dev/mp-weixin
```
或用 HBuilderX 直接导入本目录运行（无需 npm，更省事）。

## 六、访问地址

| 端 | 地址 |
|---|---|
| **员工端** | http://localhost:8081/index.html |
| **顾客端** | http://localhost:8081/customer.html |
| **管理后台** | http://localhost:5174 |
| 后端 API | http://localhost:8080 |
| H2 控制台（仅 h2 档） | http://localhost:8080/h2-console |
| 小程序 H5 预览 | http://localhost:5173 |

>  - 5173 为 Vite 默认端口，被占用时自动递增，以终端实际输出为准。
> - 真机演示需把代码中的 `localhost` 改为电脑局域网 IP，位置见「十一、注意事项」。

## 七、演示账号

| 角色 | 手机号 | 密码 | 权限 |
|---|---|---|---|
| 店长 | 13800000000 | admin123 | 全部 |
| 收银员 | 13800000001 | 123456 | 扫码收款、核销、储值 |

账号由 `DataInitializer` 在员工表为空时自动创建。**生产环境务必修改初始密码。**

## 八、核心流程：扫码收款闭环

浏览器开两个标签页即可完整演示：

1. **顾客端** → 底部第 3 个 tab「付款码」→ 生成动态付款码
   （一次性 token，含 member_id + 时间戳 + 签名，有效期 60 秒，防重放）
2. **员工端** → 用收银员账号登录 → 「扫码收款」→ 粘贴该付款码
3. 后端解析 token → 返回会员姓名、等级、卡内余额、等级折扣
4. 填写消费明细 → 确认扣款（**数据库事务 + 乐观锁 + 幂等控制**，防重复扣费与超扣）
5. 返回回执：扣款金额、剩余余额、收款员工、订单号

## 九、数据库

14 张业务表，建表脚本 `bama-store-admin/src/main/resources/db/schema.sql`：

| 模块 | 表 |
|---|---|
| 门店 | `t_store` |
| 员工与权限（RBAC） | `t_staff`、`t_role`、`t_permission`、`t_staff_role`、`t_role_permission` |
| 会员与资金 | `t_member`、`t_member_account`、`t_txn` |
| 茶室与预定 | `t_tea_room`、`t_reservation` |
| 货品与消费 | `t_product`、`t_consume_order`、`t_consume_item` |

## 十、接口概览

| 前缀 | 用途 |
|---|---|
| `/api/auth` | 员工登录、获取 Token |
| `/api/customer/**` | 顾客端：门店信息、货品、茶室、预定 |
| `/api/paycode/**` | 动态付款码生成与解析 |
| `/api/charge/**` | 扫码收款：解析付款码、确认扣款 |
| `/api/members` `/api/account` | 会员管理、储值充值 |
| `/api/staff` `/api/rooms` `/api/products` `/api/reservations` `/api/store` | 后台管理类接口 |
| `/api/dashboard` | 数据概览统计 |

接口明细见 [`bama-store-admin/README.md`](bama-store-admin/README.md)。

## 十一、注意事项

### 敏感信息
- 数据库密码、JWT 密钥**一律不入库**，通过 `application-local.yml` 或环境变量
  （`DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` / `JWT_SECRET`）注入。
- `.gitignore` 已拦截：`mysql.txt`、`application-local.yml`、`target/`、`*.log`、`*.bak`。
- 提交前建议执行 `git status` 核对，确认无上述文件。

### 接口地址写死的位置
换机器或部署时需同步修改以下 3 处：
- `demo-h5/index.html` 第 203 行 `const BASE = 'http://127.0.0.1:8080'`
- `demo-h5/customer.html` 第 362 行 `const BASE = 'http://127.0.0.1:8080'`
- `bama-store-miniapp/common/request.js` 第 4 行 `BASE_URL = 'http://localhost:8080'`

### 生产部署
- `spring.sql.init.mode` 改为 `never`，建表交由 DBA 手动执行
- `JWT_SECRET` 替换为随机长密钥（≥32 字节，`openssl rand -base64 48`）
- 关闭 MyBatis SQL 日志打印、调整 `logging.level` 为 `info`
- 微信小程序需已备案的 HTTPS 域名，并在微信公众平台配置 request 合法域名

## 十二、待办

- [x] ~~Web 管理后台前端（Vue3 + Element Plus）~~ 已完成，见 `bama-store-web/`
- [ ] 管理后台接入 Element Plus 按需引入，优化打包体积（当前单 chunk 约 1.2 MB）
- [ ] 多门店管理（后端需补充门店列表接口，当前后台仅支持单门店）
- [ ] 顾客端微信 openid 登录（当前顾客身份为演示数据，接口位置已在 `common/api.js` 预留）
- [ ] 微信支付对接（当前储值为后台代充）
- [ ] 消费流水报表导出
