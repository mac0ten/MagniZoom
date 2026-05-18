package com.boredjejemonph.magnizoom

enum class OverlayCaptureMode(val wireName: String) {
    TargetAppRealtime("target_app_realtime"),
    WholeScreenRealtime("whole_screen_realtime"),
    WholeScreenSnapshot("whole_screen_snapshot");

    companion object {
        fun fromWireName(value: String?): OverlayCaptureMode {
            return entries.firstOrNull { it.wireName == value } ?: TargetAppRealtime
        }
    }
}

enum class OverlayCaptureStatus {
    Idle,
    Starting,
    Live,
    Snapshot,
    Error
}
