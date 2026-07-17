# 视频速览 · VideoFlow

一款把手机里保存的视频，按 **时长 / 大小** 自动分类，并用 **抖音式滑动** 观看的安卓 App。

## 交互方式

| 手势 | 作用 |
| --- | --- |
| 👆 向上滑 | 切换到下一个视频 |
| 👇 向下滑 | 回到上一个视频 |
| 👉 向右滑 | 删除当前视频（会弹出系统删除确认框，安全不误删） |
| 👈 向左滑 | 收藏 / 取消收藏当前视频 |

顶部可切换分类：**全部 / 按时长 / 按大小 / 收藏**。
- 按时长：1 分钟内、1–5 分钟、5–30 分钟、30 分钟以上
- 按大小：10MB 内、10–50MB、50–200MB、200MB 以上

---

## 如何拿到可安装的 APK（无需装任何开发工具）

整个过程用你的电脑浏览器就能完成，大约 5 分钟。

### 第 1 步：注册 / 登录 GitHub
打开 https://github.com ，注册一个免费账号并登录。

### 第 2 步：新建一个仓库
点右上角 “+” → **New repository**：
- Repository name 随便填，例如 `video-flow`
- 选 **Private**（私有）即可
- 点 **Create repository**

### 第 3 步：上传本项目文件
在新建好的仓库页面，点 **uploading an existing file**（或 Add file → Upload files），
把本文件夹（video-flow）里的 **所有内容** 拖进去上传，然后点 **Commit changes**。

> 注意：要把文件夹里的文件都传上去，包括隐藏的 `.github` 目录（里面是自动打包脚本）。
> 如果拖拽看不到 `.github`，可以直接把整个 video-flow 文件夹压缩后用 GitHub Desktop 上传，或让我帮你打包。

### 第 4 步：等待云端自动编译
上传完成后，GitHub 会自动开始编译。点仓库顶部的 **Actions** 标签，
能看到一个名为“构建 APK”的任务正在运行（转圈）。约 3–5 分钟后变成绿色 ✅。

### 第 5 步：下载 APK
点进那次绿色的运行记录，在页面底部 **Artifacts** 区域，
下载 **视频速览-APK**（是个 zip，解压后得到 `app-debug.apk`）。

### 第 6 步：安装到手机
把 `app-debug.apk` 传到安卓手机上打开安装。
首次安装需允许“安装未知来源应用”，按手机提示开启即可。
打开 App → 授予“读取视频”权限 → 开始刷你的视频库。

---

## 常见问题

**Q：安装时提示“应用未安装”或“解析包错误”？**
A：确认下载的是解压后的 `.apk` 文件而不是 zip 本身；并确认手机是 Android 10 及以上。

**Q：打开后没有视频？**
A：确认已授予读取视频权限；App 读取的是系统媒体库里的视频（相册/下载等目录里的视频都会被扫描到）。

**Q：删除会不会误删？**
A：右滑删除会弹出安卓系统自带的删除确认框，需你再确认一次才真正删除，不会一滑就没。

**Q：Actions 编译红色失败了？**
A：点进日志把报错发给我，我来帮你修。

---

## 技术信息（给懂技术的你）
- 语言：Kotlin + Jetpack Compose
- 播放：Media3 ExoPlayer
- 视频来源：MediaStore（`READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE`）
- 删除：`MediaStore.createDeleteRequest`（Android 11+）/ `RecoverableSecurityException`（Android 10）
- minSdk 29，targetSdk 34
- 如本地有 Android Studio，直接打开本目录即可运行（Run app）。

---

## 关于视频格式支持（已集成 FFmpeg 软解）

App 已内置 **Media3 FFmpeg 软解扩展**（`media3-decoder-ffmpeg`），默认开启
`EXTENSION_RENDERER_MODE_ON`：能走硬件解码的格式（绝大多数）仍走硬解，省电流畅；
只有当手机自带解码器不支持时，才自动回退到 FFmpeg 软解。

**因此相比“纯硬解”，现在能多放很多冷门视频**，例如：

- 各种奇葩编码的 **MKV / AVI / FLV / TS**（如 MPEG-4、VP9、AV1、DivX/Xvid 封装等）
- 带 **AC3 / E-AC3 / DTS / Opus / Vorbis** 等音频流的视频
- 老设备对 H.265(HEVC) 支持不全时，也能用软解顶上

**仍大概率放不了的格式（重要）：**
- **RMVB（RealMedia）**：官方预编译的 FFmpeg 扩展 AAR **不包含** RealVideo 解码器，所以 RMVB 基本还是刷不动。
- **WMV（VC-1 / WMV1/2/3）**：官方预编译扩展通常也未包含对应解码器，多数放不了。

> 如果你手里确实有 RMVB / WMV 视频必须播放，那需要**自己用 NDK 交叉编译带 RealMedia/WMV 解码的 FFmpeg**，
> 工程复杂度高很多，且涉及专利问题。如有需要可以单独找我评估。

**结论**：日常主流格式（MP4、MKV、MOV、WebM、3GP、TS 及各种常见编码）现在都能放心刷；
只有 RMVB / WMV 这类老格式可能仍不支持。
