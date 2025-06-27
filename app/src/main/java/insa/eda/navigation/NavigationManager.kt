package insa.eda.navigation

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.common.objects.KNError

class NavigationManager {
    companion object {
        private const val APP_KEY = "31d8cf5f175c769c9ab8ed569571621b"
        private const val TAG = "NavigationManager"

        fun authenticateNavigation(context: Context, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
            Log.d(TAG, "authenticateNavigation 시작")

            KNApplication.knsdk.initializeWithAppKey(
                aAppKey = APP_KEY,
                aClientVersion = "1.0.0",
                aUserKey = "EDA_User",
                aLangType = KNLanguageType.KNLanguageType_KOREAN
            ) { error ->
                Log.d(TAG, "인증 콜백 진입")

                if (error == null) {
                    Log.d(TAG, "인증 성공")
                    Toast.makeText(context, "네비게이션 인증 성공", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Log.e(TAG, "인증 실패 - code: ${error.code}, message: ${error}")
                    onFailure("인증 실패: ${error}")
                }
            }
        }

        fun startNavigation(context: Context) {
            val intent = Intent(context, NaviActivity::class.java)
            context.startActivity(intent)
        }

        fun authenticateAndStartNavigation(context: Context) {
            authenticateNavigation(
                context = context,
                onSuccess = { startNavigation(context) },
                onFailure = { errorMessage ->
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
