import moe.chensi.volume.tv.TvRemoteControls;
import moe.chensi.volume.tv.TvRemoteAction;

/** Runs the same pure remote contract without an Android SDK or JUnit. */
public class RemoteContractTest {
    static int checks;
    static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(TvRemoteControls.move(0, -1, 8) == 0, "first row");
        check(TvRemoteControls.move(7, 1, 8) == 7, "last row");
        check(TvRemoteControls.move(3, 1, 8) == 4, "next row");
        check(TvRemoteControls.move(0, 1, 0) == -1, "empty list");
        check(TvRemoteControls.move(-1, 1, 4) == 0, "new list");
        check(Math.abs(TvRemoteControls.adjust(.5f, -1) - .45f) < .0001f, "5 percent down");
        check(Math.abs(TvRemoteControls.adjust(.5f, 1) - .55f) < .0001f, "5 percent up");
        check(TvRemoteControls.adjust(.02f, -1) == 0f, "lower clamp");
        check(TvRemoteControls.adjust(.98f, 1) == 1f, "upper clamp");
        float value = 0f;
        for (int i=0; i<30; i++) value = TvRemoteControls.adjust(value, 1);
        check(value == 1f, "held right");
        for (int i=0; i<30; i++) value = TvRemoteControls.adjust(value, -1);
        check(value == 0f, "held left");
        check(TvRemoteControls.action(21, 0, 12) == TvRemoteAction.LEFT, "repeat left");
        check(TvRemoteControls.action(23, 0, 0) == TvRemoteAction.CONFIRM, "confirm");
        check(TvRemoteControls.action(23, 0, 1) == TvRemoteAction.NONE, "no repeat confirm");
        check(TvRemoteControls.action(21, 1, 0) == TvRemoteAction.NONE, "ignore key up");
        check(TvRemoteControls.action(24, 0, 0) == TvRemoteAction.NONE, "system volume unchanged");
        check(TvRemoteControls.action(4, 0, 0) == TvRemoteAction.NONE, "back handled separately");
        check(TvRemoteControls.action(19, 0, 0) == TvRemoteAction.UP, "up");
        check(TvRemoteControls.action(20, 0, 0) == TvRemoteAction.DOWN, "down");
        check(TvRemoteControls.action(22, 0, 0) == TvRemoteAction.RIGHT, "right");
        check(TvRemoteControls.action(66, 0, 0) == TvRemoteAction.CONFIRM, "enter");
        check(TvRemoteControls.action(160, 0, 0) == TvRemoteAction.CONFIRM, "numpad enter");
        System.out.println("PASS: " + checks + " remote contract assertions");
    }
}
