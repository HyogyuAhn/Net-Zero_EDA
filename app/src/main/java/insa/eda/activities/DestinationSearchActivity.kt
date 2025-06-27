package insa.eda.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.kakaomobility.knsdk.common.objects.KNError
import insa.eda.MainApplication
import insa.eda.R
import insa.eda.adapters.SearchResultAdapter
import java.util.Timer
import java.util.TimerTask

data class AddressInfo(
    val name: String,
    val address: String,
    val coordinate: Coordinate?
)

data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

class DestinationSearchActivity : AppCompatActivity() {
    private val tag = "DestinationSearch"
    
    private lateinit var etSearch: TextInputEditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var btnStartDriving: Button
    private lateinit var progressLoading: ProgressBar
    
    private var searchTimer: Timer? = null
    private var selectedDestination: AddressInfo? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_destination_search)
        
        initializeViews()
        setupSearchListener()
    }
    
    private fun initializeViews() {
        etSearch = findViewById(R.id.et_destination_search)
        rvSearchResults = findViewById(R.id.rv_search_results)
        btnStartDriving = findViewById(R.id.btn_start_driving)
        progressLoading = findViewById(R.id.progress_loading)
        
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        
        btnStartDriving.isEnabled = false
        btnStartDriving.setOnClickListener {
            startDrivingActivity()
        }
    }
    
    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchTimer?.cancel()
                
                if (s?.length ?: 0 < 2) return
                
                searchTimer = Timer()
                searchTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            performSearch(s.toString())
                        }
                    }
                }, 500)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun performSearch(query: String) {
        Log.d(tag, "주소 검색 시작: $query")
        progressLoading.visibility = View.VISIBLE
        
        try {
            val searchManagerObj = MainApplication.searchManager
            if (searchManagerObj == null) {
                Log.e(tag, "SearchManager가 초기화되지 않았습니다.")
                Toast.makeText(this, "검색 서비스가 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
                progressLoading.visibility = View.GONE
                return
            }
            
            try {
                val requestMethod = searchManagerObj.javaClass.getMethod(
                    "requestAddressSuggestion",
                    String::class.java,
                    Function2::class.java
                )
                
                requestMethod.invoke(
                    searchManagerObj,
                    query,
                    object : Function2<Any?, Any?, Unit> {
                        override fun invoke(error: Any?, results: Any?) {
                            runOnUiThread {
                                processSearchResults(error, results)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "검색 메서드 호출 실패: ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@DestinationSearchActivity, "검색 요청 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return
            }
        } catch (e: Exception) {
            Log.e(tag, "주소 검색 중 예외 발생: ${e.message}")
            progressLoading.visibility = View.GONE
            Toast.makeText(this, "검색 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun processSearchResults(error: Any?, results: Any?) {
        progressLoading.visibility = View.GONE
        
        try {
            val resultsList = if (results != null) {
                val sizeMethod = results.javaClass.getMethod("size")
                val getMethod = results.javaClass.getMethod("get", Int::class.java)
                
                val resultSize = sizeMethod.invoke(results) as Int
                Log.d(tag, "주소 검색 결과: ${resultSize}개")
                
                val addressList = mutableListOf<AddressInfo>()
                
                for (i in 0 until resultSize) {
                    try {
                        val item = getMethod.invoke(results, i)
                        
                        val nameField = item.javaClass.getMethod("getName")
                        val addressField = item.javaClass.getMethod("getAddress")
                        val coordField = item.javaClass.getMethod("getCoordinate")
                        
                        val name = nameField.invoke(item)?.toString() ?: ""
                        val address = addressField.invoke(item)?.toString() ?: ""
                        val coordObj = coordField.invoke(item)
                        
                        var coordinate: Coordinate? = null
                        if (coordObj != null) {
                            try {
                                val latMethod = coordObj.javaClass.getMethod("getLatitude")
                                val lngMethod = coordObj.javaClass.getMethod("getLongitude")
                                
                                val latitude = latMethod.invoke(coordObj) as? Double ?: 0.0
                                val longitude = lngMethod.invoke(coordObj) as? Double ?: 0.0
                                
                                coordinate = Coordinate(latitude, longitude)
                            } catch (e: Exception) {
                                Log.e(tag, "좌표 데이터 추출 오류: ${e.message}")
                            }
                        }
                        
                        addressList.add(AddressInfo(name, address, coordinate))
                        Log.d(tag, "추출된 주소: $name, $address")
                    } catch (e: Exception) {
                        Log.e(tag, "항목 $i 추출 중 오류: ${e.message}")
                    }
                }
                
                addressList
            } else {
                mutableListOf()
            }
            
            val adapter = SearchResultAdapter(resultsList) { addressInfo ->
                selectedDestination = addressInfo
                btnStartDriving.isEnabled = true
                Log.d(tag, "선택된 주소: ${addressInfo.name}, ${addressInfo.address}")
            }
            rvSearchResults.adapter = adapter
            
            if (resultsList.isEmpty() && error != null) {
                val errorMsg = if (error is Exception) error.message else error.toString()
                Log.e(tag, "검색 오류 발생: $errorMsg")
                Toast.makeText(this, "검색 실패: $errorMsg", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(tag, "검색 결과 처리 오류: ${e.message}")
            Toast.makeText(this, "검색 결과를 처리하는 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startDrivingActivity() {
        selectedDestination?.let {
            val intent = Intent(this, DrivingActivity::class.java).apply {
                putExtra("DESTINATION_NAME", it.name)
                putExtra("DESTINATION_ADDRESS", it.address)
                putExtra("DESTINATION_LAT", it.coordinate?.latitude ?: 0.0)
                putExtra("DESTINATION_LON", it.coordinate?.longitude ?: 0.0)
            }
            startActivity(intent)
        }
    }
}
