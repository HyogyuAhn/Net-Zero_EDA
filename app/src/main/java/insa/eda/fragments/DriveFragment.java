package insa.eda.fragments;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Random;

import insa.eda.R;
import insa.eda.activities.DestinationSearchActivity;
import insa.eda.database.JavaDrivingRecordRepository;
import insa.eda.database.models.DrivingRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Date;

public class DriveFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "DriveFragment";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location currentLocation;

    private View preDriveLayout;
    private View drivingToolbar;
    private View drivingMetricsCard;
    private TextView speedText, fuelEfficiencyText, co2EmissionText, ecoScoreText;
    private ExtendedFloatingActionButton startDriveButton;

    private boolean isDriving = false;
    private boolean areControlsVisible = true;
    private final Handler handler = new Handler();
    private final Random random = new Random();
        private Runnable drivingDataUpdater;

    // Driving Session Data
    private JavaDrivingRecordRepository drivingRecordRepository;
    private String currentRecordId;
    private long startTimeMillis;
    private double totalDistance = 0.0;
    private double totalCo2Saved = 0.0;
    private int ecoScoreSum = 0;
    private int metricUpdates = 0;
    private Location lastLocation;
    
    // TTS and Notification components
    private TextToSpeech textToSpeech;
    private static final String CHANNEL_ID = "EDA_driving_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drive, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preDriveLayout = view.findViewById(R.id.preDriveLayout);
        drivingToolbar = view.findViewById(R.id.drivingToolbar);
        drivingMetricsCard = view.findViewById(R.id.drivingMetricsCard);
        speedText = view.findViewById(R.id.speedText);
        fuelEfficiencyText = view.findViewById(R.id.fuelEfficiencyText);
        co2EmissionText = view.findViewById(R.id.co2EmissionText);
        ecoScoreText = view.findViewById(R.id.ecoScoreText);
        startDriveButton = view.findViewById(R.id.startDriveButton);
        
        createLocationRequest();

        setupToolbar();
        initTextToSpeech();
        createNotificationChannel();

        if (hasPermissions()) {
            initMap();
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }

                drivingRecordRepository = new JavaDrivingRecordRepository();
        startDriveButton.setOnClickListener(v -> startDriving());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        createLocationCallback();
        createDrivingDataUpdater();
    }

    private void setupToolbar() {
        ((com.google.android.material.appbar.MaterialToolbar) drivingToolbar).setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_stop_driving) {
                stopDriving();
                return true;
            }
            return false;
        });
        ((com.google.android.material.appbar.MaterialToolbar) drivingToolbar).inflateMenu(R.menu.drive_menu);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        LatLng seoul = new LatLng(37.5665, 126.9780);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 15));

        mMap.setOnMapClickListener(latLng -> toggleDrivingControls());
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLocations().isEmpty()) return;
                currentLocation = locationResult.getLastLocation();
                if (mMap != null && currentLocation != null) {
                    LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
                }
            }
        };
    }
    
    private void createLocationRequest() {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(1000);
    }

    private void createDrivingDataUpdater() {
        drivingDataUpdater = new Runnable() {
            @Override
            public void run() {
                if (isDriving) {
                    updateDrivingMetrics();
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void startDriving() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "주행을 기록하려면 로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset metrics for new session
        totalDistance = 0.0;
        totalCo2Saved = 0.0;
        ecoScoreSum = 0;
        metricUpdates = 0;
        lastLocation = null;
        currentRecordId = null;
        startTimeMillis = System.currentTimeMillis();
        isDriving = true;

        drivingRecordRepository.startDrivingSession(currentUser.getUid(), new JavaDrivingRecordRepository.Callback<String>() {
            @Override
            public void onSuccess(String recordId) {
                currentRecordId = recordId;
                Log.d(TAG, "주행 세션 시작, ID: " + recordId);

                setDrivingUi(true);
                startLocationUpdates();
                handler.post(drivingDataUpdater);

                speakDrivingStartMessage();
                showDrivingStartNotification();
                Toast.makeText(requireContext(), "주행이 시작되었습니다. 안전운전 하세요!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "주행 세션 시작 실패", e);
                Toast.makeText(requireContext(), "주행 기록 시작에 실패했습니다.", Toast.LENGTH_SHORT).show();
                isDriving = false; // Revert state
                setDrivingUi(false);
            }
        });
    }
    
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }
    
    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
    
    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private void toggleDrivingControls() {
        areControlsVisible = !areControlsVisible;
        drivingToolbar.setVisibility(areControlsVisible ? View.VISIBLE : View.GONE);
        drivingMetricsCard.setVisibility(areControlsVisible ? View.VISIBLE : View.GONE);
    }

    private void stopDriving() {
        isDriving = false;
        stopLocationUpdates();
        handler.removeCallbacks(drivingDataUpdater);

        NotificationManager notificationManager = (NotificationManager) requireContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        if (textToSpeech != null) {
            textToSpeech.speak("주행이 종료되었습니다. 수고하셨습니다.", TextToSpeech.QUEUE_FLUSH, null, "driving_end");
        }

        if (currentRecordId == null) {
            Log.w(TAG, "주행 기록 ID가 없어 저장할 수 없습니다.");
            setDrivingUi(false);
            return;
        }

        long endTimeMillis = System.currentTimeMillis();
        long durationSeconds = (endTimeMillis - startTimeMillis) / 1000;
        
        double avgSpeed = 0;
        if (durationSeconds > 0) {
            avgSpeed = (totalDistance / durationSeconds) * 3.6; // km/h
        }
        if (Double.isNaN(avgSpeed) || Double.isInfinite(avgSpeed)) {
            avgSpeed = 0;
        }
        
        int avgEcoScore = (metricUpdates > 0) ? (ecoScoreSum / metricUpdates) : 0;

        DrivingRecord record = new DrivingRecord();
        record.setEndTime(new Date(endTimeMillis));
        record.setDuration((int) durationSeconds);
        record.setDistance((float) (totalDistance / 1000.0)); // km
        record.setAvgSpeed((float) avgSpeed);
        record.setEcoScore(avgEcoScore);
        record.setCo2Saved((float) totalCo2Saved);

        drivingRecordRepository.endDrivingSession(currentRecordId, record, new JavaDrivingRecordRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d(TAG, "주행 기록 저장 성공");
                Toast.makeText(requireContext(), "주행 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                navigateToReport();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "주행 기록 저장 실패", e);
                Toast.makeText(requireContext(), "주행 기록 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        
        setDrivingUi(false);
        currentRecordId = null;
    }

    private void setDrivingUi(boolean isDriving) {
        if (isDriving) {
            preDriveLayout.setVisibility(View.GONE);
            drivingToolbar.setVisibility(View.VISIBLE);
            drivingMetricsCard.setVisibility(View.VISIBLE);
            areControlsVisible = true;
        } else {
            preDriveLayout.setVisibility(View.VISIBLE);
            drivingToolbar.setVisibility(View.GONE);
            drivingMetricsCard.setVisibility(View.GONE);
            speedText.setText(getString(R.string.zero_speed_value));
            fuelEfficiencyText.setText(getString(R.string.no_value));
            co2EmissionText.setText(getString(R.string.no_value));
            ecoScoreText.setText(getString(R.string.no_value));
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception in updateLocationUI: " + e.getMessage());
        }
    }

    private void updateDrivingMetrics() {
        if (currentLocation != null) {
            if (lastLocation != null) {
                totalDistance += currentLocation.distanceTo(lastLocation); // meters
            }
            lastLocation = currentLocation;
        }

        float speedKmh = 0;
        if (currentLocation != null && currentLocation.hasSpeed()) {
            speedKmh = currentLocation.getSpeed() * 3.6f;
        }

        float fuelEfficiency = 10.0f + (random.nextFloat() * 10.0f);
        // This is a placeholder calculation. A real app would use a formula based on speed, acceleration, etc.
        float co2SavedPerUpdate = 0.01f + (random.nextFloat() * 0.05f); 
        int ecoScore = 70 + random.nextInt(30);
        
        totalCo2Saved += co2SavedPerUpdate;
        ecoScoreSum += ecoScore;
        metricUpdates++;

        speedText.setText(String.format("%.0f", speedKmh));
        fuelEfficiencyText.setText(String.format("%.1f", fuelEfficiency));
        co2EmissionText.setText(String.format("%.2f", totalCo2Saved));
        ecoScoreText.setText(String.valueOf(ecoScore));
    }

    private void updateUI() {
        if (!isDriving) {
            speedText.setText(getString(R.string.zero_speed_value));
            fuelEfficiencyText.setText(getString(R.string.no_value));
            co2EmissionText.setText(getString(R.string.no_value));
            ecoScoreText.setText(getString(R.string.no_value));
        }
    }

    private void navigateToReport() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_report);
    }

    private void navigateToDestinationSearch() {
        Intent intent = new Intent(getActivity(), DestinationSearchActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isDriving) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(drivingDataUpdater);
        
        // Shutdown TTS
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
    
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new java.util.Locale("ko", "KR"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Korean language not supported by TTS, using default");
                }
                textToSpeech.setSpeechRate(0.9f);
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: " + status);
            }
        });
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "EDA 주행 알림",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("에코드라이빙 앱 주행 상태 알림");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void speakDrivingStartMessage() {
        if (textToSpeech != null) {
            handler.postDelayed(() -> {
                textToSpeech.speak(
                        "주행 기록을 시작합니다. 안전 운전하세요.", 
                        TextToSpeech.QUEUE_FLUSH, 
                        null, 
                        "driving_start");
            }, 500);
        }
    }
    
    private void showDrivingStartNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("EDA 주행 중")
                .setContentText("에코 드라이빙 기록 중입니다. 안전운전하세요!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(false)
                .setOngoing(true);
                
        NotificationManager notificationManager = (NotificationManager) requireContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
