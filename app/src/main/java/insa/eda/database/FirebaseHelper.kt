package insa.eda.database

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

object FirebaseHelper {
    private const val TAG = "FirebaseHelper"
    private var context: Context? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    
    fun getFirestore(): FirebaseFirestore {
        return firestore
    }
    
    @JvmStatic
    fun init(appContext: Context) {
        context = appContext
        try {
            FirebaseApp.initializeApp(appContext)
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            storage = FirebaseStorage.getInstance()
            Log.d(TAG, "Firebase 초기화 성공")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 초기화 실패: ${e.message}", e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("로그인 실패: 사용자 정보를 찾을 수 없습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그인 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun registerUser(name: String, email: String, phone: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "created_at" to com.google.firebase.Timestamp.now()
                )
                
                firestore.collection(FirebaseConfig.COLLECTION_USERS)
                    .document(user.uid)
                    .set(userData)
                    .await()
                
                Result.success(user)
            } else {
                Result.failure(Exception("사용자 등록 실패"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "사용자 등록 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUserData(userId: String): Result<Map<String, Any>?> = withContext(Dispatchers.IO) {
        try {
            val documentSnapshot = firestore.collection(FirebaseConfig.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
                
            if (documentSnapshot.exists()) {
                Result.success(documentSnapshot.data)
            } else {
                Result.failure(Exception("사용자 정보가 없습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "사용자 정보 조회 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun saveDrivingRecord(
        userId: String,
        startTime: Date,
        endTime: Date? = null,
        duration: Int = 0,
        distance: Float = 0f,
        avgSpeed: Float = 0f,
        ecoScore: Int = 0,
        co2Saved: Float = 0f
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drivingData = hashMapOf(
                "user_id" to userId,
                "start_time" to startTime,
                "end_time" to endTime,
                "duration" to duration,
                "distance" to distance,
                "avg_speed" to avgSpeed,
                "eco_score" to ecoScore,
                "co2_saved" to co2Saved,
                "created_at" to com.google.firebase.Timestamp.now()
            )
            
            val documentReference = firestore.collection(FirebaseConfig.COLLECTION_DRIVING_RECORDS)
                .add(drivingData)
                .await()
                
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Log.e(TAG, "주행 기록 저장 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateDrivingRecord(
        recordId: String,
        updateData: Map<String, Any>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(FirebaseConfig.COLLECTION_DRIVING_RECORDS)
                .document(recordId)
                .set(updateData, SetOptions.merge())
                .await()
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "주행 기록 업데이트 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUserDrivingRecords(userId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = firestore.collection(FirebaseConfig.COLLECTION_DRIVING_RECORDS)
                .whereEqualTo("user_id", userId)
                .orderBy("created_at")
                .get()
                .await()
                
            val records = querySnapshot.documents.mapNotNull { it.data }
            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "주행 기록 조회 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteDrivingRecord(recordId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(FirebaseConfig.COLLECTION_DRIVING_RECORDS)
                .document(recordId)
                .delete()
                .await()
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "주행 기록 삭제 중 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    @JvmStatic
    fun initializeFirebaseFromJava() {
        if (context != null) {
            try {
                FirebaseApp.initializeApp(context!!)
                firestore = FirebaseFirestore.getInstance()
                auth = FirebaseAuth.getInstance()
                storage = FirebaseStorage.getInstance()
                Log.d(TAG, "Firebase 초기화 성공")
            } catch (e: Exception) {
                Log.e(TAG, "Firebase 초기화 실패: ${e.message}", e)
            }
        } else {
            Log.e(TAG, "Firebase 초기화 실패: 컨텍스트가 null입니다")
        }
    }
    
    fun logout() {
        auth.signOut()
    }
}
