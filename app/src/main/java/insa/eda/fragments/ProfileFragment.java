package insa.eda.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import insa.eda.LoginActivity;
import insa.eda.R;
import insa.eda.database.JavaDrivingRecordRepository;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImageView;
    private TextView userNameText;
    private TextView userEmailText;
    private TextView totalDrivesText;
    private TextView totalDistanceText;
    private TextView totalCO2SavedText;
    private TextView avgEcoScoreText;
    private MaterialCardView achievementCard;
    private Button logoutButton;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private JavaDrivingRecordRepository drivingRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        profileImageView = view.findViewById(R.id.profileImageView);
        userNameText = view.findViewById(R.id.userNameText);
        userEmailText = view.findViewById(R.id.userEmailText);
        totalDrivesText = view.findViewById(R.id.totalDrivesValue);
        totalDistanceText = view.findViewById(R.id.totalDistanceValue);
        totalCO2SavedText = view.findViewById(R.id.totalCO2SavedValue);
        avgEcoScoreText = view.findViewById(R.id.avgEcoScoreValue);
        achievementCard = view.findViewById(R.id.achievementCard);
        logoutButton = view.findViewById(R.id.logoutButton);
        
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        drivingRepository = new JavaDrivingRecordRepository();
        
        logoutButton.setOnClickListener(v -> logoutUser());
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserProfile();
        loadUserStatistics();
    }
    
    private void loadUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            
            if (displayName == null || displayName.isEmpty()) {
                displayName = email != null ? email.split("@")[0] : "사용자";
            }
            
            userNameText.setText(displayName);
            userEmailText.setText(email != null ? email : "이메일 정보 없음");
            
            if (getContext() != null) {
                if (currentUser.getPhotoUrl() != null) {
                    Glide.with(getContext())
                            .load(currentUser.getPhotoUrl())
                            .apply(new RequestOptions()
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder))
                            .into(profileImageView);
                } else {
                    Glide.with(getContext())
                            .load(R.drawable.ic_profile_placeholder)
                            .into(profileImageView);
                }
            }
        } else {
            userNameText.setText("로그인이 필요합니다");
            userEmailText.setText("");
            
            if (getContext() != null) {
                Glide.with(getContext())
                        .load(R.drawable.ic_profile_placeholder)
                        .into(profileImageView);
            }
        }
    }
    
    private void loadUserStatistics() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        
        drivingRepository.getUserDrivingHistory(userId, 100, new JavaDrivingRecordRepository.Callback<java.util.List<insa.eda.database.models.DrivingRecord>>() {
            @Override
            public void onSuccess(java.util.List<insa.eda.database.models.DrivingRecord> records) {
                if (isAdded() && getActivity() != null) {
                    updateStatistics(records);
                }
            }
            
            @Override
            public void onError(Exception e) {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getContext(), "통계 정보를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        drivingRepository.getUserAverageEcoScore(userId, new JavaDrivingRecordRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (isAdded() && getActivity() != null) {
                    avgEcoScoreText.setText(String.valueOf(result));
                }
            }
            
            @Override
            public void onError(Exception e) {
            }
        });
        
        drivingRepository.getUserTotalCO2Saved(userId, new JavaDrivingRecordRepository.Callback<Float>() {
            @Override
            public void onSuccess(Float result) {
                if (isAdded() && getActivity() != null) {
                    totalCO2SavedText.setText(String.format(Locale.getDefault(), "%.2f kg", result));
                }
            }
            
            @Override
            public void onError(Exception e) {
            }
        });
    }
    
    private void updateStatistics(java.util.List<insa.eda.database.models.DrivingRecord> records) {
        if (records == null) return;
        
        int totalDrives = records.size();
        float totalDistance = 0;
        
        for (insa.eda.database.models.DrivingRecord record : records) {
            totalDistance += record.getDistance();
        }
        
        totalDrivesText.setText(String.valueOf(totalDrives));
        totalDistanceText.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
    }
    
    private void logoutUser() {
        new AlertDialog.Builder(requireContext())
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("예", (dialog, which) -> {
                SharedPreferences preferences = requireActivity().getSharedPreferences("EcoDriverPrefs", android.content.Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isLoggedIn", false); 
                editor.apply();
                
                firebaseAuth.signOut();
                
                WebView webView = new WebView(requireContext());
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
                webView.clearCache(true);
                webView.clearHistory();
                
                Toast.makeText(getContext(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
                
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                
                if (getActivity() != null) {
                    getActivity().finishAffinity();
                }
            })
            .setNegativeButton("아니오", null)
            .show();
    }
}
