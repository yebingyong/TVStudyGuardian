# 电视学习管家 V1.3（Android TV 原型）

这是面向 Android TV / 华为智慧屏的学习答题拦截原型源码。

## 在线构建 APK
1. 打开仓库的 **Actions** 页面。
2. 选择 **Build Android APK**。
3. 点击 **Run workflow**，再点击绿色按钮确认。
4. 等待任务完成，进入该次运行页面底部的 **Artifacts**，下载 `TVStudyGuardian-debug`。

推送到 main 分支的提交也会自动触发构建。

## 项目配置
- Android Gradle Plugin 8.2.2
- Gradle 8.2
- JDK 17
- compileSdk 34 / minSdk 23 / targetSdk 28
- armeabi-v7a、arm64-v8a

## 当前题库说明（重要）
仓库目前只放入了空题库占位文件 `app/src/main/assets/question_bank.json`，因此程序会使用代码内置的少量随机兜底题。原 V1.3 ZIP 中的 3,486 道题尚未上传到此仓库；这不影响 APK 编译，但题目数量和覆盖范围会明显少于原 ZIP 版本。请勿将当前仓库描述为已包含完整离线题库。

## 使用和兼容性限制
安装后需在电视系统设置中手动开启本应用的无障碍服务。无障碍覆盖层不能保证拦截 Home、系统安全窗口、所有厂商视频窗口，也不能阻止强制停止或卸载。华为 Smart Screen SE43 MEM（HarmonyOS 2）尚未实机验证；是否可安装和无障碍服务是否可用，需要实际测试。

当前 Actions 生成的是 debug APK，仅供测试安装，不代表已通过华为电视兼容性验证。
