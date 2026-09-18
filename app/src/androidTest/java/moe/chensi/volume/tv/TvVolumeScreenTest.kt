package moe.chensi.volume.tv

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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
                }, {}, {}, {}
            )
        }
        compose.onNodeWithTag("app-xiaoai").assertIsSelected()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        FileOutputStream(File(context.getExternalFilesDir(null), "tv-remote-preview.png")).use {
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
                { requests++ }, { opens++ }, {})
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
                { _, _ -> }, {}, {}, {})
        }
        compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("menu-0").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithTag("menu-0").assertDoesNotExist()
        compose.onNodeWithTag("app-xiaoai").assertIsSelected()
    }

    @Test fun scrollingKeepsSelectionVisible() {
        compose.setContent {
            TvVolumeScreen((0..39).map { TvAppItem("app$it", "应用 $it", 1f) },
                true, "已连接", "", { _, _ -> }, {}, {}, {})
        }
        repeat(25) {
            compose.onNodeWithTag("tv-root").performKeyInput { pressKey(Key.DirectionDown) }
            compose.waitForIdle()
        }
        compose.onNodeWithTag("app-app25").assertIsDisplayed().assertIsSelected()
    }
}

