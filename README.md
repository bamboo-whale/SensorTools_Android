# SensorTools

SensorTools 是一款基于 Jetpack Compose 的 Android 传感器工具应用，用于查看、监听、校准、健康检测和数据录制导出。

## 功能特性

- 首页按分类展示传感器，支持折叠/展开
- 传感器详情页支持实时监听、暂停、重置与统计分析
- 校准页提供传感器校准状态反馈
- 健康页提供扫描进度、结果汇总与异常提示
- 数据页支持录制、监听预览、CSV/JSON 导出、分享和打开保存位置
- 工具页提供水平仪、指南针等实用功能

## 项目架构

- `app/src/main/java/com/sensortools/data`
  - 数据模型、仓库、后台录制服务、偏好设置
- `app/src/main/java/com/sensortools/domain`
  - 传感器分类、健康分析、运动识别等业务逻辑
- `app/src/main/java/com/sensortools/ui`
  - Compose 页面、ViewModel、导航和通用组件
- `app/src/main/java/com/sensortools/util`
  - 导出、运动检测、文件分享等工具类
- `app/src/main/res`
  - 主题、图标、字符串和其他资源文件

## 代码结构说明

- `ui/home`
  - 首页传感器总览、分类分组与详情入口
- `ui/detail`
  - 传感器详情页，展示实时数据与统计信息
- `ui/calibration`
  - 校准流程与状态反馈
- `ui/health`
  - 健康扫描与结果展示
- `ui/record`
  - 数据录制、监听预览、导出和分享
- `ui/components`
  - 图表、仪表盘、传感器卡片等复用组件
- `ui/navigation`
  - 页面路由与底部导航

## 构建与安装

### 编译调试包

```bash
./gradlew assembleDebug
```

### 安装到已连接手机

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 开发说明

- 运行环境：Android 8.0 及以上
- 依赖真实传感器的功能建议在真机上测试
- 部分设备可能不支持某些传感器，界面会显示不可用或无数据状态

## 最近更新

- 首页传感器分类默认折叠，并增加图标
- 数据页与首页统一为分类卡片风格
- 数据页支持监听预览、导出后打开保存位置
- 修复分享导出闪退问题
- 详情页增加均值统计
- 水平仪和指南针优化了观感与采样反馈

## 截图

建议将应用截图放到仓库根目录的 `docs/` 目录中，再在此处补充链接。
