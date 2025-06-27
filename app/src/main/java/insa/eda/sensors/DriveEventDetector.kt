package insa.eda.sensors

import android.location.Location
import android.util.Log
import insa.eda.database.models.DrivingEvent
import insa.eda.database.models.DrivingEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

class DriveEventDetector { 
    private val tag = "DriveEventDetector"
    
    companion object {
        private const val SUDDEN_ACCELERATION_THRESHOLD = 3.5f
        private const val SUDDEN_BRAKING_THRESHOLD = -3.0f
        private const val IDLING_MOVEMENT_THRESHOLD = 0.3f
        private const val IDLING_DURATION_MS = 60000L
        private const val EVENT_COOLDOWN_MS = 5000L
    }
    
    private var lastSuddenAccelerationTime = 0L
    private var lastSuddenBrakingTime = 0L
    private var idlingDetectionStartTime = 0L
    private var isIdlingDetected = false
    
    private var isMoving = false
    private var idlingJob: Job? = null
    
    private var currentLocation: Location? = null
    
    private val _detectedEvent = MutableStateFlow<DrivingEvent?>(null)
    val detectedEvent: StateFlow<DrivingEvent?> = _detectedEvent.asStateFlow()
    
    private val _driveEvents = MutableStateFlow<DrivingEvent?>(null)
    val driveEvents: StateFlow<DrivingEvent?> = _driveEvents.asStateFlow()
    
    private val _suddenAccelerationCount = MutableStateFlow(0)
    val suddenAccelerationCount: StateFlow<Int> = _suddenAccelerationCount.asStateFlow()
    
    private val _suddenBrakingCount = MutableStateFlow(0)
    val suddenBrakingCount: StateFlow<Int> = _suddenBrakingCount.asStateFlow()
    
    private val _idlingCount = MutableStateFlow(0)
    val idlingCount: StateFlow<Int> = _idlingCount.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Default)
    private var detectionJob: Job? = null
    
    fun processSensorData(sensorData: DrivingSensorManager.SensorData) {
        processAccelerometerData(sensorData.accelerometer)
    }
    
    fun startDetection() {
        Log.d(tag, "이벤트 감지 시작")
        
        detectionJob = scope.launch {}
        Log.d(tag, "이벤트 감지 활성화 완료")
    }
    
    fun stopDetection() {
        Log.d(tag, "이벤트 감지 중지")
        detectionJob?.cancel()
        idlingJob?.cancel()
    }
    
    fun updateLocation(location: Location) {
        currentLocation = location
    }
    
    private fun processAccelerometerData(accelValues: FloatArray) {
        val currentTime = System.currentTimeMillis()
        val forwardAcceleration = accelValues[0]
        val magnitude = sqrt(
            accelValues[0] * accelValues[0] +
            accelValues[1] * accelValues[1] +
            accelValues[2] * accelValues[2]
        )
        
        val movementIntensity = abs(magnitude - 9.8f)
        
        val wasMoving = isMoving
        isMoving = movementIntensity > IDLING_MOVEMENT_THRESHOLD
        
        if (forwardAcceleration > SUDDEN_ACCELERATION_THRESHOLD &&
            currentTime - lastSuddenAccelerationTime > EVENT_COOLDOWN_MS) {
            
            lastSuddenAccelerationTime = currentTime
            _suddenAccelerationCount.value = _suddenAccelerationCount.value + 1
            
            val event = DrivingEvent(
                id = UUID.randomUUID().toString(),
                recordId = "",
                type = DrivingEventType.SUDDEN_ACCELERATION,
                timestamp = Date(),
                intensity = forwardAcceleration,
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude
            )
            
            Log.d(tag, "급가속 감지: $forwardAcceleration m/s², 총 ${_suddenAccelerationCount.value}회")
            _detectedEvent.value = event
            _driveEvents.value = event
        }
        
        if (forwardAcceleration < SUDDEN_BRAKING_THRESHOLD &&
            currentTime - lastSuddenBrakingTime > EVENT_COOLDOWN_MS) {
            
            lastSuddenBrakingTime = currentTime
            _suddenBrakingCount.value = _suddenBrakingCount.value + 1
            
            val event = DrivingEvent(
                id = UUID.randomUUID().toString(),
                recordId = "", 
                type = DrivingEventType.SUDDEN_BRAKE,
                timestamp = Date(),
                intensity = forwardAcceleration,
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude
            )
            
            Log.d(tag, "급제동 감지: $forwardAcceleration m/s², 총 ${_suddenBrakingCount.value}회")
            _detectedEvent.value = event
            _driveEvents.value = event
        }
        
        if (wasMoving && !isMoving) {
            idlingDetectionStartTime = currentTime
            
            idlingJob?.cancel()
            
            idlingJob = scope.launch {
                delay(IDLING_DURATION_MS)
                
                if (!isMoving && !isIdlingDetected) {
                    isIdlingDetected = true
                    _idlingCount.value = _idlingCount.value + 1
                    
                    val event = DrivingEvent(
                        id = UUID.randomUUID().toString(),
                        recordId = "",
                        type = DrivingEventType.IDLING,
                        timestamp = Date(),
                        intensity = 0f,
                        latitude = currentLocation?.latitude,
                        longitude = currentLocation?.longitude
                    )
                    
                    Log.d(tag, "공회전 감지: ${IDLING_DURATION_MS/1000}초 이상 정지, 총 ${_idlingCount.value}회")
                    _detectedEvent.value = event
                    _driveEvents.value = event
                }
            }
        }
        
        if (!wasMoving && isMoving) {
            isIdlingDetected = false
            idlingJob?.cancel()
        }
    }
}
