# Volume Manager 电视遥控适配版

基于 DDOneApps/VolumeManagerPlus（092fc5c），保留上游 GPL-2.0 许可及音量控制实现。

## 获取 APK：只在 GitHub Actions 构建

打开此仓库 **Actions → Build TV APK**，选择成功的运行，在 **Artifacts** 中下载 **Volume-Manager-TV**。解压得到 `Volume-Manager-TV.apk`，另附 SHA-256 校验值。

也可以在 Actions 中点击 **Run workflow** 手动构建。工作流会做单元测试、Android 编译、lint，以及独立的 Android 模拟器遥控 UI 测试。模拟器不包含小爱与真实电视系统，所以不等于实机音频兼容测试。

## 安装和首次使用

1. 需要 Android 13 或更高版本；没有降低上游最低版本或绕过权限要求。
2. 将 APK 通过甲壳虫 ADB、U 盘或已有侧载方式安装到电视。
3. 应用名称为 **应用音量 TV / Volume Manager TV**，包名 `moe.DDOne.volume.tv`。它与原版并存，不覆盖原版，也不继承原版配置。
4. **测试前停止原版的音量控制后台服务／撤销原版 Shizuku 授权，避免两个版本同时写入音量。**不需要卸载原版。
5. 启动 Shizuku 后，打开电视版，在“设置与权限”中请求授权。Shizuku 自己的授权窗口不属于本项目，若其窗口无法遥控，可临时通过 ADB/鼠标授权。
6. 授权后应用尝试启用原有辅助服务维持后台运行；失败信息会显示，可从设置重试。设备厂商的后台限制仍可能需要在电视设置处理。
7. 找到小爱同学，把音量先降到 30%，按返回退出界面并唤醒小爱实测。显示“等待下次播放”时仅代表当前没有可控制播放器，不代表该应用必然兼容。
8. 重启电视后检查 Shizuku 是否已启动。项目不保证自动启动 Shizuku。

## 遥控器

- 上／下：选择应用，列表自动滚动。
- 左／右：仅调整选中的应用，每次 5 个百分点，范围 0–100%；支持遥控器产生的重复按键。
- 确认：打开操作菜单（静音、30%、100%、返回）。
- 返回：先关闭菜单，再退出界面。
- 应用列表顶部为“设置与权限”，可请求授权、打开 Shizuku、重试后台服务。
- 电视实体音量键保持系统行为；电视版不弹出原来的触摸音量浮窗。
- 名称中包含“小爱”的应用优先显示；仍列出其他应用，不通过猜测包名强制控制系统服务。

## 构建与签名

`mobile` flavor 保留手机界面，`tv` flavor 始终使用电视界面。TV 桌面入口同时支持电视设备上的 mobile flavor。

CI 命令：

```sh
./gradlew :app:testTvDebugUnitTest :app:assembleTvDebug :app:assembleTvDebugAndroidTest :app:lintTvDebug
./gradlew :app:connectedTvDebugAndroidTest
```

交付的是可安装的 **debug 测试 APK**，不是商店发行版。仓库 Secret `TV_DEBUG_KEYSTORE_BASE64` 可保存固定的测试签名，以便后续覆盖安装；密钥不应提交到源码。没有该 Secret 的 fork 会自动使用临时 debug 签名，不同运行的 APK 可能需要先卸载旧电视版再安装（会丢失电视版配置）。

## 测试边界

纯按键规则包括 5% 步长、上下限、空列表、重复按键、忽略系统音量键。UI 测试覆盖选择应用、独立修改、确认菜单、授权入口、返回关闭菜单、长列表滚动。`TV-remote-UI-checks` 包含测试报告和示例界面截图；示例数据不代表实机检测结果。

仍需电视实测：小爱播报实际音量、唤醒提示音是否同一播放器、其他视频是否不受影响、服务重启后的设置恢复、长按重复和系统授权窗口兼容性。
