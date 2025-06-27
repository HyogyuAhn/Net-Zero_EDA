package insa.eda;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import insa.eda.utils.SecurityUtils;
import insa.eda.database.JavaUserRepository;
import insa.eda.database.models.User;


public class RegisterActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton registerButton;
    private TextView loginLink;

    private JavaUserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        backButton = findViewById(R.id.backButton);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
        
        userRepository = new JavaUserRepository();
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegistration();
            }
        });
        
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void attemptRegistration() {
        nameInput.setError(null);
        emailInput.setError(null);
        phoneInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);
        
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        
        boolean cancel = false;
        View focusView = null;
        
        if (TextUtils.isEmpty(name)) {
            nameInput.setError(getString(R.string.error_name_required));
            focusView = nameInput;
            cancel = true;
        }
        
        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.error_email_required));
            focusView = emailInput;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailInput.setError(getString(R.string.error_invalid_email));
            focusView = emailInput;
            cancel = true;
        }
        
        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError(getString(R.string.error_phone_required));
            focusView = phoneInput;
            cancel = true;
        } else if (!isPhoneValid(phone)) {
            phoneInput.setError(getString(R.string.error_invalid_phone));
            focusView = phoneInput;
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
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.error_passwords_not_match));
            focusView = confirmPasswordInput;
            cancel = true;
        }
        
        if (cancel) {
            focusView.requestFocus();
        } else {
            registerButton.setEnabled(false);
            registerButton.setText(getString(R.string.registering));
            performRegistration(name, email, phone, password);
        }
    }
    
    private void performRegistration(String name, String email, String phone, String password) {
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPhone(phone);

        userRepository.createUser(newUser, password, new JavaUserRepository.Callback<String>() {
            @Override
            public void onSuccess(String userId) {
                registerButton.setEnabled(true);
                registerButton.setText(getString(R.string.register));
                if (userId != null && !userId.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.registration_successful), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                registerButton.setEnabled(true);
                registerButton.setText(getString(R.string.register));
                if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.error_user_exists), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    

    
    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }
    
    private boolean isPhoneValid(String phone) {
        return phone.length() >= 10;
    }
    
    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }
}
