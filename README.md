# SensorTools / SensorTools_Android

SensorTools is a Compose-based Android app for inspecting, monitoring, calibrating, and testing device sensors.

SensorTools 是一款基于 Jetpack Compose 的 Android 传感器工具应用，用于查看、监测、校准和测试设备传感器。

It focuses on four things:
- sensor discovery and categorization
- real-time sensor data preview
- calibration workflows with explicit status feedback
- health checking and result reporting

它主要关注四件事：
- 传感器发现与分类
- 实时传感器数据预览
- 带明确状态反馈的校准流程
- 健康检测与结果汇报

## Highlights | 亮点

- Friendly sensor categories on the home screen
- Detail pages that show concrete sensor model/vendor information
- Health screen with dashboard-style layout and scan status
- Calibration flows for accelerometer, gyroscope, and magnetometer
- Material 3 dark UI with clear status colors

- 首页使用友好的传感器分类
- 详情页展示具体型号、厂商和技术参数
- 健康页采用仪表盘式布局，并显示扫描状态
- 支持加速度计、陀螺仪、磁力计校准
- 使用 Material 3 深色主题，并用状态颜色强化反馈

## Main Screens | 主要界面

- Home: grouped sensor list with collapse/expand sections
- Detail: real-time data and technical sensor details
- Calibration: calibration cards with progress and result states
- Health: sensor health scan with summary and detail results
- Record: sensor data recording and export-related flows
- Settings: app preferences and behavior tuning
- About: application information

- 首页：按类别分组的传感器列表，支持折叠/展开
- 详情页：实时数据和传感器技术信息
- 校准页：带进度和结果状态的校准卡片
- 健康页：传感器健康扫描，提供汇总和详细结果
- 记录页：传感器数据记录与导出相关功能
- 设置页：应用偏好与行为配置
- 关于页：应用信息展示

## Tech Stack | 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel + StateFlow
- Navigation Compose
- SensorManager APIs
- DataStore Preferences
- Coroutines

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel + StateFlow
- Navigation Compose
- SensorManager API
- DataStore Preferences
- 协程

## Requirements | 环境要求

- Android 8.0+ (`minSdk 26`)
- A physical Android device or emulator with sensor support

- Android 8.0 及以上（`minSdk 26`）
- 支持传感器的真机或模拟器

## Build and Run | 构建与运行

```bash
./gradlew assembleDebug
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

构建调试版 APK：

```bash
./gradlew assembleDebug
```

安装调试版 APK：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure | 项目结构

- `app/src/main/java/com/sensortools/data`: models, repositories, local storage, and service layer
- `app/src/main/java/com/sensortools/domain`: sensor analysis and classification rules
- `app/src/main/java/com/sensortools/ui`: Compose screens, view models, navigation, and shared UI components
- `app/src/main/java/com/sensortools/util`: helper utilities such as motion detection and export logic
- `app/src/main/res`: UI resources, icons, and theme assets

- `app/src/main/java/com/sensortools/data`：数据模型、仓库、本地存储和服务层
- `app/src/main/java/com/sensortools/domain`：传感器分析与分类规则
- `app/src/main/java/com/sensortools/ui`：Compose 界面、ViewModel、导航与通用 UI 组件
- `app/src/main/java/com/sensortools/util`：运动检测、导出等辅助工具
- `app/src/main/res`：资源文件、图标与主题资产

## Notes | 说明

- The app is optimized for real-device sensor testing.
- Some sensors may be unavailable on certain phones. The UI shows explicit unavailable or no-data states instead of failing silently.
- The health and calibration pages were designed to surface progress and error reasons clearly.

- 本应用更适合真机传感器测试。
- 某些手机可能不支持部分传感器，界面会明确显示不可用或无数据状态，不会静默失败。
- 健康页和校准页都强调进度反馈与错误原因展示。

## Screenshots | 截图

Add screenshots in the repository root or a `docs/` folder and link them here for the GitHub landing page.

建议将截图放在仓库根目录或 `docs/` 目录中，再在这里添加链接，便于 GitHub 首页展示。
