package insa.eda.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import insa.eda.R;
import insa.eda.adapters.DrivingHistoryAdapter;
import insa.eda.database.JavaDrivingRecordRepository;
import insa.eda.database.models.DrivingRecord;

import static android.content.Context.MODE_PRIVATE;

public class HistoryFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private TabLayout tabLayout;
    private MaterialCardView emptyStateCard;
    private MaterialButton startDrivingBtn;
    private DrivingHistoryAdapter adapter;
    
    private JavaDrivingRecordRepository drivingRepository;
    private String userId = null;
    private boolean hasHistory = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        tabLayout = view.findViewById(R.id.tabLayout);
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        startDrivingBtn = view.findViewById(R.id.startDrivingBtn);
        
        drivingRepository = new JavaDrivingRecordRepository();
        
        loadUserId();
        
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DrivingHistoryAdapter(record -> {
            Toast.makeText(requireContext(), "Drive ID: " + record.getId(), Toast.LENGTH_SHORT).show();
        });
        historyRecyclerView.setAdapter(adapter);
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadHistoryData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadHistoryData(tab.getPosition());
            }
        });
        
        startDrivingBtn.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.navigation_drive);
        });
        
        loadHistoryData(0);
    }
    
    private void loadUserId() {
        SharedPreferences preferences = requireActivity().getSharedPreferences("EcoDriverPrefs", MODE_PRIVATE);
        userId = preferences.getString("userId", null);
    }
    
    private void loadHistoryData(int tabPosition) {
        if (userId == null || userId.isEmpty()) {
            updateUI(false);
            return;
        }
        
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        
        switch (tabPosition) {
            case 0:
                break;
            case 1:
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case 2:
                calendar.add(Calendar.MONTH, -1);
                break;
        }
        
        long startTime = calendar.getTimeInMillis();
        
        drivingRepository.getDrivingRecordsByUserId(userId, new JavaDrivingRecordRepository.Callback<List<DrivingRecord>>() {
            @Override
            public void onSuccess(List<DrivingRecord> records) {
                if (isAdded() && getActivity() != null) {
                    List<DrivingRecord> filteredRecords = new ArrayList<>();
                    
                    if (tabPosition == 0) {
                        filteredRecords = records;
                    } else {
                        for (DrivingRecord record : records) {
                            if (record.getStartTime().getTime() >= startTime && 
                                record.getStartTime().getTime() <= endTime) {
                                filteredRecords.add(record);
                            }
                        }
                    }
                    
                    adapter.setDrivingRecords(filteredRecords);
                    updateUI(!filteredRecords.isEmpty());
                }
            }
            
            @Override
            public void onError(Exception e) {
                if (isAdded() && getActivity() != null) {
                    Log.e("HistoryFragment", "Error loading driving records", e);
                    updateUI(false);
                }
            }
        });
    }
    
    private void updateUI(boolean hasHistory) {
        this.hasHistory = hasHistory;
        
        if (hasHistory) {
            historyRecyclerView.setVisibility(View.VISIBLE);
            emptyStateCard.setVisibility(View.GONE);
        } else {
            historyRecyclerView.setVisibility(View.GONE);
            emptyStateCard.setVisibility(View.VISIBLE);
        }
    }
}
