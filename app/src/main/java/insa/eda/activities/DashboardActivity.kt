package insa.eda.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import insa.eda.R
import insa.eda.database.FirebaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    
    private val tag = "DashboardActivity"
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private lateinit var tvAvgScore: TextView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvTotalDrives: TextView
    private lateinit var tvEcoTip: TextView
    
    private lateinit var btnProfile: ImageButton
    private lateinit var cvDestinationSearch: CardView
    private lateinit var cvStatistics: CardView
    private lateinit var cvEcoTips: CardView
    private lateinit var cvSettings: CardView
    
    private val ecoTips = listOf(
        "급가속과 급제동을 피하고 일정한 속도를 유지하면 연료 효율을 높일 수 있습니다.",
        "신호등 앞에서는 미리 감속하여 급제동을 피하세요.",
        "정차 시 엔진을 끄면 공회전으로 인한 불필요한 연료 소비를 줄일 수 있습니다.",
        "타이어 공기압을 적절하게 유지하면 연비를 5-10% 향상시킬 수 있습니다.",
        "과적을 피하세요. 차량 무게가 100kg 증가하면 연료 소비는 약 5% 증가합니다.",
        "에어컨 사용을 최소화하면 연료 효율을 높일 수 있습니다.",
        "엔진 오일과 에어필터를 정기적으로 교체하면 연료 효율을 개선할 수 있습니다.",
        "불필요한 차량 액세서리는 공기저항을 증가시켜 연비를 감소시킵니다.",
        "경제속도(60-80km/h)를 유지하면 최적의 연비를 얻을 수 있습니다.",
        "내리막길에서는 가속페달에서 발을 떼고 관성으로 주행하세요."
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        initViews()
        loadUserData()
        setupClickListeners()
        showRandomEcoTip()
    }
    
    private fun initViews() {
        tvAvgScore = findViewById(R.id.tv_avg_score)
        tvTotalDistance = findViewById(R.id.tv_total_distance)
        tvTotalDrives = findViewById(R.id.tv_total_drives)
        tvEcoTip = findViewById(R.id.tv_eco_tip)
        
        btnProfile = findViewById(R.id.btn_profile)
        cvDestinationSearch = findViewById(R.id.cv_destination_search)
        cvStatistics = findViewById(R.id.cv_statistics)
        cvEcoTips = findViewById(R.id.cv_eco_tips)
        cvSettings = findViewById(R.id.cv_settings)
    }
    
    private fun loadUserData() {
        val currentUser = FirebaseHelper.getCurrentUser()
        
        if (currentUser == null) {
            return
        }
        
        scope.launch {
            try {
                val result = FirebaseHelper.getUserDrivingRecords(currentUser.uid)
                
                result.onSuccess { records ->
                    if (records.isEmpty()) {
                        return@onSuccess
                    }
                    
                    withContext(Dispatchers.Main) {
                        val avgScore = records
                            .mapNotNull { it["eco_score"] as? Number }
                            .map { it.toInt() }
                            .average()
                            .toInt()
                        tvAvgScore.text = "${avgScore}점"
                        
                        val totalDistance = records
                            .mapNotNull { it["distance"] as? Number }
                            .sumOf { it.toDouble() }
                        tvTotalDistance.text = String.format("%.1f km", totalDistance)
                        
                        tvTotalDrives.text = "${records.size}회"
                    }
                }.onFailure { e ->
                    Log.e(tag, "주행 기록 로드 실패: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(tag, "주행 데이터 로드 중 오류 발생: ${e.message}")
            }
        }
    }
    
    private fun setupClickListeners() {
        btnProfile.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }
        
        cvDestinationSearch.setOnClickListener {
            val intent = Intent(this, DestinationSearchActivity::class.java)
            startActivity(intent)
        }
        
        cvStatistics.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
        
        cvEcoTips.setOnClickListener {
            showRandomEcoTip()
        }
        
        cvSettings.setOnClickListener {
        }
    }
    
    private fun showRandomEcoTip() {
        val randomTip = ecoTips.random()
        tvEcoTip.text = randomTip
    }
    
    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}
