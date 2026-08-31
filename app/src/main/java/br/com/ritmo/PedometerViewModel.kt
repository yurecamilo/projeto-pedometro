package br.com.ritmo

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.ritmo.data.ActivityDao
import br.com.ritmo.data.DailyActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class RunnerProfile(val strideCm: Int = 72, val weightKg: Int = 70)
data class PedometerUiState(
    val steps: Int = 0,
    val cadence: Int = 0,
    val sensorAvailable: Boolean = true,
    val profile: RunnerProfile = RunnerProfile()
) {
    val distanceKm: Double get() = steps * profile.strideCm / 100000.0
    val calories: Double get() = steps * profile.strideCm / 100000.0 * profile.weightKg * 0.75
}

class PedometerViewModel(application: Application, private val dao: ActivityDao) :
    AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val preferences = application.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(PedometerUiState(profile = loadProfile()))
    val state = _state.asStateFlow()
    private var lastCounter = -1f
    private var lastEventMillis = 0L
    private var lastAccelerometerStepMillis = 0L
    private var persistenceJob: Job? = null
    private val today get() = LocalDate.now()

    private val weekStart: Long get() = today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val week: StateFlow<List<DailyActivity>> = dao.observeFrom(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { loadToday() }

    fun startTracking() {
        val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val sensor = counter ?: detector ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            _state.value = _state.value.copy(sensorAvailable = false)
            return
        }
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopTracking() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val increment = when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val raw = event.values[0]
                val diff = if (lastCounter < 0 || raw < lastCounter) 0 else (raw - lastCounter).toInt()
                lastCounter = raw
                diff
            }
            Sensor.TYPE_STEP_DETECTOR -> 1
            Sensor.TYPE_ACCELEROMETER -> {
                val magnitude = kotlin.math.sqrt(
                    event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]
                )
                val now = System.currentTimeMillis()
                if (magnitude > 12.3f && now - lastAccelerometerStepMillis > 280) {
                    lastAccelerometerStepMillis = now
                    1
                } else 0
            }
            else -> 0
        }
        if (increment > 0) addSteps(increment)
        val now = System.currentTimeMillis()
        if (lastEventMillis > 0 && now > lastEventMillis) {
            val spm = (60_000.0 / (now - lastEventMillis)).toInt().coerceIn(0, 300)
            _state.value = _state.value.copy(cadence = spm)
        }
        lastEventMillis = now
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun updateProfile(strideCm: Int, weightKg: Int) {
        val profile = RunnerProfile(strideCm.coerceIn(30, 150), weightKg.coerceIn(25, 250))
        preferences.edit().putInt("stride", profile.strideCm).putInt("weight", profile.weightKg).apply()
        _state.value = _state.value.copy(profile = profile)
        persist()
    }

    private fun addSteps(increment: Int) {
        _state.value = _state.value.copy(steps = _state.value.steps + increment)
        persist()
    }

    private fun persist() {
        persistenceJob?.cancel()
        persistenceJob = viewModelScope.launch {
            val s = _state.value
            dao.save(DailyActivity(dayKey(today), s.steps, s.distanceKm * 1000, s.calories))
        }
    }

    private fun loadToday() = viewModelScope.launch {
        dao.get(dayKey(today))?.let { saved -> _state.value = _state.value.copy(steps = saved.steps) }
    }

    private fun loadProfile() = RunnerProfile(preferences.getInt("stride", 72), preferences.getInt("weight", 70))
    private fun dayKey(date: LocalDate) = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
