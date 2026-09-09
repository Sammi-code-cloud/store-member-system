# 首页 Banner 管理

后台「资料管理 → 首页 Banner」管理当前分店图片，切换顶部分店后可分别配置。

- 上传 / 替换 JPG、PNG 图片，建议 1500 × 750（2:1），单张不超过 2MB，每分店最多 10 张。
- 可设置标题、排序、启用 / 停用、点击进入预订茶室或不跳转。排序数字越小越靠前。
- 图片保存在后端数据库。保存后顾客重新进入首页会加载新内容；多张每 5 秒轮播，也可手动切换。
- 停用或删除全部图片后，首页显示原有茶室预订介绍。删除过的默认图不会因重启恢复。
- 上传与删除沿用「门店设置」权限，服务端按员工当前分店校验，并记入操作记录。
- 当前 H2 是临时演示库，重启会重置数据；正式部署使用持久数据库。图片接口须与小程序 API 一起配置 HTTPS 域名。

默认图片：`bama-store-admin/src/main/resources/banners/tea-welcome.png`。由内置 imagegen 生成，用于初始演示，后台可直接替换。
生成提示词：Create a photorealistic editorial banner photograph for a Chinese tea room booking miniapp. Landscape 2:1 composition. Warm burnt-orange and cream color palette, peaceful premium natural afternoon lighting. A beautiful ivory porcelain gaiwan and two small cups with amber tea on a pale wood tea tray, a subtle orange linen cloth, softly blurred traditional tea room background with a window. Place tea objects on the right half and leave calm darker warm brown negative space on the left for UI headline overlay. Realistic materials, refined minimal composition, no text, no logos, no people. This is a homepage tea lifestyle banner, not a product packshot.

多图轮播：底部圆点可指定图片，左右按钮 / 左右滑动可循环切换，自动轮播间隔5秒。手动切换后重新计时；离开首页暂停。默认示例现在包括茶席图与茶室图两张。已配置过的分店不会在重启时追加示例图。
第二张图片：`bama-store-admin/src/main/resources/banners/tea-room.png`，由内置 imagegen 生成。
提示词：A photorealistic wide 2:1 homepage banner for a Chinese tea room booking miniapp. An inviting elegant private tea room, warm cream walls and burnt orange accents, wooden tea table with porcelain tea service on the RIGHT, soft sunlight through wooden lattice windows, quiet premium welcoming atmosphere, natural realistic interior photography. Left half softly darker with calm negative space for UI headline. No people, no text, no logos. Cohesive warm orange and cream palette.
实际发布的第二张图使用同目录 `tea-room.jpg`（由生成原图转为 JPEG，以减小传输体积）；PNG 原图保留供后续替换。
