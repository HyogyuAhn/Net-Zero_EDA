package insa.eda.navigation

import android.app.Application
import com.kakaomobility.knsdk.KNSDK
import android.util.Log

class KNApplication : Application() {
    companion object {
        lateinit var knsdk: KNSDK
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("KNApplication", "onCreate() 실행됨")
        initialize()
    }

    private fun initialize() {
        Log.d("KNApplication", "initialize() 진입 - KNSDK install 호출 시도")

        knsdk = KNSDK.apply {
            Log.d("KNApplication", "KNSDK.install() 호출 중 - filesDir = $filesDir")
            install(this@KNApplication, "$filesDir/KNSample")
            Log.d("KNApplication", "KNSDK.install() 호출 완료")
        }

        Log.d("KNApplication", "initialize() 종료")
    }
}
