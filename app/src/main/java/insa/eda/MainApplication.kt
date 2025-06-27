package insa.eda

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.kakaomobility.knsdk.KNSDK

class MainApplication : Application() {
    
    companion object {
        lateinit var knsdk: KNSDK
        var searchManager: Any? = null
        private const val TAG = "MainApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        
        val auth = FirebaseAuth.getInstance()
        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        
        initializeKakaoNaviSDK()
    }

    private fun initializeKakaoNaviSDK() {
        Log.d(TAG, "카카오 네비게이션 SDK 초기화 시작")
        
        try {
            knsdk = KNSDK.apply {
                Log.d(TAG, "KNSDK.install() 호출 중 - filesDir = $filesDir")
                install(this@MainApplication, "$filesDir/KNSample")
                Log.d(TAG, "KNSDK.install() 호출 완료")
            }
            
            initializeSearchManager()
            
            if (searchManager == null) {
                Log.w(TAG, "모든 초기화 방법을 시도했지만 SearchManager가 여전히 null입니다")
                tryCreateSearchManagerDirectly()
            } else {
                Log.d(TAG, "카카오 네비게이션 SDK 초기화 성공")
            }
        } catch (e: Exception) {
            Log.e(TAG, "카카오 네비게이션 SDK 초기화 실패: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun initializeSearchManager() {
        try {
            val knsdkClass = knsdk.javaClass
            Log.d(TAG, "KNSDK 클래스: ${knsdkClass.name}")
            
            val searchManagerMethod = knsdkClass.methods.firstOrNull { method -> 
                method.name == "getSearchManager" || method.name == "searchManager"
            }
            
            if (searchManagerMethod != null) {
                Log.d(TAG, "SearchManager 메서드 발견: ${searchManagerMethod.name}")
                searchManager = searchManagerMethod.invoke(knsdk)
                Log.d(TAG, "SearchManager 초기화 성공 (메서드): $searchManager")
                return
            }
            
            try {
                val searchManagerField = knsdkClass.getDeclaredField("searchManager")
                searchManagerField.isAccessible = true
                searchManager = searchManagerField.get(knsdk)
                Log.d(TAG, "SearchManager 초기화 성공 (필드): $searchManager")
                return
            } catch (e: Exception) {
                Log.e(TAG, "SearchManager 필드 접근 실패: ${e.message}")
            }
            
            knsdkClass.declaredFields.forEach { field ->
                runCatching {
                    field.isAccessible = true
                    val value = field.get(knsdk)
                    if (value != null && field.type.name.contains("search", ignoreCase = true)) {
                        Log.d(TAG, "가능한 SearchManager 필드 발견: ${field.name}, 타입: ${field.type.name}")
                        searchManager = value
                        return
                    }
                }.onFailure {
                    Log.w(TAG, "SearchManager 필드 검색 중 실패: ${field.name}, 이유: ${it.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SearchManager 초기화 중 예외 발생: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun tryCreateSearchManagerDirectly() {
        try {
            val possibleClassNames = listOf(
                "com.kakaomobility.knsdk.search.KNSearchManager",
                "com.kakaomobility.knsdk.KNSearchManager",
                "com.kakaomobility.knsdk.search.SearchManager"  
            )
            
            for (className in possibleClassNames) {
                try {
                    val searchManagerClass = Class.forName(className)
                    Log.d(TAG, "SearchManager 클래스 발견: $className")
                    
                    val constructor = searchManagerClass.declaredConstructors.firstOrNull()
                    if (constructor != null) {
                        constructor.isAccessible = true
                        if (constructor.parameterCount == 0) {
                            searchManager = constructor.newInstance()
                        } else if (constructor.parameterCount == 1) {
                            searchManager = constructor.newInstance(knsdk)
                        }
                        Log.d(TAG, "SearchManager 직접 생성 성공: $searchManager")
                        return
                    }
                } catch (e: ClassNotFoundException) {
                    Log.d(TAG, "클래스 없음: $className")
                } catch (e: Exception) {
                    Log.e(TAG, "$className 클래스 인스턴스화 실패: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SearchManager 직접 생성 중 오류: ${e.message}")
            e.printStackTrace()
        }
    }
}
