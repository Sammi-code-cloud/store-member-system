# 项目文档

三个工程保留在根目录，各自的 README 介绍启动方式。本文档目录只保存可公开内容，以下路径均相对于仓库根目录。

| 分类 | 内容 |
| --- | --- |
| `docs/guides/` | 后台、预约、首页 Banner 和员工微信绑定使用说明 |
| `docs/integrations/` | 微信、短信等外部服务接入说明 |
| `docs/deployment/` | 通用部署流程与 MySQL 配置说明 |
| `docs/design/` | 后台功能方案 |
| `assets/branding/` | 八马小程序头像原图 |
| `.local/operations/` | 本机完整部署历史，不提交 |
| `.local/config/` | 本机服务器连接资料，不提交 |
| `.local/design/` | 原始计划与设计预览，不提交 |
| `bama-store-admin/.local/` | 现有私有发布脚本、备份和验证产物，不提交 |

常用入口：

- [后台使用说明](guides/后台管理端-使用说明.md)
- [员工微信绑定](guides/员工微信绑定-使用说明.md)
- [部署说明](deployment/部署说明.md)
- [MySQL 连接](deployment/阿里云MySQL-连接说明.md)
- [微信接入](integrations/微信登录-接入说明.md)

部分早期说明记录的是当时的功能设计，当前实现以代码为准。禁止将真实密码、会员数据、微信密钥或服务器地址写入公开文档。
