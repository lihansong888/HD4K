# 服务器示例文件

App 启动时会访问两个地址，地址写在 `app/build.gradle`，可用 `gradle.properties` 里的 `hd4kUpdateUrl` / `hd4kConfigUrl` 覆盖（设为空表示关闭对应功能）。

| 用途 | 默认地址 | 说明 |
|---|---|---|
| 检查更新 | `https://api.lihansong.top/update.json` | 格式见下，纯静态文件即可，宝塔里建一个站点放进去 |
| 默认配置 | `https://api.lihansong.top/config.json` | 首次启动自动使用；用户自己填写过别的地址后以用户的为准 |

两个地址都必须是 **https**。

## update.json

`update.json` 在本目录，里面的 `size` 为 0、`sha256` 为占位文字，所以直接照抄上线也不会弹出更新（客户端会忽略不完整的条目）。

字段：

- `version`：最新版本号，必须是 `数字.数字.数字`，并且**必须和 APK 里的 versionName 完全一致**。
- `notes`：更新说明。可以是一段字符串（所有设备共用），也可以像示例那样按 `leanback` / `mobile` 分开写；每行一条，行首的 `- ` 会显示成圆点。
- `files`：每个安装包一项，键名固定为 `{版本类型}-{CPU架构}.apk`，也就是编译出来的正式包文件名：
  - `leanback-arm64_v8a.apk`、`leanback-armeabi_v7a.apk`（电视、盒子）
  - `mobile-arm64_v8a.apk`、`mobile-armeabi_v7a.apk`（手机、平板）
  - 模拟器如果是 x86 会找不到对应项，不提示更新，属于正常现象。
  - `url`：安装包下载地址（https）。
  - `size`：文件字节数，Linux 下用 `stat -c %s 文件名` 取得。
  - `sha256`：64 位十六进制，Linux 下用 `sha256sum 文件名` 取得。

发布新版本的步骤：

1. 把 `app/build.gradle` 里的 `versionCode` 加大、`versionName` 改成新版本，用**同一个签名文件**编译。
2. 把四个 APK 传到服务器下载目录。
3. 算出每个文件的 `size` 和 `sha256`，写进 `update.json`，`version` 与 `versionName` 保持一致。
4. 最后再更新 `update.json`，用户才会看到更新提示。

客户端会校验：包名一致、版本号更大、文件大小和 SHA-256 吻合，并且 Android 本身要求签名一致才能覆盖安装。

## config.json

内容格式和 TVBox 系应用的配置文件一致（站点、直播、解析等），内容由你自己提供，请只接入有合法授权的片源。本目录不提供示例，避免把无关内容带进来。

如果这个地址暂时打不开，App 首次启动会提示获取配置失败，但不影响用户在「设置」里手动填写其他配置地址。
