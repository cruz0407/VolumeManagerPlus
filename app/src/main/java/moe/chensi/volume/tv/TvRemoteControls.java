package moe.chensi.volume.tv;

/** Android-independent remote rules. Key codes follow android.view.KeyEvent. */
public final class TvRemoteControls {
    private TvRemoteControls() {}

    public static int move(int current, int direction, int count) {
        if (count <= 0) return -1;
        if (current < 0) return 0;
        return Math.max(0, Math.min(count - 1, current + Integer.signum(direction)));
    }

    public static float adjust(float value, int direction) {
        // Work in integer percentage points to avoid accumulated floating-point drift.
        int percent = Math.round(value * 100f) + Integer.signum(direction) * 5;
        return Math.max(0, Math.min(100, percent)) / 100f;
    }

    public static TvRemoteAction action(int keyCode, int eventAction, int repeatCount) {
        if (eventAction != 0) return TvRemoteAction.NONE;
        switch (keyCode) {
            case 19: return TvRemoteAction.UP;
            case 20: return TvRemoteAction.DOWN;
            case 21: return TvRemoteAction.LEFT;
            case 22: return TvRemoteAction.RIGHT;
            case 23:
            case 66:
            case 160: return repeatCount == 0 ? TvRemoteAction.CONFIRM : TvRemoteAction.NONE;
            default: return TvRemoteAction.NONE;
        }
    }
}
