# 高清4K影视

自研通用影视播放器（Kotlin + Jetpack Compose + Media3），支持手机 / 平板 / 电视 / 盒子 / 模拟器。

- 内容来自 `hd4kCatalogUrl` 指向的 JSON 目录（格式见 `server-samples/catalog.json`），未配置时使用内置示例。
- 更新检查读取 `hd4kUpdateUrl`（格式见 `server-samples/update.json`）。
- 以上两项在 `gradle.properties` 中配置。
- 测试签名 `ci/test.jks` 仅用于测试包；正式发布请换成自己的签名。
