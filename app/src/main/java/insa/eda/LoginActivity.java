package insa.eda;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import insa.eda.utils.SecurityUtils;


import insa.eda.database.FirebaseHelper;
import insa.eda.database.JavaUserRepository;
import insa.eda.database.models.User;


public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView registerLink;
    private TextView forgotPasswordText;

    private JavaUserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initializeDatabase();
        
        if (isUserLoggedIn()) {
            startMainActivity();
            return;
        }
        
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        

        userRepository = new JavaUserRepository();
        
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
        
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
        
        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, getString(R.string.password_recovery_not_implemented), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean isUserLoggedIn() {
        SharedPreferences preferences = getSharedPreferences("EcoDriverPrefs", MODE_PRIVATE);
        return preferences.getBoolean("isLoggedIn", false);
    }
    
    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, ActivityHome.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void attemptLogin() {
        emailInput.setError(null);
        passwordInput.setError(null);
        
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        
        boolean cancel = false;
        View focusView = null;
        
        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.error_email_required));
            focusView = emailInput;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailInput.setError(getString(R.string.error_invalid_email));
            focusView = emailInput;
            cancel = true;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError(getString(R.string.error_password_required));
            focusView = passwordInput;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            passwordInput.setError(getString(R.string.error_password_too_short));
            focusView = passwordInput;
            cancel = true;
        }
        
        if (cancel) {
            focusView.requestFocus();
        } else {
            loginButton.setEnabled(false);
            loginButton.setText(getString(R.string.logging_in));
            
            performLogin(email, password);
        }
    }
    
    private void performLogin(String email, String password) {
        tryFirebaseAuthentication(email, password);
    }
    
    private void tryFirebaseAuthentication(String email, String password) {
        userRepository.authenticateUser(email, password, new JavaUserRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                loginButton.setEnabled(true);
                loginButton.setText(getString(R.string.login));
                if (user != null) {
                    saveLocalUserData(user);
                    startMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                loginButton.setEnabled(true);
                loginButton.setText(getString(R.string.login));
                Toast.makeText(LoginActivity.this, getString(R.string.login_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    

    

    
    private void saveLocalUserData(User user) {
        SharedPreferences preferences = getSharedPreferences("EcoDriverPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userUid", user.getUid());
        editor.putString("userName", user.getName());
        editor.putString("userEmail", user.getEmail());
        editor.apply();
    }
    

    
    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }
    
    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    private void initializeDatabase() {
        FirebaseHelper.init(getApplicationContext());
        FirebaseHelper.initializeFirebaseFromJava();
    }
}
