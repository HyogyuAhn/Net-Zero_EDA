package insa.eda.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;

import insa.eda.R;
import insa.eda.database.JavaDrivingRecordRepository;
import insa.eda.services.AzureOpenAIService;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    private TextView welcomeText;
    private TextView ecoScoreText;
    private TextView co2SavedText;
    private TextView drivingTipText;
    private ProgressBar tipLoadingProgress;
    private MaterialCardView startDriveCard;
    
    private JavaDrivingRecordRepository drivingRepository;
    private AzureOpenAIService azureOpenAIService;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        welcomeText = view.findViewById(R.id.welcomeText);
        ecoScoreText = view.findViewById(R.id.ecoScoreText);
        co2SavedText = view.findViewById(R.id.co2SavedText);
        drivingTipText = view.findViewById(R.id.drivingTipText);
        tipLoadingProgress = view.findViewById(R.id.tipLoadingProgress);
        startDriveCard = view.findViewById(R.id.startDriveCard);
        
        drivingRepository = new JavaDrivingRecordRepository();
        azureOpenAIService = new AzureOpenAIService();
        
        loadUserId();
        
        startDriveCard.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.navigation_drive);
        });
        
        updateUserData();
        loadDrivingTip();
    }
    
    private void loadUserId() {
        SharedPreferences preferences = requireActivity().getSharedPreferences("EcoDriverPrefs", MODE_PRIVATE);
        userId = preferences.getString("userId", null);
        String userName = preferences.getString("userName", "User");

        if (userId != null && !userId.isEmpty()) {
            welcomeText.setText(getString(R.string.welcome_name, userName));
        } else {
            welcomeText.setText(getString(R.string.welcome_message));
        }
    }
    
    private void updateUserData() {
        ecoScoreText.setText(getString(R.string.default_eco_score_value));
        co2SavedText.setText(getString(R.string.default_co2_saved_value));
        
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        drivingRepository.getUserAverageEcoScore(userId, new JavaDrivingRecordRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer score) {
                if (isAdded() && getActivity() != null) {
                    if (score != null) {
                        ecoScoreText.setText(score.toString());
                    } else {
                        ecoScoreText.setText("0");
                    }
                }
            }
            
            @Override
            public void onError(Exception e) {
                if (isAdded() && getActivity() != null) {
                    Log.e("HomeFragment", "Error loading eco score", e);
                }
            }
        });
        
        drivingRepository.getUserTotalCO2Saved(userId, new JavaDrivingRecordRepository.Callback<Float>() {
            @Override
            public void onSuccess(Float co2Saved) {
                if (isAdded() && getActivity() != null) {
                    if (co2Saved != null) {
                        co2SavedText.setText(String.format("%.2f kg", co2Saved));
                    } else {
                        co2SavedText.setText("0.00 kg");
                    }
                }
            }
            
            @Override
            public void onError(Exception e) {
                if (isAdded() && getActivity() != null) {
                    Log.e("HomeFragment", "Error loading CO2 saved", e);
                }
            }
        });
    }
    
    private void loadDrivingTip() {
        // Show loading indicator
        tipLoadingProgress.setVisibility(View.VISIBLE);
        drivingTipText.setText("오늘의 운전 팁을 불러오는 중...");
        
        // Generate tip using Azure OpenAI
        azureOpenAIService.generateEcoDrivingTip(new AzureOpenAIService.TipCallback() {
            @Override
            public void onTipGenerated(String tip) {
                if (isAdded() && getActivity() != null) {
                    // Hide loading indicator and show tip
                    tipLoadingProgress.setVisibility(View.GONE);
                    drivingTipText.setText(tip);
                    
                    // Save tip to shared preferences for future use if needed
                    SharedPreferences preferences = requireActivity().getSharedPreferences("EcoDriverPrefs", MODE_PRIVATE);
                    preferences.edit().putString("lastDrivingTip", tip).apply();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (isAdded() && getActivity() != null) {
                    // Hide loading indicator and show fallback tip
                    tipLoadingProgress.setVisibility(View.GONE);
                    drivingTipText.setText("부드러운 가속과 적절한 감속으로 친환경 운전을 실천해보세요!");
                    Log.e("HomeFragment", "Error loading driving tip: " + errorMessage);
                }
            }
        });
    }
}
