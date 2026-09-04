# 八马门店小程序（uni-app · Vue3）

顾客端 + 员工端合一的小程序，配合后端 `bama-store-admin` 使用。

## 一、技术栈
- uni-app（Vue3），一套代码可编译到微信小程序 / H5 / App
- 主题与后台设计图一致（八马红 / 茶金 / 茶绿）

## 二、目录结构
```
bama-store-miniapp/
├── pages.json            页面路由与全局样式
├── manifest.json         应用配置（appid、平台设置）
├── main.js / App.vue     入口
├── uni.scss              主题变量（全局注入）
├── common/
│   ├── request.js        请求封装（含 token、统一错误处理）
│   ├── api.js            后端接口方法
│   ├── store.js          登录态管理
│   └── mock.js           顾客端演示数据
└── pages/
    ├── role/select       角色入口（选顾客/员工）
    ├── customer/         顾客端：home / paycode / recharge / rooms / profile
    └── staff/            员工端：login / workbench / scan / charge / recharge / dashboard
```

## 三、运行方式

### 方式一：HBuilderX（推荐，最简单）
1. 下载安装 HBuilderX（自带 uni-app 编译，无需 npm）。
2. 菜单「文件 → 导入 → 从本地目录导入」，选择本目录 `bama-store-miniapp`。
3. 顶部「运行 → 运行到小程序模拟器 → 微信开发者工具」（需先装微信开发者工具并开启服务端口），
   或「运行到浏览器」（H5 预览）。

### 方式二：命令行 CLI
```bash
npm install
npm run dev:h5           # H5 预览
npm run dev:mp-weixin    # 微信小程序，产物在 dist/dev/mp-weixin，用微信开发者工具打开
```
> 若 CLI 依赖版本报错，可用官方模板重建后把本项目 `pages/`、`common/`、`pages.json` 等拷入：
> `npx degit dcloudio/uni-preset-vue#vite bama-store-miniapp`

## 四、联调后端
1. 先启动后端 `bama-store-admin`（默认 `http://localhost:8080`）。
2. `common/request.js` 中 `BASE_URL` 指向后端地址。
3. 微信开发者工具中勾选「设置 → 本地设置 → 不校验合法域名」，即可请求 localhost。
4. 真机 / 正式发布：后端需换成已备案的 **https 域名**，并在微信公众平台配置 request 合法域名。

## 五、可跑通的真实流程（员工端，后端已支持）
1. 角色入口选「我是员工」→ 用演示账号登录（收银员 `13800000001 / 123456`）。
2. 顾客端「付款码」页生成付款码（真实接口 `POST /api/paycode/generate`）。
3. 员工端「扫码收款」→ 手动粘贴该付款码（或真机扫码）→ 识别会员。
4. 「收款确认」填/选消费明细 → 确认扣款（真实扣会员卡余额，事务+乐观锁+幂等）。
5. 「会员储值」为会员充值；「数据概览」查看统计；工作台可核销预定。

## 六、说明
- **员工端**：全部对接后端真实接口。
- **顾客端**：付款码为真实接口；首页/储值/茶室/个人中心的余额、流水等为**演示数据**（页面已标注）。
  这些需后端补充「微信 openid 登录」及顾客侧查询接口后替换为真实调用，
  接口位置已在 `common/api.js` 预留，改动量很小。
