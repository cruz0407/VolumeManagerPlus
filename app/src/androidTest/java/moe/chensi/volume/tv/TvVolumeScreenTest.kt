package moe.chensi.volume.tv

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvVolumeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dpadChangesOnlySelectedAppAndMenuRestoresVolume() {
        var first by mutableFloatStateOf(.5f)
        var second by mutableFloatStateOf(.8f)
        compose.setContent {
            TvVolumeScreen(
                listOf(TvAppItem("xiaoai", "小爱同学", first), TvAppItem("video", "视频", second)),
                true, "Shizuku 已连接", "测试", { id, volume ->
                    if (id == "xiaoai") first = volume else second = volume
                }, {}, {}, {}, { id, direction ->
                    if (id == "xiaoai") first = TvRemoteControls.adjust(first, direction)
                    else second = TvRemoteControls.adjust(second, direction)
                }
            )
        }
        compose.onNodeWithTag("app-xiaoai").assertIsSelected()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Public test media survives Gradle uninstalling the app at the end of the suite.
        val preview = requireNotNull(context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "tv-remote-preview.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VolumeManagerTV")
            }))
        requireNotNull(context.contentResolver.openOutputStream(preview)).use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionLeft) }
        compose.runOnIdle { assertEquals(.45f, first, .0001f); assertEquals(.8f, second, .0001f) }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("app-video").assertIsSelected()
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(.85f, second, .0001f) }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("menu-0").assertIsSelected()
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(0f, second, 0f) }
        compose.onNodeWithTag("app-video").assertIsSelected()
    }

    @Test fun setupIsOperableWithoutAnyApps() {
        var requests = 0
        var opens = 0
        compose.setContent {
            TvVolumeScreen(emptyList(), false, "未连接", "请授权", { _, _ -> },
                { requests++ }, { opens++ }, {}, { _, _ -> })
        }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun backClosesMenuWithoutLeavingApp() {
        compose.setContent {
            TvVolumeScreen(listOf(TvAppItem("xiaoai", "小爱同学", .5f)), true, "已连接", "",
                { _, _ -> }, {}, {}, {}, { _, _ -> })
        }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("menu-0").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithTag("menu-0").assertDoesNotExist()
        compose.onNodeWithTag("app-xiaoai").assertIsSelected()
    }

    @Test fun burstOfKeysUsesCurrentSelectionAndCurrentVolume() {
        var first by mutableFloatStateOf(.5f)
        var second by mutableFloatStateOf(.5f)
        compose.setContent {
            TvVolumeScreen(listOf(TvAppItem("first", "第一项", first), TvAppItem("second", "第二项", second)),
                true, "已连接", "", { _, _ -> }, {}, {}, {}, { id, direction ->
                    if (id == "first") first = TvRemoteControls.adjust(first, direction)
                    else second = TvRemoteControls.adjust(second, direction)
                })
        }
        compose.onNodeWithTag("tv-root").performKeyInput {
            pressKey(Key.DirectionDown)
            repeat(4) { pressKey(Key.DirectionRight) }
        }
        compose.runOnIdle {
            assertEquals(.5f, first, .0001f)
            assertEquals(.7f, second, .0001f)
        }
    }

    @Test fun menuBackdropBlocksHiddenRows() {
        compose.setContent {
            TvVolumeScreen(listOf(TvAppItem("first", "第一项", .5f)), true, "已连接", "",
                { _, _ -> }, {}, {}, {}, { _, _ -> })
        }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("menu-0").assertIsSelected()
        compose.onNodeWithTag("app-first").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(5f, height / 2f))
        }
        compose.onNodeWithText("请求 Shizuku 授权").assertIsDisplayed()
    }

    @Test fun scrollingKeepsSelectionVisible() {
        compose.setContent {
            TvVolumeScreen((0..39).map { TvAppItem("app$it", "应用 $it", 1f) },
                true, "已连接", "", { _, _ -> }, {}, {}, {}, { _, _ -> })
        }
        repeat(25) {
            compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionDown) }
            compose.waitForIdle()
        }
        compose.onNodeWithTag("app-app25").assertIsDisplayed().assertIsSelected()
    }
}
