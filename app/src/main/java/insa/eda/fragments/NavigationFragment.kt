package insa.eda.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import insa.eda.R
import insa.eda.MainApplication
import insa.eda.navigation.NaviActivity
import com.kakaomobility.knsdk.KNLanguageType

class NavigationFragment : Fragment() {

    private val TAG = "NavigationFragment"
    private lateinit var btnStartNavigation: Button
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "모든 위치 권한 허용됨")
            initializeKakaoNavi()
        } else {
            Log.d(TAG, "일부 또는 모든 위치 권한이 거부됨")
            Toast.makeText(requireContext(), "네비게이션 기능을 사용하려면 위치 권한이 필요합니다", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_navigation, container, false)
        
        btnStartNavigation = view.findViewById(R.id.btn_start_navigation)
        btnStartNavigation.setOnClickListener {
            checkLocationPermissions()
        }
        
        return view
    }

    private fun checkLocationPermissions() {
        Log.d(TAG, "위치 권한 확인 시작")
        
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "위치 권한 이미 허용됨")
            initializeKakaoNavi()
        } else {
            Log.d(TAG, "위치 권한 요청 필요")
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initializeKakaoNavi() {
        Log.d(TAG, "카카오 네비 초기화 시작")
        
        MainApplication.knsdk.initializeWithAppKey(
            aAppKey = "31d8cf5f175c769c9ab8ed569571621b",
            aClientVersion = "1.0.0",
            aUserKey = "EDA_User",
            aLangType = KNLanguageType.KNLanguageType_KOREAN
        ) { error ->
            if (error == null) {
                Log.d(TAG, "카카오 네비 인증 성공")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "네비게이션 연결 성공", Toast.LENGTH_SHORT).show()
                    startNaviActivity()
                }
            } else {
                Log.e(TAG, "카카오 네비 인증 실패: ${error.toString()}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "네비게이션 연결 실패: ${error.toString()}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startNaviActivity() {
        Log.d(TAG, "네비게이션 액티비티 시작")
        
        val intent = Intent(requireContext(), NaviActivity::class.java)
        startActivity(intent)
    }

    companion object {
        fun newInstance() = NavigationFragment()
    }
}
