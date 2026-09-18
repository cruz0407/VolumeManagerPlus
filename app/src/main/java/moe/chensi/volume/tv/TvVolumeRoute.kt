package moe.chensi.volume.tv

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.chensi.volume.Manager
import rikka.shizuku.Shizuku

/** Keeps Android privileges and the existing audio engine outside the remote UI. */
@Composable
fun TvVolumeRoute(manager: Manager, enableBackgroundService: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("首次使用请在设置中授权 Shizuku。") }
    var busy by remember { mutableStateOf(false) }
    val connected = manager.shizukuStatus == Manager.ShizukuStatus.Connected

    fun enableService() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { enableBackgroundService() }
                message = "后台服务已启用；重启电视后请检查 Shizuku 是否已运行。"
            } catch (e: Exception) {
                message = "后台服务未启用：${e.message ?: e.javaClass.simpleName}。可在设置中重试。"
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(connected) { if (connected) enableService() }

    val apps = manager.apps.values.sortedWith(
        compareByDescending<moe.chensi.volume.data.App> { it.name.contains("小爱") }
            .thenBy { it.name }.thenBy { it.packageName }
    ).map { app ->
        TvAppItem(app.packageName, app.name, app.volume, app.isPlaying, app.players.isNotEmpty())
    }
    val status = when (manager.shizukuStatus) {
        Manager.ShizukuStatus.Connected -> "Shizuku 已连接"
        Manager.ShizukuStatus.PermissionDenied -> "等待 Shizuku 授权"
        Manager.ShizukuStatus.Disconnected -> "Shizuku 未连接"
        Manager.ShizukuStatus.Uninstalled -> "未安装 Shizuku"
    }

    TvVolumeScreen(
        apps = if (connected) apps else emptyList(),
        connected = connected,
        status = status,
        message = if (busy) "正在启用后台服务…" else message,
        onVolumeChange = { id, value ->
            try {
                if (connected) manager.apps[id]?.volume = value
            } catch (e: Exception) {
                message = "调整失败：${e.message ?: e.javaClass.simpleName}"
            }
        },
        onRequestPermission = {
            try {
                if (Shizuku.pingBinder()) {
                    if (connected) message = "Shizuku 已授权，无需重复授权。"
                    else Shizuku.requestPermission(0)
                } else message = "请先打开 Shizuku 并启动服务，然后返回本应用。"
            } catch (e: Exception) {
                message = "请求授权失败：${e.message ?: e.javaClass.simpleName}"
            }
        },
        onOpenShizuku = {
            try {
                val pm = context.packageManager
                val intent = pm.getLeanbackLaunchIntentForPackage(Manager.SHIZUKU_PACKAGE_NAME)
                    ?: pm.getLaunchIntentForPackage(Manager.SHIZUKU_PACKAGE_NAME)
                if (intent != null) context.startActivity(intent)
                else message = "找不到 Shizuku 启动入口，请从电视应用列表打开或先安装它。"
            } catch (e: Exception) {
                message = "无法打开 Shizuku：${e.message ?: e.javaClass.simpleName}"
            }
        },
        onEnableBackground = {
            if (connected) enableService() else message = "请先启动并授权 Shizuku。"
        }
    )
}
