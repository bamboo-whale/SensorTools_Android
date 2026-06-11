# SensorTools Developer Guide | 开发者说明

This document is for team members and for project presentation to instructors.

本文档面向小组开发成员，也适合作为老师汇报时的技术说明材料。

## 1. Project Overview | 项目概述

SensorTools is an Android sensor toolkit built with Jetpack Compose. It provides:
- sensor browsing and grouping
- live sensor monitoring
- calibration for multiple sensors
- health scans with categorized results
- recording/export support

SensorTools 是一个基于 Jetpack Compose 的 Android 传感器工具应用，提供：
- 传感器浏览与分类
- 实时传感器监测
- 多种传感器校准
- 带分类结果的健康检测
- 数据记录与导出支持

The current UX goal is:
- outer layer shows friendly names and categories
- detail layer shows technical names, vendor, and device parameters
- every long-running operation must have visible progress and error feedback

当前的交互目标是：
- 外层展示友好名称与分类
- 详情层展示技术名、厂商和设备参数
- 所有耗时操作都必须有明确进度和错误反馈

## 2. Architecture | 架构

The app follows a layered structure:

- Presentation layer
  - Compose screens
  - ViewModels
  - UI state collected through `StateFlow`

- Domain layer
  - sensor classification rules
  - health analysis logic
  - motion detection helpers

- Data layer
  - `SensorRepository` for sensor discovery and live reading
  - `CalibrationRepository` for calibration sampling and persistence
  - `PreferencesManager` for local settings and saved calibration data
  - `SensorRecordingService` for recording-related background work

The design keeps UI code thin and moves sensor access into repositories.

项目采用分层结构：

- 表现层
  - Compose 界面
  - ViewModel
  - 通过 `StateFlow` 收集 UI 状态

- 领域层
  - 传感器分类规则
  - 健康分析逻辑
  - 运动检测辅助工具

- 数据层
  - `SensorRepository`：负责传感器发现和实时读取
  - `CalibrationRepository`：负责校准采样与持久化
  - `PreferencesManager`：负责本地设置与保存的校准数据
  - `SensorRecordingService`：负责录制相关的后台工作

这种设计让 UI 层保持轻量，把传感器访问集中到仓库层。

## 3. Code Structure | 代码结构

### App entry | 应用入口

- `app/src/main/java/com/sensortools/MainActivity.kt`
  - app host activity
  - sets up the Compose root

- `app/src/main/java/com/sensortools/ui/navigation/NavGraph.kt`
  - central navigation routes
  - screen switching and shared navigation arguments

- `app/src/main/java/com/sensortools/MainActivity.kt`
  - 应用宿主 Activity
  - 初始化 Compose 根节点

- `app/src/main/java/com/sensortools/ui/navigation/NavGraph.kt`
  - 集中管理导航路由
  - 负责页面切换与参数传递

### Presentation | 表现层

- `ui/home`
  - `HomeScreen.kt`
  - `HomeViewModel.kt`
  - sensor list grouping by category

- `ui/detail`
  - `DetailScreen.kt`
  - `DetailViewModel.kt`
  - real-time sensor data visualization

- `ui/calibration`
  - `CalibrationScreen.kt`
  - `CalibrationViewModel.kt`
  - calibration states: idle, running, completed, failed/unavailable

- `ui/health`
  - `HealthScreen.kt`
  - `HealthViewModel.kt`
  - health scan dashboard and categorized results

- `ui/record`
  - recording UI and start/stop logic

- `ui/settings`
  - app preferences and behavior controls

- `ui/about`
  - project/app info

- `ui/components`
  - shared cards, status badges, charts, and sensor icon helpers

- `ui/home`
  - `HomeScreen.kt`
  - `HomeViewModel.kt`
  - 按类别对传感器列表分组

- `ui/detail`
  - `DetailScreen.kt`
  - `DetailViewModel.kt`
  - 实时传感器数据展示

- `ui/calibration`
  - `CalibrationScreen.kt`
  - `CalibrationViewModel.kt`
  - 校准状态：未开始、进行中、已完成、失败/不可用

- `ui/health`
  - `HealthScreen.kt`
  - `HealthViewModel.kt`
  - 健康检测仪表盘与分类结果

- `ui/record`
  - 录制界面与开始/停止逻辑

- `ui/settings`
  - 应用偏好与行为控制

- `ui/about`
  - 项目与应用信息

- `ui/components`
  - 通用卡片、状态标签、图表和传感器图标工具

### Data | 数据层

- `data/model`
  - `SensorInfo.kt`
  - `SensorData.kt`
  - `CalibrationData.kt`

- `data/repository`
  - `SensorRepository.kt`
  - `CalibrationRepository.kt`

- `data/local`
  - `PreferencesManager.kt`

- `data/service`
  - `SensorRecordingService.kt`

- `data/model`
  - `SensorInfo.kt`
  - `SensorData.kt`
  - `CalibrationData.kt`

- `data/repository`
  - `SensorRepository.kt`
  - `CalibrationRepository.kt`

- `data/local`
  - `PreferencesManager.kt`

- `data/service`
  - `SensorRecordingService.kt`

### Domain and utils | 领域层与工具层

- `domain/HealthAnalyzer.kt`
  - health status evaluation

- `domain/SensorClassifier.kt`
  - sensor-specific classification rules

- `util/MotionDetector.kt`
  - motion state detection

- `util/ExportManager.kt`
  - file export support

- `domain/HealthAnalyzer.kt`
  - 健康状态评估

- `domain/SensorClassifier.kt`
  - 传感器分类规则

- `util/MotionDetector.kt`
  - 运动状态检测

- `util/ExportManager.kt`
  - 文件导出支持

## 4. UI / UX Rules | 界面规范

Current interface rules used in the latest changes:
- Home page uses friendly category names like `运动与姿态` and `环境感知`
- Category cards are collapsible to reduce visual noise
- Each category has an icon to improve scanability
- Detail page emphasizes the concrete model name and vendor information
- Health page uses dashboard-style cards, not a single-button layout
- Calibration page must not appear frozen when the result is incomplete

最近修改中遵循的界面规范：
- 首页使用 `运动与姿态`、`环境感知` 等友好分类名
- 分类卡片支持折叠，降低视觉噪音
- 每个分类前添加图标，增强扫读性
- 详情页突出具体型号和厂商信息
- 健康页采用仪表盘式卡片，而不是单按钮页面
- 校准页在结果未完成时不能表现得像“卡死”

## 5. State Management Pattern | 状态管理模式

Most screens follow this pattern:
1. ViewModel loads or subscribes to sensor data.
2. UI collects `StateFlow`.
3. Compose recomposes when state changes.
4. Errors, timeout, and unavailable states are surfaced in the UI.

Why this matters:
- easier to debug
- clearer demo behavior
- safer than passing sensor state directly through UI callbacks

多数页面遵循以下模式：
1. ViewModel 负责加载或订阅传感器数据。
2. UI 层收集 `StateFlow`。
3. 状态变化后 Compose 自动重组。
4. 错误、超时、不可用状态都显式展示在界面上。

这样做的好处：
- 更容易调试
- 演示效果更清晰
- 比直接在 UI 里处理传感器状态更安全

## 6. Sensor Flow Summary | 传感器流程说明

### Home | 首页
- load all sensors from repository
- group them by category
- show friendly names first
- navigate to details on tap

- 从仓库加载全部传感器
- 按类别分组
- 优先展示友好名称
- 点击进入详情

### Detail | 详情页
- initialize a specific sensor
- start live sampling
- display data, stats, and chart

- 初始化指定传感器
- 开始实时采样
- 展示数据、统计和曲线

### Calibration | 校准页
- start calibration for accelerometer or gyroscope
- observe magnetometer calibration
- show progress immediately
- show completion or failure reason

- 启动加速度计或陀螺仪校准
- 观察磁力计校准过程
- 立即显示进度
- 显示完成状态或失败原因

### Health | 健康页
- run health scan over available sensors
- preserve scan results
- display normal/abnormal/suspect/no-data categories

- 对可用传感器执行健康扫描
- 保留扫描结果
- 显示正常/异常/可疑/无数据分类

## 7. Local Testing | 本地测试

Recommended commands:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat
```

推荐命令：

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat
```

Manual verification checklist:
- app opens normally
- home screen categories expand/collapse correctly
- detail page shows the correct sensor model information
- calibration buttons change state immediately
- health page opens without crashing
- missing sensors show visible feedback instead of silent failure

手动验证清单：
- 应用可正常打开
- 首页分类可正确折叠/展开
- 详情页能显示正确的传感器型号信息
- 校准按钮点击后能立刻反馈状态
- 健康页打开不会崩溃
- 缺失传感器会显示可见反馈，而不是静默失败

## 8. Presentation Talking Points | 答辩汇报要点

When reporting to the teacher, you can describe the work like this:

1. We separated the app into data, domain, and UI layers.
2. We improved usability by replacing raw sensor names with friendly categories on the home screen.
3. We kept technical detail available in the detail page for advanced inspection.
4. We fixed blocking bugs in health and calibration flows by adding explicit state handling.
5. We redesigned the health page into a dashboard-style interface with denser information.
6. We made calibration states visible so incomplete work is not mistaken for a freeze.

向老师汇报时可以这样描述：

1. 我们把应用拆成了数据层、领域层和 UI 层。
2. 首页用友好分类替代原始传感器名称，提升了可读性。
3. 详情页保留技术细节，方便进一步分析。
4. 通过增加显式状态处理，修复了健康页和校准页的关键阻塞问题。
5. 将健康页重构为仪表盘式页面，信息更密集、更易扫描。
6. 校准状态可视化后，未完成不再像“卡死”。

## 9. Team Notes | 小组协作说明

- Keep UI text consistent with the friendly-name-first rule.
- Add new sensor categories in both the home grouping logic and the detail icon/title mapping.
- If repository behavior changes, update the corresponding ViewModel state messages so the UI stays informative.
- Avoid silent failures in any sensor-related flow. Prefer explicit `NO_DATA`, `UNAVAILABLE`, or `FAILED` states.

- UI 文案保持“友好名称优先”的统一规则。
- 新增传感器分类时，同时更新首页分组逻辑和详情页图标/标题映射。
- 如果仓库层行为变化，要同步更新对应 ViewModel 的状态提示，保证 UI 仍然清晰。
- 任何传感器相关流程都不要静默失败，优先使用 `NO_DATA`、`UNAVAILABLE` 或 `FAILED` 这类明确状态。
