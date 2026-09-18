package moe.chensi.volume.tv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/** Plain values make the full remote UI testable without Shizuku or system services. */
data class TvAppItem(
    val id: String,
    val name: String,
    val volume: Float,
    val playing: Boolean = false,
    val hasPlayer: Boolean = false
)

private val Background = Color(0xFF10171E)
private val Panel = Color(0xFF1D2933)
private val Accent = Color(0xFF8CE3CC)
private val Muted = Color(0xFFACBAC6)
private val Foreground = Color(0xFFF2F6F8)

/**
 * One real keyboard focus owner, explicit selected rows. No invisible focus targets can
 * steal left/right from the volume control. Back stays with Android's BackHandler.
 */
@Composable
fun TvVolumeScreen(
    apps: List<TvAppItem>,
    connected: Boolean,
    status: String,
    message: String,
    onVolumeChange: (String, Float) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit,
    onEnableBackground: () -> Unit,
    onVolumeAdjust: (String, Int) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var initiallySelected by rememberSaveable { mutableStateOf(false) }
    var menu by rememberSaveable { mutableStateOf<String?>(null) }
    var menuIndex by rememberSaveable { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val settings = menu == "settings"
    val menuApp = apps.firstOrNull { it.id == menu }
    val options = if (settings) listOf("请求 Shizuku 授权", "打开 Shizuku", "重新启用后台服务", "返回应用列表")
        else listOf("静音 · 0%", "设为 · 30%", "恢复 · 100%", "返回应用列表")

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(apps.map { it.id }) {
        if (!initiallySelected && apps.isNotEmpty()) {
            selectedId = apps.first().id
            initiallySelected = true
        } else if (selectedId != null && apps.none { it.id == selectedId }) {
            selectedId = apps.firstOrNull()?.id
        }
        if (menu != null && menu != "settings" && apps.none { it.id == menu }) menu = null
    }
    LaunchedEffect(selectedId, apps.map { it.id }) {
        val index = apps.indexOfFirst { it.id == selectedId }
        if (index >= 0) listState.scrollToItem(index)
    }
    BackHandler(enabled = menu != null) { menu = null }

    fun openMenu(id: String) { menuIndex = 0; menu = id }
    fun activateOption(index: Int) {
        if (menu == "settings") {
            when (index) {
                0 -> onRequestPermission()
                1 -> onOpenShizuku()
                2 -> onEnableBackground()
                else -> menu = null
            }
        } else {
            val currentApp = apps.firstOrNull { it.id == menu }
            if (index < 3 && currentApp != null) {
                onVolumeChange(currentApp.id, listOf(0f, .3f, 1f)[index])
            }
            menu = null
        }
    }
    fun handle(action: TvRemoteAction) {
        if (menu != null) {
            when (action) {
                TvRemoteAction.UP -> menuIndex = TvRemoteControls.move(menuIndex, -1, options.size)
                TvRemoteAction.DOWN -> menuIndex = TvRemoteControls.move(menuIndex, 1, options.size)
                TvRemoteAction.CONFIRM -> activateOption(menuIndex)
                else -> Unit
            }
            return
        }
        val selectedApp = apps.firstOrNull { it.id == selectedId }
        when (action) {
            TvRemoteAction.UP, TvRemoteAction.DOWN -> {
                // Position 0 is the setup header; the app list starts at position 1.
                val current = apps.indexOfFirst { it.id == selectedId } + 1
                val next = TvRemoteControls.move(current, if (action == TvRemoteAction.UP) -1 else 1, apps.size + 1)
                selectedId = apps.getOrNull(next - 1)?.id
            }
            TvRemoteAction.LEFT, TvRemoteAction.RIGHT -> {
                if (connected && selectedApp != null) {
                    onVolumeAdjust(selectedApp.id, if (action == TvRemoteAction.LEFT) -1 else 1)
                }
            }
            TvRemoteAction.CONFIRM -> openMenu(selectedApp?.id ?: "settings")
            else -> Unit
        }
    }

    Box(
        Modifier.fillMaxSize().background(Background)
            .testTag("tv-root")
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                val handled = native.keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
                if (handled) handle(TvRemoteControls.action(native.keyCode, native.action, native.repeatCount))
                handled
            }.focusable()
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("应用音量 · TV", color = Foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("单独调整应用，不改变电视总音量", color = Muted, fontSize = 15.sp)
                }
                Text(status, color = if (connected) Accent else Color(0xFFFFCA86), fontSize = 16.sp)
            }
            RemotePanel(
                selected = selectedId == null && menu == null,
                modifier = Modifier.fillMaxWidth().testTag("tv-settings"),
                onTap = { selectedId = null; openMenu("settings") }
            ) {
                Text("设置与权限", color = Foreground, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text("选中后按确认键打开", color = Muted, fontSize = 14.sp)
            }
            if (apps.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Text(if (connected) "正在读取应用列表…" else "先连接 Shizuku，再调整应用音量", color = Foreground, fontSize = 24.sp)
                    Text("按确认键打开设置。Shizuku 的系统授权窗口由它自身提供。", color = Muted, fontSize = 16.sp)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).testTag("tv-app-list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                    itemsIndexed(apps, key = { _, app -> app.id }) { _, app ->
                        RemotePanel(
                            selected = selectedId == app.id && menu == null,
                            modifier = Modifier.fillMaxWidth().testTag("app-${app.id}"),
                            onTap = { selectedId = app.id; openMenu(app.id) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(app.name, color = Foreground, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(app.id, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(if (app.playing) "正在播放" else if (app.hasPlayer) "已识别播放器" else "等待下次播放",
                                    color = if (app.playing) Accent else Muted, fontSize = 13.sp)
                                Text("${(app.volume * 100).roundToInt()}%", color = Accent, fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { app.volume.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp), color = Accent, trackColor = Background)
                        }
                    }
                }
            }
            Text(message, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("↑↓ 选择应用     ←→ 调整 5%     确认 更多操作     返回 关闭菜单", color = Foreground, fontSize = 15.sp)
        }
        if (menu != null) {
            Box(Modifier.fillMaxSize().background(Background.copy(alpha = .97f))
                .pointerInput(Unit) { detectTapGestures(onTap = { /* Block clicks behind this modal. */ }) },
                contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 620.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (settings) "设置与权限" else menuApp?.name.orEmpty(), color = Foreground,
                        fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    if (settings) Text("$status\n$message", color = Muted, fontSize = 15.sp, maxLines = 4)
                    options.forEachIndexed { index, label ->
                        RemotePanel(selected = menuIndex == index, modifier = Modifier.fillMaxWidth().testTag("menu-$index"),
                            onTap = { menuIndex = index; activateOption(index) }) {
                            Text(label, color = Foreground, fontSize = 20.sp)
                        }
                    }
                    Text("↑↓ 选择     确认 执行     返回 关闭", color = Muted, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun RemotePanel(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.semantics { this.selected = selected }
        .background(if (selected) Color(0xFF253F42) else Panel, shape)
        .border(3.dp, if (selected) Accent else Color.Transparent, shape)
        .pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) }
        .padding(horizontal = 20.dp, vertical = 12.dp), content = content)
}
