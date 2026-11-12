package org.mupen64plus.ae.v3.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mapea el giroscopio del teléfono a entrada de joystick virtual para juegos de carreras N64.
 *
 * Funcionamiento:
 * - Inclinación izquierda/derecha (roll) → Eje X del joystick (dirección)
 * - Inclinación adelante/atrás (pitch) → Eje Y del joystick (aceleración/freno opcional)
 * - Solo activo cuando RacingGameDetector indica que el juego actual es de carreras
 * - Se desactiva automáticamente si el usuario toca el joystick virtual en pantalla
 */
class GyroscopeInputMapper(
    private val context: Context,
    private val racingGameDetector: RacingGameDetector
) : SensorEventListener, LifecycleObserver {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Estado actual del joystick virtual (-1.0 a 1.0)
    private val _joystickX = MutableStateFlow(0f)
    private val _joystickY = MutableStateFlow(0f)
    val joystickX: StateFlow<Float> = _joystickX.asStateFlow()
    val joystickY: StateFlow<Float> = _joystickY.asStateFlow()

    // Control de activación
    private val isEnabled = AtomicBoolean(false)
    private val isListening = AtomicBoolean(false)
    private var lastTimestamp: Long = 0

    // Configuración de sensibilidad (ajustable en settings)
    companion object {
        const val DEFAULT_SENSITIVITY = 1.5f
        const val MAX_TILT_ANGLE = 45f // grados máximos de inclinación útil
        const val DEAD_ZONE = 5f       // zona muerta en grados
        const val SMOOTHING_FACTOR = 0.15f // factor de suavizado (0-1)
    }

    var sensitivity: Float = DEFAULT_SENSITIVITY
        set(value) { field = value.coerceIn(0.1f, 5f) }

    // Integración de giroscopio para obtener orientación absoluta
    private val orientation = FloatArray(3) // [pitch, roll, yaw]
    private val rotationMatrix = FloatArray(9)

    /**
     * Activar/desactivar el mapeo de giroscopio.
     * Solo tiene efecto si RacingGameDetector dice que es juego de carreras.
     */
    fun setEnabled(enabled: Boolean) {
        val shouldEnable = enabled && racingGameDetector.isRacingGame.value
        if (isEnabled.getAndSet(shouldEnable) != shouldEnable) {
            if (shouldEnable) startListening() else stopListening()
        }
    }

    /** Observer de lifecycle para pausar/reanudar automáticamente */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        if (isEnabled.get()) startListening()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        stopListening()
    }

    private fun startListening() {
        if (isListening.getAndSet(true)) return
        if (gyroscope != null) sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        if (rotationVector != null) sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        lastTimestamp = 0
        orientation[0] = 0f; orientation[1] = 0f; orientation[2] = 0f
    }

    private fun stopListening() {
        if (!isListening.getAndSet(false)) return
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isEnabled.get()) return

        val now = System.nanoTime()
        if (lastTimestamp == 0L) {
            lastTimestamp = now
            return
        }
        val dt = (now - lastTimestamp) / 1_000_000_000.0 // segundos
        lastTimestamp = now

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> integrateGyroscope(event.values, dt)
            Sensor.TYPE_ROTATION_VECTOR -> correctWithRotationVector(event.values)
        }

        updateJoystickOutput()
    }

    private fun integrateGyroscope(gyro: FloatArray, dt: Double) {
        // Integración simple: ángulo += velocidad_angular * dt
        // Gyroscope values: [x, y, z] en rad/s
        // x = pitch (adelante/atrás), y = roll (izq/der), z = yaw
        orientation[0] += (gyro[0] * dt).toFloat() // pitch
        orientation[1] += (gyro[1] * dt).toFloat() // roll
        orientation[2] += (gyro[2] * dt).toFloat() // yaw (no usado)

        // Drift correction: clamp razonable
        orientation[0] = orientation[0].coerceIn(-Math.PI.toFloat(), Math.PI.toFloat())
        orientation[1] = orientation[1].coerceIn(-Math.PI.toFloat(), Math.PI.toFloat())
    }

    private fun correctWithRotationVector(rv: FloatArray) {
        // Rotation vector nos da orientación absoluta (sin drift)
        // Lo usamos para corregir el drift del giroscopio lentamente
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rv)

        // Ajustar por orientación de pantalla
        val screenRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation
        val remapped = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            getRemapX(screenRotation),
            getRemapY(screenRotation),
            remapped
        )

        val corrected = FloatArray(3)
        SensorManager.getOrientation(remapped, corrected)
        // corrected: [azimuth, pitch, roll]

        // Fusionar con filtro complementario (95% giroscopio, 5% rotation vector)
        val alpha = 0.95f
        orientation[0] = alpha * orientation[0] + (1 - alpha) * corrected[1] // pitch
        orientation[1] = alpha * orientation[1] + (1 - alpha) * corrected[2] // roll
    }

    private fun getRemapX(rotation: Int): Int = when (rotation) {
        Surface.ROTATION_0 -> SensorManager.AXIS_X
        Surface.ROTATION_90 -> SensorManager.AXIS_Y
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y
        else -> SensorManager.AXIS_X
    }

    private fun getRemapY(rotation: Int): Int = when (rotation) {
        Surface.ROTATION_0 -> SensorManager.AXIS_Y
        Surface.ROTATION_90 -> SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_X
        else -> SensorManager.AXIS_Y
    }

    private fun updateJoystickOutput() {
        // Convertir orientación (radianes) a joystick (-1 a 1)
        val pitchDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()

        // Zona muerta
        val rawX = if (abs(rollDeg) < DEAD_ZONE) 0f else rollDeg
        val rawY = if (abs(pitchDeg) < DEAD_ZONE) 0f else pitchDeg

        // Normalizar a [-1, 1] con sensibilidad
        val maxAngle = MAX_TILT_ANGLE / sensitivity
        var targetX = (rawX / maxAngle).coerceIn(-1f, 1f)
        var targetY = (rawY / maxAngle).coerceIn(-1f, 1f)

        // Suavizado exponencial
        _joystickX.value = lerp(_joystickX.value, targetX, SMOOTHING_FACTOR)
        _joystickY.value = lerp(_joystickY.value, targetY, SMOOTHING_FACTOR)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /** Limpieza al destruir */
    fun destroy() {
        stopListening()
        CoroutineScope(Dispatchers.Main).launch {
            _joystickX.value = 0f
            _joystickY.value = 0f
        }
    }
}