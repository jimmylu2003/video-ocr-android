# 🦞 视频识中文 - Android APP

基于 Google ML Kit 的中文 OCR 识别应用，支持摄像头实时识别和图片文件识别。

## 功能特性

✅ **摄像头实时识别** - 打开摄像头，实时识别画面中的中文
✅ **图片文件识别** - 从相册选择图片进行识别
✅ **结果复制** - 一键复制识别结果到剪贴板
✅ **结果保存** - 保存识别结果到本地文件
✅ **中文优化** - 使用 Google ML Kit 中文识别模型

## 技术栈

- **语言**: Kotlin
- **最低版本**: Android 7.0 (API 24)
- **目标版本**: Android 14 (API 34)
- **摄像头**: CameraX
- **OCR**: Google ML Kit Text Recognition (中文)

## 构建方法

### 方法 1: Android Studio (推荐)

1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio，选择 `Open an existing project`
3. 选择 `video-ocr-android` 文件夹
4. 等待 Gradle 同步完成
5. 点击 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
6. APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`

### 方法 2: 命令行

```bash
cd video-ocr-android

# Linux/Mac
chmod +x gradlew
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

APK 位置：`app/build/outputs/apk/debug/app-debug.apk`

## 安装

1. 将 APK 文件传输到 Android 手机
2. 在手机上打开 APK 文件
3. 如果提示"未知来源"，请在设置中允许安装
4. 完成安装

## 使用说明

1. **首次启动** - 授予摄像头权限
2. **摄像头模式** - 点击"启动摄像头"，对准要识别的文字
3. **图片模式** - 点击"选择图片"，从相册选择图片
4. **查看结果** - 识别结果自动显示在下方
5. **复制/保存** - 点击对应按钮操作

## 项目结构

```
video-ocr-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/jimmylu/videoocr/
│   │   │   └── MainActivity.kt      # 主界面 + OCR 逻辑
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml # UI 布局
│   │   │   ├── values/
│   │   │   │   ├── strings.xml       # 字符串资源
│   │   │   │   ├── colors.xml        # 颜色定义
│   │   │   │   └── themes.xml        # 主题样式
│   │   │   └── drawable/
│   │   │       └── rounded_bg.xml    # 圆角背景
│   │   └── AndroidManifest.xml       # 应用配置
│   └── build.gradle.kts              # 应用构建配置
├── build.gradle.kts                  # 项目构建配置
└── settings.gradle.kts               # 项目设置
```

## 依赖说明

- **CameraX**: Google 官方摄像头库，提供稳定的摄像头 API
- **ML Kit Text Recognition**: Google 机器学习工具包，支持中文识别
- **Material Components**: Material Design UI 组件

## 权限说明

- `CAMERA`: 摄像头访问（必需）
- `READ_EXTERNAL_STORAGE`: 读取图片文件（Android 12 及以下）
- `READ_MEDIA_IMAGES`: 读取图片（Android 13+）

## 常见问题

**Q: 识别不准确怎么办？**
A: 确保光线充足，文字清晰，尽量保持手机稳定。

**Q: 摄像头打不开？**
A: 检查是否授予了摄像头权限，或在设置中重新授权。

**Q: 识别速度慢？**
A: 首次使用需要下载 ML Kit 模型，后续会使用缓存。

## 开发者

Jimmy Lu - https://github.com/jimmylu2003

## 许可证

MIT License
