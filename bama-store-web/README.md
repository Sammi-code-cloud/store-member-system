# 八马门店管理后台（Web）

面向店长 / 管理员的 PC 端管理后台，配合后端 `bama-store-admin` 使用。
除后台自身的资料管理职能外，还整合了**员工端的代客储值、数据概览、预定核销**三项功能，
店长无需切换到小程序即可在 PC 上完成日常运营操作。

## 一、技术栈

- Vue 3（Composition API，`<script setup>`）
- Vite 5 构建，开发端口 `5174`
- Element Plus 2（中文语言包）+ @element-plus/icons-vue
- Pinia 状态管理、Vue Router 4（hash 模式）
- axios（统一拦截 Token 与错误提示）

## 二、目录结构

```
bama-store-web/
├── index.html
├── vite.config.js         端口、别名、/api 代理配置
└── src/
    ├── main.js            应用入口，注册 Element Plus 与全部图标
    ├── style.css          全局样式与主题变量（八马红/茶金/茶绿）
    ├── api/index.js       后端接口封装，与 Controller 一一对应
    ├── utils/request.js   axios 实例：JWT 注入、Result 拆包、401 跳登录
    ├── store/user.js      登录态与权限码集合
    ├── router/index.js    路由表（含权限码 meta）与登录守卫
    ├── layout/index.vue   侧边栏 + 顶栏布局，菜单按权限过滤
    └── views/             9 个业务页面
```

## 三、功能与权限对照

| 分组 | 页面 | 权限码 | 说明 | 来源 |
|---|---|---|---|---|
| 门店运营 | 数据概览 | `dashboard:view` | 6 项统计 + 今日待核销预定 | **员工端** |
| 门店运营 | 代客储值 | `account:recharge` | 三步式：搜会员 → 填金额 → 完成 | **员工端** |
| 门店运营 | 预定核销 | `reservation:view` / `reservation:verify` | 按状态筛选、分页、核销 | **员工端** |
| 资料管理 | 会员管理 | `member:view` | 搜索、分页、账户详情抽屉 | 后台 |
| 资料管理 | 货品管理 | `product:view` / `product:manage` | 增删改、上下架、低库存标红 | 后台 |
| 资料管理 | 茶室管理 | `reservation:manage` | 增删改茶室与价格 | 后台 |
| 系统设置 | 员工与权限 | `staff:view` / `staff:manage` | 新增员工、分配角色、重置密码、权限清单 | 后台 |
| 系统设置 | 门店设置 | `store:manage` | 门店名称、地址、电话、营业状态 | 后台 |

> **未包含扫码收款**：付款码扫描依赖摄像头，是员工手机端的场景，PC 后台不重复实现。

权限控制为双层：
1. **前端**：无权限的菜单不显示，无权限的按钮置灰（`userStore.has('权限码')`）。
2. **后端**：`@PreAuthorize` 注解拦截，前端绕过也无法调用——前端控制仅为体验优化。

## 四、运行

```bash
npm install
npm run dev        # http://localhost:5174，自动打开浏览器
npm run build      # 构建产物输出到 dist/
npm run preview    # 预览构建产物
```

**前置条件**：后端 `bama-store-admin` 已在 `http://localhost:8080` 启动。

开发期请求通过 Vite 代理转发，前端只写 `/api/xxx`，不硬编码后端地址。
若后端不在默认地址，设置环境变量即可，无需改代码：

```bash
# Windows PowerShell
$env:VITE_API_TARGET="http://192.168.1.100:8080"; npm run dev
```

## 五、登录

使用后端 `t_staff` 表中的员工账号登录（与员工端小程序同一套账号）。
演示环境默认账号见项目根目录 README，生产环境请先在「员工与权限」中录入正式账号并停用演示账号。

## 六、生产部署

```bash
npm run build
```

将 `dist/` 部署到 Nginx，并把 `/api` 反向代理到后端服务：

```nginx
server {
    listen 80;
    root /var/www/bama-store-web/dist;

    location / {
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 七、已知事项

- Element Plus 目前为全量引入，打包后单 chunk 约 1.2 MB（gzip 约 388 KB）。
  若需优化，可接入 `unplugin-vue-components` 做按需引入。
- 门店设置仅支持单门店（门店 ID 取当前登录员工的 `storeId`），
  多门店管理需后端补充门店列表接口后开放。
- 数据概览的「今日待核销预定」取预定列表前 50 条再按当日过滤，
  数据量增大后建议后端提供按日期查询的专用接口。
