package moe.chensi.volume.tv

import org.junit.Assert.*
import org.junit.Test

class TvRemoteControlsTest {
    @Test fun navigationClampsAtBothEnds() {
        assertEquals(0, TvRemoteControls.move(0, -1, 8))
        assertEquals(7, TvRemoteControls.move(7, 1, 8))
        assertEquals(4, TvRemoteControls.move(3, 1, 8))
    }
    @Test fun emptyListHasNoSelection() {
        assertEquals(-1, TvRemoteControls.move(0, 1, 0))
        assertEquals(0, TvRemoteControls.move(-1, 1, 4))
    }
    @Test fun volumeUsesFivePercentagePointSteps() {
        assertEquals(0.45f, TvRemoteControls.adjust(0.5f, -1), 0.0001f)
        assertEquals(0.55f, TvRemoteControls.adjust(0.5f, 1), 0.0001f)
    }
    @Test fun volumeNeverLeavesValidRange() {
        assertEquals(0f, TvRemoteControls.adjust(0.02f, -1), 0f)
        assertEquals(1f, TvRemoteControls.adjust(0.98f, 1), 0f)
        assertEquals(0f, TvRemoteControls.adjust(0f, -1), 0f)
        assertEquals(1f, TvRemoteControls.adjust(1f, 1), 0f)
    }
    @Test fun repeatedPressesReachExactBounds() {
        var value = 0f
        repeat(30) { value = TvRemoteControls.adjust(value, 1) }
        assertEquals(1f, value, 0f)
        repeat(30) { value = TvRemoteControls.adjust(value, -1) }
        assertEquals(0f, value, 0f)
    }
    @Test fun recognizedKeysOnlyAndConfirmDoesNotRepeat() {
        assertEquals(TvRemoteAction.LEFT, TvRemoteControls.action(21, 0, 12))
        assertEquals(TvRemoteAction.CONFIRM, TvRemoteControls.action(23, 0, 0))
        assertEquals(TvRemoteAction.NONE, TvRemoteControls.action(23, 0, 1))
        assertEquals(TvRemoteAction.NONE, TvRemoteControls.action(21, 1, 0))
        assertEquals(TvRemoteAction.NONE, TvRemoteControls.action(24, 0, 0))
        assertEquals(TvRemoteAction.NONE, TvRemoteControls.action(4, 0, 0))
    }
    @Test fun recognizesEnterAndDirectionalKeys() {
        assertEquals(TvRemoteAction.UP, TvRemoteControls.action(19, 0, 0))
        assertEquals(TvRemoteAction.DOWN, TvRemoteControls.action(20, 0, 0))
        assertEquals(TvRemoteAction.RIGHT, TvRemoteControls.action(22, 0, 0))
        assertEquals(TvRemoteAction.CONFIRM, TvRemoteControls.action(66, 0, 0))
        assertEquals(TvRemoteAction.CONFIRM, TvRemoteControls.action(160, 0, 0))
    }
}
