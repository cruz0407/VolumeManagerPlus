package moe.chensi.volume.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import moe.chensi.volume.BuildConfig

object TvDevice {
    fun isTelevision(context: Context): Boolean =
        BuildConfig.TV_MODE || context.getSystemService(UiModeManager::class.java)
            ?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}
