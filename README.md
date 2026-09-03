# ForceFocus v16 原样 UI Android 版

本工程只使用一个 Android `WebView` 加载最终确认的 `ForceFocus_v16.html`。网页承担全部视觉界面与网页交互，Android 原生层只承担应用启动、无障碍白名单限制、精确闹钟、通知和持久化。

## 唯一视觉基准

- 文件：`app/src/main/assets/ForceFocus_v16.html`
- SHA-256：`05eb065e5db421f28fa1db720764e2efed0b9bb1eebcf3f74e1838851f9b5f2f`
- `MainActivity` 直接加载：`file:///android_asset/ForceFocus_v16.html`
- `native-bridge.js` 在页面完成后单独注入，不修改 HTML、CSS、扇形几何、图片或日历布局。

## 原生能力

- 当前任务白名单：简历→WPS；岗位调研→小红书+WPS；考公→粉笔；磨耳朵→录音机+WPS。
- 无障碍服务在专注期间拉回非白名单 App，同时放行系统设置、权限控制器、系统界面和输入法。
- 周六、周日自动使用三个时段：09:00–11:30、13:30–17:00、19:00–21:30；每段开始前 5 分钟声音、通知和震动提醒。
- Android 12+ 优先使用精确闹钟；未授权时自动退化为系统允许的待机闹钟。
- Android 13+ 运行时申请通知权限。
- 专注状态、时长锁、最近三个不同时长、每周两次提前结束、周末任务、周末锁、日期竹叶标记和每日学习时长均使用 `SharedPreferences` 持久化。
- 手机重启、系统时区或时间变化、应用更新后会重新安排闹钟并恢复未结束状态。

## 构建

GitHub Actions 会先核对 v16 HTML 哈希，再运行 Android Lint 和 `assembleDebug`，最后从 APK 内重新提取 HTML 核对同一哈希。产物名称为 `ForceFocus_v16_原样UI_正式版.apk`。

首次安装后，请在侧栏“设置”中开启 ForceFocus 无障碍服务，并允许精确闹钟；Android 13+ 还需允许通知。
