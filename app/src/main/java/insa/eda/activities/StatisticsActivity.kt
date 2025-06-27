package insa.eda.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import insa.eda.R
import insa.eda.adapters.DrivingRecordAdapter
import insa.eda.database.FirebaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class StatisticsActivity : AppCompatActivity() {
    
    private val tag = "StatisticsActivity"
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvAvgScore: TextView
    private lateinit var tvDriveCount: TextView
    private lateinit var tvTotalSuddenAccel: TextView
    private lateinit var tvTotalSuddenBrake: TextView
    private lateinit var tvTotalIdling: TextView
    private lateinit var tvTotalCarbon: TextView
    private lateinit var rvDrivingRecords: RecyclerView
    
    private val adapter = DrivingRecordAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        initViews()
        setupRecyclerView()
        loadDrivingRecords()
    }
    
    private fun initViews() {
        tvTotalDistance = findViewById(R.id.tv_total_distance)
        tvTotalTime = findViewById(R.id.tv_total_time)
        tvAvgScore = findViewById(R.id.tv_avg_score)
        tvDriveCount = findViewById(R.id.tv_drive_count)
        tvTotalSuddenAccel = findViewById(R.id.tv_total_sudden_accel)
        tvTotalSuddenBrake = findViewById(R.id.tv_total_sudden_brake)
        tvTotalIdling = findViewById(R.id.tv_total_idling)
        tvTotalCarbon = findViewById(R.id.tv_total_carbon)
        rvDrivingRecords = findViewById(R.id.rv_driving_records)
    }
    
    private fun setupRecyclerView() {
        rvDrivingRecords.layoutManager = LinearLayoutManager(this)
        rvDrivingRecords.adapter = adapter
    }
    
    private fun loadDrivingRecords() {
        val currentUser = FirebaseHelper.getCurrentUser()
        
        if (currentUser == null) {
            Log.e(tag, "사용자가 로그인되어 있지 않습니다.")
            return
        }
        
        scope.launch {
            try {
                val result = FirebaseHelper.getUserDrivingRecords(currentUser.uid)
                
                result.onSuccess { records ->
                    val recordItems = records.mapNotNull { recordMap ->
                        try {
                            val id = recordMap["id"] as? String ?: ""
                            val startTime = recordMap["start_time"] as? Date ?: Date()
                            val endTime = recordMap["end_time"] as? Date
                            val duration = (recordMap["duration"] as? Number)?.toInt() ?: 0
                            val distance = (recordMap["distance"] as? Number)?.toFloat() ?: 0f
                            val ecoScore = (recordMap["eco_score"] as? Number)?.toInt() ?: 0
                            val suddenAccelCount = (recordMap["sudden_acceleration_count"] as? Number)?.toInt() ?: 0
                            val suddenBrakeCount = (recordMap["sudden_brake_count"] as? Number)?.toInt() ?: 0
                            val idlingCount = (recordMap["idling_count"] as? Number)?.toInt() ?: 0
                            val co2Emission = (recordMap["co2_saved"] as? Number)?.toFloat() ?: 0f
                            
                            DrivingRecordAdapter.DrivingRecordItem(
                                id = id,
                                startTime = startTime,
                                endTime = endTime,
                                duration = duration,
                                distance = distance,
                                ecoScore = ecoScore,
                                suddenAccelCount = suddenAccelCount,
                                suddenBrakeCount = suddenBrakeCount,
                                idlingCount = idlingCount,
                                co2Emission = co2Emission
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "주행 기록 변환 오류: ${e.message}")
                            null
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        adapter.updateRecords(recordItems)
                        calculateAndDisplayStatistics(recordItems)
                    }
                }.onFailure { e ->
                    Log.e(tag, "주행 기록 로드 실패: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(tag, "주행 기록 로드 중 오류 발생: ${e.message}")
            }
        }
    }
    
    private fun calculateAndDisplayStatistics(records: List<DrivingRecordAdapter.DrivingRecordItem>) {
        if (records.isEmpty()) {
            return
        }
        
        val totalDistance = records.sumOf { it.distance.toDouble() }
        tvTotalDistance.text = String.format("%.1f km", totalDistance)
        
        val totalTimeMinutes = records.sumOf { it.duration.toLong() }
        val hours = totalTimeMinutes / 60
        val minutes = totalTimeMinutes % 60
        tvTotalTime.text = if (hours > 0) {
            String.format("%d시간 %d분", hours, minutes)
        } else {
            String.format("%d분", minutes)
        }
        
        val avgScore = records.map { it.ecoScore }.average().toInt()
        tvAvgScore.text = String.format("%d점", avgScore)
        
        tvDriveCount.text = String.format("%d회", records.size)
        
        val totalSuddenAccel = records.sumOf { it.suddenAccelCount }
        val totalSuddenBrake = records.sumOf { it.suddenBrakeCount }
        val totalIdling = records.sumOf { it.idlingCount }
        tvTotalSuddenAccel.text = String.format("%d회", totalSuddenAccel)
        tvTotalSuddenBrake.text = String.format("%d회", totalSuddenBrake)
        tvTotalIdling.text = String.format("%d회", totalIdling)
        
        val totalCarbon = records.sumOf { it.co2Emission.toDouble() }
        tvTotalCarbon.text = String.format("%.2f kg", totalCarbon)
    }
}
