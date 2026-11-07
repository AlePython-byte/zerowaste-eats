package com.zerowasteeats.app.data.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.zerowasteeats.app.data.session.KeyValueStore

/** Where the app's data comes from. */
enum class DataMode {
    /** Supabase and the backend. */
    LIVE,

    /** The in-memory demo accounts and data; works offline and without any configuration. */
    DEMO
}

/** Remembers the chosen [DataMode] between launches. A plain preference, so it may be backed up. */
class DataModeStore(private val store: KeyValueStore) {

    /** The saved mode, or null when none was chosen yet. */
    fun load(): DataMode? = store.getString(KEY_MODE)?.let { name ->
        DataMode.entries.firstOrNull { it.name == name }
    }

    fun save(mode: DataMode) = store.replaceAll(mapOf(KEY_MODE to mode.name))

    companion object {
        const val FILE_NAME = "data_mode"
        private const val KEY_MODE = "mode"
    }
}

/**
 * Decides the current [DataMode] and when it may change. Live data needs the endpoints from
 * local.properties; without them ([liveAvailable] false) only demo data exists. The mode only
 * changes while nobody is signed in, so a session always belongs to the mode that created it.
 */
class DataModeController(
    private val store: DataModeStore,
    val liveAvailable: Boolean,
    private val isSignedIn: () -> Boolean,
    /** Runs after every change, to rebuild what depends on the mode. */
    private val onChanged: (DataMode) -> Unit = {}
) {
    private val modeState = mutableStateOf(
        if (liveAvailable) store.load() ?: DataMode.LIVE else DataMode.DEMO
    )

    val mode: State<DataMode> get() = modeState

    /** Whether the switch can be used right now. */
    val canChange: Boolean get() = liveAvailable && !isSignedIn()

    /** Switches to [mode]. Returns false, changing nothing, when that is not allowed now. */
    fun setMode(mode: DataMode): Boolean {
        if (mode == modeState.value) return true
        if (!canChange) return false
        store.save(mode)
        modeState.value = mode
        onChanged(mode)
        return true
    }
}
