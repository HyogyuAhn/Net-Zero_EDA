package insa.eda.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import insa.eda.R

class DrivingResultActivity : AppCompatActivity() {
    
    private lateinit var tvScore: TextView
    private lateinit var tvScoreMessage: TextView
    private lateinit var tvDrivingTime: TextView
    private lateinit var tvDrivingDistance: TextView
    private lateinit var tvSuddenAccelCount: TextView
    private lateinit var tvSuddenBrakeCount: TextView
    private lateinit var tvIdlingCount: TextView
    private lateinit var tvCarbonEmission: TextView
    private lateinit var btnClose: Button
    private lateinit var btnViewStats: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving_result)
        
        initializeViews()
        displayResults()
        setupButtons()
    }
    
    private fun initializeViews() {
        tvScore = findViewById(R.id.tv_score)
        tvScoreMessage = findViewById(R.id.tv_score_message)
        tvDrivingTime = findViewById(R.id.tv_driving_time)
        tvDrivingDistance = findViewById(R.id.tv_driving_distance)
        tvSuddenAccelCount = findViewById(R.id.tv_sudden_accel_count)
        tvSuddenBrakeCount = findViewById(R.id.tv_sudden_brake_count)
        tvIdlingCount = findViewById(R.id.tv_idling_count)
        tvCarbonEmission = findViewById(R.id.tv_carbon_emission)
        btnClose = findViewById(R.id.btn_close)
        btnViewStats = findViewById(R.id.btn_view_stats)
    }
    
    private fun displayResults() {
        val score = intent.getIntExtra("SCORE", 0)
        val suddenAccelCount = intent.getIntExtra("SUDDEN_ACCEL", 0)
        val suddenBrakeCount = intent.getIntExtra("SUDDEN_BRAKE", 0)
        val idlingCount = intent.getIntExtra("IDLING", 0)
        val drivingTimeInMinutes = intent.getIntExtra("DRIVING_TIME_MINUTES", 0)
        val drivingDistance = intent.getFloatExtra("DRIVING_DISTANCE", 0f)
        val carbonEmission = intent.getFloatExtra("CARBON_EMISSION", 0f)
        
        tvScore.text = score.toString()
        
        tvScoreMessage.text = when {
            score >= 90 -> "훌륭한 에코 드라이버입니다!"
            score >= 80 -> "좋은 운전 습관을 가지고 계시네요!"
            score >= 70 -> "조금만 더 노력하면 좋은 에코 드라이버가 될 수 있어요!"
            else -> "에코 드라이빙 습관을 개선해보세요."
        }
        
        tvScore.setTextColor(resources.getColor(when {
            score >= 80 -> android.R.color.holo_green_dark
            score >= 60 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }, theme))
        
        val hours = drivingTimeInMinutes / 60
        val minutes = drivingTimeInMinutes % 60
        tvDrivingTime.text = String.format("%02d:%02d:00", hours, minutes)
        
        tvDrivingDistance.text = String.format("%.1f km", drivingDistance)
        tvSuddenAccelCount.text = String.format("%d회", suddenAccelCount)
        tvSuddenBrakeCount.text = String.format("%d회", suddenBrakeCount)
        tvIdlingCount.text = String.format("%d회", idlingCount)
        tvCarbonEmission.text = String.format("%.2f kg", carbonEmission)
    }
    
    private fun setupButtons() {
        btnClose.setOnClickListener {
            finish()
        }
        
        btnViewStats.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }
}
