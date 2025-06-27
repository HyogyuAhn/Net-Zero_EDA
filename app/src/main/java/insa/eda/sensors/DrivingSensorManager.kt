package insa.eda.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DrivingSensorManager(private val context: Context) : SensorEventListener {
data class SensorData(
    val accelerometer: FloatArray = FloatArray(3) { 0f },
    val gyroscope: FloatArray = FloatArray(3) { 0f },
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (!accelerometer.contentEquals(other.accelerometer)) return false
        if (!gyroscope.contentEquals(other.gyroscope)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accelerometer.contentHashCode()
        result = 31 * result + gyroscope.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
    private val tag = "DrivingSensorManager"
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _accelerometerData = MutableStateFlow(FloatArray(3) { 0f })
    val accelerometerData: StateFlow<FloatArray> = _accelerometerData.asStateFlow()
    
    private val _gyroscopeData = MutableStateFlow(FloatArray(3) { 0f })
    val gyroscopeData: StateFlow<FloatArray> = _gyroscopeData.asStateFlow()
    
    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()
    
    val hasRequiredSensors: Boolean
        get() = accelerometerSensor != null && gyroscopeSensor != null
    
    fun startSensing() {
        if (!hasRequiredSensors) {
            Log.e(tag, "필요한 센서가 기기에 없습니다")
            return
        }
        
        accelerometerSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME 
            )
        }
        
        gyroscopeSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        
        Log.d(tag, "센서 모니터링 시작됨")
    }
    
    fun stopSensing() {
        sensorManager.unregisterListener(this)
        Log.d(tag, "센서 모니터링 종료됨")
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                _accelerometerData.value = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                _gyroscopeData.value = event.values.clone()
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
