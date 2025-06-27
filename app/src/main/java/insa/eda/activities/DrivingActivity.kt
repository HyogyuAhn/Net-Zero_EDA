package insa.eda.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import insa.eda.R
import insa.eda.sensors.DriveEventDetector
import insa.eda.sensors.DrivingSensorManager
import insa.eda.utils.TTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.Date
import insa.eda.database.FirebaseHelper

class DrivingActivity : AppCompatActivity() {
    private val tag = "DrivingActivity"
    
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var naviContainer: View
    private lateinit var navBar: ConstraintLayout
    private lateinit var tvDrivingTime: TextView
    private lateinit var btnEndDriving: Button
    private lateinit var eventNotificationCard: CardView
    private lateinit var tvEventTitle: TextView
    private lateinit var tvEventMessage: TextView
    private lateinit var sensorManager: DrivingSensorManager
    private lateinit var eventDetector: DriveEventDetector
    private lateinit var ttsManager: TTSManager
    
    private var suddenAccelCount = 0
    private var suddenBrakeCount = 0
    private var idlingCount = 0
    
    private var recordId = UUID.randomUUID().toString()
    private var startTime = SystemClock.elapsedRealtime()
    private var isNavBarVisible = false
    private var destinationName = ""
    private var destinationLat = 0.0
    private var destinationLon = 0.0
    private var drivingStartTimestamp = Date()
    
    private val handler = Handler(Looper.getMainLooper())
    private val navBarHideRunnable = Runnable { hideNavBar() }
    private val eventNotificationHideRunnable = Runnable { hideEventNotification() }
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimer()
            handler.postDelayed(this, 1000)
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)
        
        intent.extras?.let {
            destinationName = it.getString("DESTINATION_NAME", "")
            destinationLat = it.getDouble("DESTINATION_LAT", 0.0)
            destinationLon = it.getDouble("DESTINATION_LON", 0.0)
        }
        
        if (destinationName.isEmpty()) {
            Log.e(tag, "목적지 정보가 없습니다.")
            finish()
            return
        }
        
        initializeViews()
        setupTouchListeners()
        initializeSensors()
        startDriving()
    }
    
    private fun initializeViews() {
        rootLayout = findViewById(R.id.root_layout)
        naviContainer = findViewById(R.id.fl_navi_container)
        navBar = findViewById(R.id.cl_nav_bar)
        tvDrivingTime = findViewById(R.id.tv_driving_time)
        btnEndDriving = findViewById(R.id.btn_end_driving)
        eventNotificationCard = findViewById(R.id.cv_event_notification)
        tvEventTitle = findViewById(R.id.tv_event_title)
        tvEventMessage = findViewById(R.id.tv_event_message)
        
        btnEndDriving.setOnClickListener {
            endDriving()
        }
        
        navBar.visibility = View.GONE
        eventNotificationCard.visibility = View.GONE
    }
    
    private fun setupTouchListeners() {
        rootLayout.setOnClickListener {
            toggleNavBar()
        }
        
        naviContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isNavBarVisible) {
                hideNavBar()
                return@setOnTouchListener true
            }
            false
        }
    }
    
    private fun toggleNavBar() {
        if (isNavBarVisible) {
            hideNavBar()
        } else {
            showNavBar()
        }
    }
    
    private fun showNavBar() {
        handler.removeCallbacks(navBarHideRunnable)
        navBar.visibility = View.VISIBLE
        isNavBarVisible = true
        
        handler.postDelayed(navBarHideRunnable, 5000)
    }
    
    private fun hideNavBar() {
        navBar.visibility = View.GONE
        isNavBarVisible = false
    }
    
    private fun initializeSensors() {
        sensorManager = DrivingSensorManager(this)
        eventDetector = DriveEventDetector()
        ttsManager = TTSManager(this)
    }
    
    private fun startDriving() {
        startTime = SystemClock.elapsedRealtime()
        drivingStartTimestamp = Date()
        
        handler.post(timerRunnable)
        
        sensorManager.startSensing()
        
        scope.launch {
            sensorManager.sensorData.collect { sensorData ->
                eventDetector.processSensorData(sensorData)
            }
        }
        
        scope.launch {
            eventDetector.driveEvents.collect { event ->
                event?.let { handleDriveEvent(it) }
            }
        }
        
        ttsManager.speak("목적지 ${destinationName}으로 주행을 시작합니다.")
        Toast.makeText(this, "목적지: $destinationName", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateTimer() {
        val elapsedMillis = SystemClock.elapsedRealtime() - startTime
        val hours = (elapsedMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((elapsedMillis / (1000 * 60)) % 60).toInt()
        val seconds = ((elapsedMillis / 1000) % 60).toInt()
        
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        tvDrivingTime.text = timeString
    }
    
    private fun handleDriveEvent(event: insa.eda.database.models.DrivingEvent) {
        when(event.type) {
            insa.eda.database.models.DrivingEventType.SUDDEN_ACCELERATION -> {
                suddenAccelCount++
                showEventNotification("급발진 감지!", "부드럽게 가속해 주세요.")
                ttsManager.speak("급발진이 감지되었습니다. 부드럽게 가속해 주세요.")
            }
            insa.eda.database.models.DrivingEventType.SUDDEN_BRAKE -> {
                suddenBrakeCount++
                showEventNotification("급제동 감지!", "부드럽게 감속해 주세요.")
                ttsManager.speak("급제동이 감지되었습니다. 부드럽게 감속해 주세요.")
            }
            insa.eda.database.models.DrivingEventType.IDLING -> {
                idlingCount++
                showEventNotification("공회전 감지!", "1분 이상 정차 중입니다.")
                ttsManager.speak("1분 이상 정차 중입니다. 불필요한 공회전을 줄여주세요.")
            }
        }
        
        Log.d(tag, "주행 이벤트 감지: ${event.type}, 시간: ${event.timestamp}")
    }
    
    private fun showEventNotification(title: String, message: String) {
        handler.removeCallbacks(eventNotificationHideRunnable)
        
        tvEventTitle.text = title
        tvEventMessage.text = message
        eventNotificationCard.visibility = View.VISIBLE
        
        handler.postDelayed(eventNotificationHideRunnable, 5000)
    }
    
    private fun hideEventNotification() {
        eventNotificationCard.visibility = View.GONE
    }
    
    private fun endDriving() {
        val elapsedMillis = SystemClock.elapsedRealtime() - startTime
        val drivingDurationInMinutes = elapsedMillis / (1000 * 60)
        
        if (drivingDurationInMinutes < 1) {
            Toast.makeText(this, "최소 주행 시간(1분)이 되지 않았습니다.", Toast.LENGTH_SHORT).show()
            ttsManager.speak("최소 주행 시간 1분이 되지 않았습니다. 주행을 계속합니다.")
            return
        }
        
        stopAllServices()
        
        saveDrivingRecord(elapsedMillis)
        
        showDrivingResult()
    }
    
    private fun stopAllServices() {
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(navBarHideRunnable)
        handler.removeCallbacks(eventNotificationHideRunnable)
        
        sensorManager.stopSensing()
    }
    
    private fun saveDrivingRecord(elapsedMillis: Long) {
        val endTimestamp = Date()
        
        val drivingDurationInMinutes = elapsedMillis / (1000 * 60)
        
        val carbonEmission = (drivingDurationInMinutes / 10.0).coerceAtLeast(0.1)
        
        val estimatedDistance = 40 * (drivingDurationInMinutes / 60.0f)
        
        val score = calculateDrivingScore()
        
        scope.launch {
            try {
                val currentUser = FirebaseHelper.getCurrentUser()
                
                if (currentUser != null) {
                    val userId = currentUser.uid
                    
                    val result = FirebaseHelper.saveDrivingRecord(
                        userId = userId,
                        startTime = drivingStartTimestamp,
                        endTime = endTimestamp,
                        duration = drivingDurationInMinutes.toInt(),
                        distance = estimatedDistance,
                        avgSpeed = 40f,
                        ecoScore = score,
                        co2Saved = carbonEmission.toFloat()
                    )
                    
                    withContext(Dispatchers.Main) {
                        result.onSuccess { recordId ->
                            Log.d(tag, "주행 기록 Firebase 저장 성공: $recordId")
                            
                            scope.launch {
                                val updateData = mapOf(
                                    "sudden_acceleration_count" to suddenAccelCount,
                                    "sudden_brake_count" to suddenBrakeCount,
                                    "idling_count" to idlingCount
                                )
                                
                                FirebaseHelper.updateDrivingRecord(recordId, updateData)
                            }
                        }.onFailure { e ->
                            Log.e(tag, "주행 기록 Firebase 저장 실패", e)
                        }
                    }
                } else {
                    Log.e(tag, "주행 기록 저장 실패: 사용자 로그인 필요")
                }
            } catch (e: Exception) {
                Log.e(tag, "주행 기록 저장 중 오류 발생", e)
            }
        }
        
        Log.d(tag, "주행 기록 - 시작: $drivingStartTimestamp, 종료: $endTimestamp")
        Log.d(tag, "주행 이벤트 - 급발진: $suddenAccelCount, 급제동: $suddenBrakeCount, 공회전: $idlingCount")
        Log.d(tag, "에코 점수: $score, 탄소 배출: $carbonEmission kg")
    }
    
    private fun calculateDrivingScore(): Int {
        var score = 100
        score -= suddenAccelCount * 5 
        score -= suddenBrakeCount * 5 
        score -= idlingCount * 3       
        
        return score.coerceIn(0, 100)
    }
    
    private fun showDrivingResult() {
        val elapsedMillis = SystemClock.elapsedRealtime() - startTime
        val drivingDurationInMinutes = elapsedMillis / (1000 * 60)
        val carbonEmission = (drivingDurationInMinutes / 10.0).coerceAtLeast(0.1)
        val estimatedDistance = 40 * (drivingDurationInMinutes / 60.0f)
        
        val intent = Intent(this, DrivingResultActivity::class.java).apply {
            putExtra("RECORD_ID", recordId)
            putExtra("SUDDEN_ACCEL", suddenAccelCount)
            putExtra("SUDDEN_BRAKE", suddenBrakeCount)
            putExtra("IDLING", idlingCount)
            putExtra("SCORE", calculateDrivingScore())
            putExtra("DRIVING_TIME_MINUTES", drivingDurationInMinutes.toInt())
            putExtra("DRIVING_DISTANCE", estimatedDistance)
            putExtra("CARBON_EMISSION", carbonEmission.toFloat())
        }
        
        startActivity(intent)
        finish()
    }
    
    override fun onPause() {
        super.onPause()
    }
    
    override fun onResume() {
        super.onResume()
    }
    
    override fun onDestroy() {
        stopAllServices()
        ttsManager.release()
        super.onDestroy()
    }
}
