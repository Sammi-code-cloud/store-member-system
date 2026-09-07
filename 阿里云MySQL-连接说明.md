# MySQL 连接说明

实际数据库地址、账号、密码、SSH 信息及迁移记录仅保存在本地，不提交到仓库。

## 本地配置

复制 `bama-store-admin/src/main/resources/application-local.yml.example` 为同目录下的 `application-local.yml`，填写数据库连接参数。该配置已被 Git 忽略。

若数据库仅允许内网访问，先通过已授权的 SSH 主机建立隧道：

```powershell
ssh -N -L <LOCAL_PORT>:<DB_HOST>:<DB_PORT> <SSH_USER>@<SSH_HOST>
```

将本地配置中的数据库地址设为 `127.0.0.1`，端口设为 `<LOCAL_PORT>`，并在后端运行期间保持隧道连接。

## 启动

完成本地数据库及微信配置后，在项目根目录运行：

```powershell
./bama-store-admin/scripts/start-local.ps1
```

数据库切换不代表已完成数据迁移或连接验证；请根据实际环境验证启动和业务接口。
