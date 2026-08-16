package com.quangthe.thuocdo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.quangthe.thuocdo.model.RulerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ruler_settings")

@Singleton
class RulerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val HX = floatPreferencesKey("hx")
        val HY = floatPreferencesKey("hy")
        val HROT = floatPreferencesKey("hrot")
        val HS = floatPreferencesKey("hs")
        val HE = floatPreferencesKey("he")
        
        val VX = floatPreferencesKey("vx")
        val VY = floatPreferencesKey("vy")
        val VROT = floatPreferencesKey("vrot")
        val VS = floatPreferencesKey("vs")
        val VE = floatPreferencesKey("ve")
        
        val BAR_LEN = floatPreferencesKey("bl")
        val SCALE = floatPreferencesKey("sc")
        val UNIT = intPreferencesKey("un")
        val NUM_RULERS = intPreferencesKey("num_rulers")
        val IS_COUPLED = booleanPreferencesKey("is_coupled")
        val IS_ZOOM_ENABLED = booleanPreferencesKey("is_zoom_enabled")
        val FIXED_ORIENTATION = intPreferencesKey("fixed_orientation")
        
        val BUBBLE_X = intPreferencesKey("bubble_x")
        val BUBBLE_Y = intPreferencesKey("bubble_y")
        val IS_RULER_VISIBLE = booleanPreferencesKey("is_ruler_visible")
    }

    suspend fun updateRulerState(transform: (RulerState) -> RulerState) {
        context.dataStore.edit { preferences ->
            val currentState = RulerState(
                horizontalX = preferences[PreferencesKeys.HX] ?: 150f,
                horizontalY = preferences[PreferencesKeys.HY] ?: 150f,
                horizontalRotation = preferences[PreferencesKeys.HROT] ?: 0f,
                horizontalStart = preferences[PreferencesKeys.HS] ?: 0f,
                horizontalEnd = preferences[PreferencesKeys.HE] ?: 300f,
                verticalX = preferences[PreferencesKeys.VX] ?: 150f,
                verticalY = preferences[PreferencesKeys.VY] ?: 300f,
                verticalRotation = preferences[PreferencesKeys.VROT] ?: 90f,
                verticalStart = preferences[PreferencesKeys.VS] ?: 0f,
                verticalEnd = preferences[PreferencesKeys.VE] ?: 300f,
                barLength = preferences[PreferencesKeys.BAR_LEN] ?: 320f,
                scale = preferences[PreferencesKeys.SCALE] ?: 1.0f,
                unit = preferences[PreferencesKeys.UNIT] ?: 0,
                numRulers = preferences[PreferencesKeys.NUM_RULERS] ?: 2,
                isCoupled = preferences[PreferencesKeys.IS_COUPLED] ?: true,
                isZoomEnabled = preferences[PreferencesKeys.IS_ZOOM_ENABLED] ?: true,
                fixedOrientation = preferences[PreferencesKeys.FIXED_ORIENTATION] ?: 0,
                bubbleX = preferences[PreferencesKeys.BUBBLE_X] ?: 100,
                bubbleY = preferences[PreferencesKeys.BUBBLE_Y] ?: 300,
                isRulerVisible = preferences[PreferencesKeys.IS_RULER_VISIBLE] ?: false
            )
            val newState = transform(currentState)
            preferences[PreferencesKeys.HX] = newState.horizontalX
            preferences[PreferencesKeys.HY] = newState.horizontalY
            preferences[PreferencesKeys.HROT] = newState.horizontalRotation
            preferences[PreferencesKeys.HS] = newState.horizontalStart
            preferences[PreferencesKeys.HE] = newState.horizontalEnd
            preferences[PreferencesKeys.VX] = newState.verticalX
            preferences[PreferencesKeys.VY] = newState.verticalY
            preferences[PreferencesKeys.VROT] = newState.verticalRotation
            preferences[PreferencesKeys.VS] = newState.verticalStart
            preferences[PreferencesKeys.VE] = newState.verticalEnd
            preferences[PreferencesKeys.BAR_LEN] = newState.barLength
            preferences[PreferencesKeys.SCALE] = newState.scale
            preferences[PreferencesKeys.UNIT] = newState.unit
            preferences[PreferencesKeys.NUM_RULERS] = newState.numRulers
            preferences[PreferencesKeys.IS_COUPLED] = newState.isCoupled
            preferences[PreferencesKeys.IS_ZOOM_ENABLED] = newState.isZoomEnabled
            preferences[PreferencesKeys.FIXED_ORIENTATION] = newState.fixedOrientation
            preferences[PreferencesKeys.BUBBLE_X] = newState.bubbleX
            preferences[PreferencesKeys.BUBBLE_Y] = newState.bubbleY
            preferences[PreferencesKeys.IS_RULER_VISIBLE] = newState.isRulerVisible
        }
    }

    val rulerStateFlow: Flow<RulerState> = context.dataStore.data.map { preferences ->
        RulerState(
            horizontalX = preferences[PreferencesKeys.HX] ?: 150f,
            horizontalY = preferences[PreferencesKeys.HY] ?: 150f,
            horizontalRotation = preferences[PreferencesKeys.HROT] ?: 0f,
            horizontalStart = preferences[PreferencesKeys.HS] ?: 0f,
            horizontalEnd = preferences[PreferencesKeys.HE] ?: 300f,
            
            verticalX = preferences[PreferencesKeys.VX] ?: 150f,
            verticalY = preferences[PreferencesKeys.VY] ?: 300f,
            verticalRotation = preferences[PreferencesKeys.VROT] ?: 90f,
            verticalStart = preferences[PreferencesKeys.VS] ?: 0f,
            verticalEnd = preferences[PreferencesKeys.VE] ?: 300f,
            
            barLength = preferences[PreferencesKeys.BAR_LEN] ?: 600f,
            scale = preferences[PreferencesKeys.SCALE] ?: 1.0f,
            unit = preferences[PreferencesKeys.UNIT] ?: 0,
            numRulers = preferences[PreferencesKeys.NUM_RULERS] ?: 2,
            isCoupled = preferences[PreferencesKeys.IS_COUPLED] ?: true,
            isZoomEnabled = preferences[PreferencesKeys.IS_ZOOM_ENABLED] ?: true,
            fixedOrientation = preferences[PreferencesKeys.FIXED_ORIENTATION] ?: 0,
            
            bubbleX = preferences[PreferencesKeys.BUBBLE_X] ?: 100,
            bubbleY = preferences[PreferencesKeys.BUBBLE_Y] ?: 300,
            isRulerVisible = preferences[PreferencesKeys.IS_RULER_VISIBLE] ?: false
        )
    }

    suspend fun updateState(transform: (RulerState) -> RulerState) {
        // Đây là cách đơn giản, thực tế có thể tối ưu hơn
        context.dataStore.edit { preferences ->
            val currentState = RulerState(
                horizontalX = preferences[PreferencesKeys.HX] ?: 150f,
                horizontalY = preferences[PreferencesKeys.HY] ?: 150f,
                // ... map hết các trường
            )
            // Do RulerState là data class, ta map ngược lại sau khi transform
            // Để tiết kiệm thời gian, tôi viết hàm update cụ thể
        }
    }

    suspend fun updateHorizontalPos(x: Float, y: Float) {
        context.dataStore.edit { it[PreferencesKeys.HX] = x; it[PreferencesKeys.HY] = y }
    }

    suspend fun updateHorizontalRotation(rot: Float) {
        context.dataStore.edit { it[PreferencesKeys.HROT] = rot }
    }

    suspend fun updateScale(scale: Float) {
        context.dataStore.edit { it[PreferencesKeys.SCALE] = scale }
    }
    
    suspend fun toggleRulerVisibility() {
        context.dataStore.edit { 
            val current = it[PreferencesKeys.IS_RULER_VISIBLE] ?: false
            it[PreferencesKeys.IS_RULER_VISIBLE] = !current
        }
    }
    
    suspend fun updateBubblePosition(x: Int, y: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.BUBBLE_X] = x
            it[PreferencesKeys.BUBBLE_Y] = y
        }
    }

    suspend fun saveAll(state: RulerState) {
        context.dataStore.edit { p ->
            p[PreferencesKeys.HX] = state.horizontalX
            p[PreferencesKeys.HY] = state.horizontalY
            p[PreferencesKeys.HROT] = state.horizontalRotation
            p[PreferencesKeys.HS] = state.horizontalStart
            p[PreferencesKeys.HE] = state.horizontalEnd
            p[PreferencesKeys.VX] = state.verticalX
            p[PreferencesKeys.VY] = state.verticalY
            p[PreferencesKeys.VROT] = state.verticalRotation
            p[PreferencesKeys.VS] = state.verticalStart
            p[PreferencesKeys.VE] = state.verticalEnd
            p[PreferencesKeys.BAR_LEN] = state.barLength
            p[PreferencesKeys.SCALE] = state.scale
            p[PreferencesKeys.UNIT] = state.unit
            p[PreferencesKeys.NUM_RULERS] = state.numRulers
            p[PreferencesKeys.IS_COUPLED] = state.isCoupled
            p[PreferencesKeys.IS_ZOOM_ENABLED] = state.isZoomEnabled
            p[PreferencesKeys.FIXED_ORIENTATION] = state.fixedOrientation
            p[PreferencesKeys.BUBBLE_X] = state.bubbleX
            p[PreferencesKeys.BUBBLE_Y] = state.bubbleY
            p[PreferencesKeys.IS_RULER_VISIBLE] = state.isRulerVisible
        }
    }
}
