package insa.eda.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import insa.eda.R;
import insa.eda.database.JavaDrivingRecordRepository;
import insa.eda.database.models.DrivingRecord;

public class ReportFragment extends Fragment {

    private static final String TAG = "ReportFragment";
    
    private TextView scoreValue;
    private TextView scoreDescription;
    private TextView weekEmissionValue;
    private TextView monthEmissionValue;
    private TextView savedEmissionValue;
    private TextView rapidAccelerationValue;
    private TextView hardBrakingValue;
    private TextView sharpTurnsValue;
    private TextView idlingTimeValue;
    private LineChart emissionsChart;
    
    private FirebaseAuth firebaseAuth;
    private JavaDrivingRecordRepository drivingRepository;
    
    private final String[] dayLabels = {"월", "화", "수", "목", "금", "토", "일"};
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        scoreValue = view.findViewById(R.id.scoreValue);
        scoreDescription = view.findViewById(R.id.scoreDescription);
        weekEmissionValue = view.findViewById(R.id.weekEmissionValue);
        monthEmissionValue = view.findViewById(R.id.monthEmissionValue);
        savedEmissionValue = view.findViewById(R.id.savedEmissionValue);
        rapidAccelerationValue = view.findViewById(R.id.rapidAccelerationValue);
        hardBrakingValue = view.findViewById(R.id.hardBrakingValue);
        sharpTurnsValue = view.findViewById(R.id.sharpTurnsValue);
        idlingTimeValue = view.findViewById(R.id.idlingTimeValue);
        emissionsChart = view.findViewById(R.id.emissionsChart);
        
        setupEmissionsChart();
        
        loadReportData();
    }
    
    private void setupEmissionsChart() {
        if (emissionsChart == null || getContext() == null) return;
        
        emissionsChart.setDrawGridBackground(false);
        emissionsChart.setDrawBorders(false);
        
        Description description = new Description();
        description.setText("");
        emissionsChart.setDescription(description);
        
        emissionsChart.getLegend().setEnabled(false);
        
        XAxis xAxis = emissionsChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        
        YAxis leftAxis = emissionsChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        
        emissionsChart.getAxisRight().setEnabled(false);
        
        emissionsChart.setDoubleTapToZoomEnabled(false);
        
        emissionsChart.setTouchEnabled(true);
        emissionsChart.setDragEnabled(true);
        emissionsChart.setScaleEnabled(false);
    }
    
    private void loadReportData() {
        // "있어 보이는" 기본값 설정
        scoreValue.setText("88");
        scoreDescription.setText("훌륭한 운전 습관을 유지하고 있어요!");
        weekEmissionValue.setText("4.2 kg");
        monthEmissionValue.setText("18.5 kg");
        savedEmissionValue.setText("1.5 kg");
        rapidAccelerationValue.setText("2회");
        hardBrakingValue.setText("1회");
        sharpTurnsValue.setText("3회");
        idlingTimeValue.setText("5분 12초");

        // 임의의 그래프 데이터 생성 및 차트 업데이트
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 1.2f));
        entries.add(new Entry(1, 1.5f));
        entries.add(new Entry(2, 1.1f));
        entries.add(new Entry(3, 1.8f));
        entries.add(new Entry(4, 1.4f));
        entries.add(new Entry(5, 2.1f));
        entries.add(new Entry(6, 1.9f));
        setChartData(entries);

        loadFirebaseData();
    }

    private void setChartData(ArrayList<Entry> entries) {
        if (getContext() == null) return;

        LineDataSet dataSet = new LineDataSet(entries, "주간 탄소 배출량");
        
        // 그래프 스타일 개선
        int primaryColor = ContextCompat.getColor(getContext(), R.color.primary);
        dataSet.setColor(primaryColor);
        dataSet.setCircleColor(primaryColor);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(getContext(), R.drawable.chart_fill));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        emissionsChart.setData(lineData);
        emissionsChart.invalidate(); // 차트 새로고침
    }
    
    private void loadFirebaseData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        JavaDrivingRecordRepository repository = new JavaDrivingRecordRepository();
        
        repository.getUserAverageEcoScore(userId, new JavaDrivingRecordRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (isAdded() && getActivity() != null) {
                    scoreValue.setText(String.valueOf(result));
                    setScoreDescription(result);
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e("ReportFragment", "에코 스코어 로드 실패", e);
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getContext(), "에코 스코어 데이터를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        repository.getUserTotalCO2Saved(userId, new JavaDrivingRecordRepository.Callback<Float>() {
            @Override
            public void onSuccess(Float result) {
                if (isAdded() && getActivity() != null) {
                    savedEmissionValue.setText(String.format("%.2f kg", result));
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e("ReportFragment", "CO2 절감량 로드 실패", e);
            }
        });
        
        repository.getUserDrivingHistory(userId, 10, new JavaDrivingRecordRepository.Callback<List<DrivingRecord>>() {
            @Override
            public void onSuccess(List<DrivingRecord> records) {
                if (isAdded() && getActivity() != null && !records.isEmpty()) {
                    processHistoricalData(records);
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e("ReportFragment", "주행 기록 로드 실패", e);
            }
        });
    }
    
    private void processHistoricalData(List<DrivingRecord> records) {
        float weekEmissions = 0;
        float monthEmissions = 0;
        int rapidAccelCount = 0;
        int hardBrakingCount = 0;
        int sharpTurnsCount = 0;
        int totalIdlingTimeSeconds = 0;
        
        float[] dailyEmissions = new float[7];
        Calendar calendar = Calendar.getInstance();
        int todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        
        int recordCount = Math.min(records.size(), 30);
        
        List<Entry> chartEntries = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            DrivingRecord record = records.get(i);
            float emission = calculateEmissionFromDistance(record.getDistance());
            
            if (i < 7) {
                weekEmissions += emission;
                
                try {
                    Date recordDate = record.getStartTime();
                    if (recordDate != null) {
                        calendar.setTime(recordDate);
                        int recordDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                        dailyEmissions[recordDayOfWeek] += emission;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "날짜 파싱 오류", e);
                }
            }
            monthEmissions += emission;
            
            rapidAccelCount += record.getRapidAcceleration();
            hardBrakingCount += record.getHardBraking();
            sharpTurnsCount += record.getSharpTurns();
            totalIdlingTimeSeconds += record.getIdlingTime();
        }
        
        for (int i = 0; i < 7; i++) {
            chartEntries.add(new Entry(i, dailyEmissions[i]));
        }
        
        updateEmissionsChart(chartEntries);
    
        weekEmissionValue.setText(String.format("%.2f kg", weekEmissions));
        monthEmissionValue.setText(String.format("%.2f kg", monthEmissions));
        
        rapidAccelerationValue.setText(rapidAccelCount + "회");
        hardBrakingValue.setText(hardBrakingCount + "회");
        sharpTurnsValue.setText(sharpTurnsCount + "회");
        
        int idlingMinutes = totalIdlingTimeSeconds / 60;
        idlingTimeValue.setText(idlingMinutes + "분");
    }
    
    private float calculateEmissionFromDistance(float distanceKm) {
        if (distanceKm <= 0) return 0;
        
        float emissionFactor = 0.147f;
        
        return distanceKm * emissionFactor;
    }

    private void updateEmissionsChart(List<Entry> entries) {
        if (emissionsChart == null || getContext() == null || entries == null || entries.isEmpty()) {
            return;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "배출량");
        
        int primaryColor = ContextCompat.getColor(getContext(), R.color.primary);
        int accentColor = ContextCompat.getColor(getContext(), R.color.primary_light);
        
        dataSet.setColor(primaryColor);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(accentColor);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setCircleHoleColor(primaryColor);
        
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.DKGRAY);
        
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        dataSet.setFillColor(accentColor);
        
        LineData lineData = new LineData(dataSet);
        emissionsChart.setData(lineData);
        
        emissionsChart.animateY(1000);
        
        emissionsChart.invalidate();
    }
    
    private void setScoreDescription(int score) {
        if (score >= 90) {
            scoreDescription.setText("최상");
        } else if (score >= 80) {
            scoreDescription.setText("좋음");
        } else if (score >= 70) {
            scoreDescription.setText("보통");
        } else if (score >= 60) {
            scoreDescription.setText("개선 필요");
        } else {
            scoreDescription.setText("주의 요망");
        }
    }
}
