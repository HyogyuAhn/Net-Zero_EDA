package insa.eda.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import insa.eda.R
import insa.eda.database.FirebaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserProfileActivity : AppCompatActivity() {
    
    private val tag = "UserProfileActivity"
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private lateinit var ivProfileImage: ImageView
    private lateinit var btnEditPhoto: ImageButton
    private lateinit var tvEmail: TextView
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etCarInfo: EditText
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSaveChanges: Button
    private lateinit var btnLogout: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        
        initViews()
        loadUserProfile()
        setupButtons()
    }
    
    private fun initViews() {
        ivProfileImage = findViewById(R.id.iv_profile_image)
        btnEditPhoto = findViewById(R.id.btn_edit_photo)
        tvEmail = findViewById(R.id.tv_email)
        etName = findViewById(R.id.et_name)
        etPhone = findViewById(R.id.et_phone)
        etCarInfo = findViewById(R.id.et_car_info)
        etCurrentPassword = findViewById(R.id.et_current_password)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSaveChanges = findViewById(R.id.btn_save_changes)
        btnLogout = findViewById(R.id.btn_logout)
    }
    
    private fun loadUserProfile() {
        val currentUser = FirebaseHelper.getCurrentUser()
        
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            redirectToLogin()
            return
        }
        
        tvEmail.text = currentUser.email
        
        scope.launch {
            try {
                val result = FirebaseHelper.getUserData(currentUser.uid)
                
                result.onSuccess { userData ->
                    if (userData != null) {
                        etName.setText(userData["name"] as? String ?: "")
                        etPhone.setText(userData["phone"] as? String ?: "")
                        etCarInfo.setText(userData["car_info"] as? String ?: "")
                        
                    }
                }.onFailure { e ->
                    Log.e(tag, "사용자 데이터 로드 실패: ${e.message}")
                    Toast.makeText(this@UserProfileActivity, "프로필 정보를 가져오는 데 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(tag, "사용자 데이터 로드 중 오류 발생: ${e.message}")
                Toast.makeText(this@UserProfileActivity, "프로필 정보 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupButtons() {
        btnEditPhoto.setOnClickListener {
            Toast.makeText(this, "프로필 사진 변경 기능은 아직 구현되지 않았습니다", Toast.LENGTH_SHORT).show()
        }
        
        btnSaveChanges.setOnClickListener {
            saveProfileChanges()
        }
        
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }
    
    private fun saveProfileChanges() {
        val currentUser = FirebaseHelper.getCurrentUser() ?: return
        
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val carInfo = etCarInfo.text.toString().trim()
        
        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        scope.launch {
            try {
                val userData = hashMapOf<String, Any>(
                    "name" to name,
                    "phone" to phone
                )
                
                if (carInfo.isNotEmpty()) {
                    userData["car_info"] = carInfo
                }
                
                withContext(Dispatchers.IO) {
                    FirebaseHelper.getFirestore()
                        .collection("users")
                        .document(currentUser.uid)
                        .update(userData)
                        .await()
                }
                
                if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
                    if (newPassword != confirmPassword) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserProfileActivity, "새 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    
                    if (newPassword.length < 6) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserProfileActivity, "비밀번호는 최소 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    
                    try {
                        val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                        withContext(Dispatchers.IO) {
                            currentUser.reauthenticate(credential).await()
                            currentUser.updatePassword(newPassword).await()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserProfileActivity, "현재 비밀번호가 올바르지 않습니다", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
                
                withContext(Dispatchers.Main) {
                    etCurrentPassword.text.clear()
                    etNewPassword.text.clear()
                    etConfirmPassword.text.clear()
                    
                    Toast.makeText(this@UserProfileActivity, "프로필이 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(tag, "프로필 업데이트 오류: ${e.message}")
                    Toast.makeText(this@UserProfileActivity, "프로필 업데이트 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃하시겠습니까?")
            .setPositiveButton("로그아웃") { _, _ -> logoutUser() }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun logoutUser() {
        FirebaseHelper.logout()
        redirectToLogin()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, insa.eda.LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
