package insa.eda.database;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import insa.eda.database.models.DrivingRecord;

public class JavaDrivingRecordRepository {
    private static final String TAG = "JavaDrivingRecordRepo";
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final Executor executor;
    private final Handler mainHandler;

    public JavaDrivingRecordRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public void startDrivingSession(String userId, Callback<String> callback) {
        executor.execute(() -> {
            try {
                Map<String, Object> drivingSession = new HashMap<>();
                Date startTime = new Date();
                
                drivingSession.put("user_id", userId);
                drivingSession.put("start_time", startTime);
                drivingSession.put("created_at", com.google.firebase.Timestamp.now());
                
                firestore.collection(FirebaseConfigJava.COLLECTION_DRIVING_RECORDS)
                    .add(drivingSession)
                    .addOnSuccessListener(documentReference -> {
                        String recordId = documentReference.getId();
                        notifySuccess(callback, recordId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "주행 세션 시작 실패", e);
                        notifyError(callback, e);
                    });
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    public void endDrivingSession(String recordId, DrivingRecord drivingRecord, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("end_time", drivingRecord.getEndTime());
                updates.put("duration", drivingRecord.getDuration());
                updates.put("distance", drivingRecord.getDistance());
                updates.put("avg_speed", drivingRecord.getAvgSpeed());
                updates.put("eco_score", drivingRecord.getEcoScore());
                updates.put("co2_saved", drivingRecord.getCo2Saved());
                
                firestore.collection(FirebaseConfigJava.COLLECTION_DRIVING_RECORDS)
                    .document(recordId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        notifySuccess(callback, true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "주행 세션 종료 실패", e);
                        notifyError(callback, e);
                    });
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    public void getUserDrivingHistory(String userId, int limit, Callback<List<DrivingRecord>> callback) {
        executor.execute(() -> {
            try {
                firestore.collection(FirebaseConfigJava.COLLECTION_DRIVING_RECORDS)
                    .whereEqualTo("user_id", userId)
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<DrivingRecord> drivingRecords = new ArrayList<>();
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            DrivingRecord record = new DrivingRecord();
                            String recordId = document.getId();
                            
                            record.setRecordId(recordId);
                            record.setUserId(userId);
                            
                            com.google.firebase.Timestamp startTimestamp = document.getTimestamp("start_time");
                            if (startTimestamp != null) {
                                record.setStartTime(startTimestamp.toDate());
                            }
                            
                            com.google.firebase.Timestamp endTimestamp = document.getTimestamp("end_time");
                            if (endTimestamp != null) {
                                record.setEndTime(endTimestamp.toDate());
                            }
                            
                            if (document.contains("duration")) {
                                record.setDuration(document.getLong("duration").intValue());
                            }
                            
                            if (document.contains("distance")) {
                                record.setDistance(document.getDouble("distance").floatValue());
                            }
                            
                            if (document.contains("avg_speed")) {
                                record.setAvgSpeed(document.getDouble("avg_speed").floatValue());
                            }
                            
                            if (document.contains("eco_score")) {
                                record.setEcoScore(document.getLong("eco_score").intValue());
                            }
                            
                            if (document.contains("co2_saved")) {
                                record.setCo2Saved(document.getDouble("co2_saved").floatValue());
                            }
                            
                            drivingRecords.add(record);
                        }
                        
                        notifySuccess(callback, drivingRecords);
                    })
                    .addOnFailureListener(e -> {
                        notifyError(callback, e);
                    });
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    public void getUserDrivingHistory(String userId, Callback<List<DrivingRecord>> callback) {
        getUserDrivingHistory(userId, 10, callback);
    }

    public void getUserAverageEcoScore(String userId, Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                firestore.collection(FirebaseConfigJava.COLLECTION_DRIVING_RECORDS)
                    .whereEqualTo("user_id", userId)
                    .whereGreaterThan("eco_score", 0)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            notifySuccess(callback, 0);
                            return;
                        }

                        int totalScore = 0;
                        int count = 0;

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            if (document.contains("eco_score")) {
                                totalScore += document.getLong("eco_score").intValue();
                                count++;
                            }
                        }

                        int averageScore = count > 0 ? totalScore / count : 0;
                        notifySuccess(callback, averageScore);
                    })
                    .addOnFailureListener(e -> {
                        notifyError(callback, e);
                    });
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    public void getUserTotalCO2Saved(String userId, Callback<Float> callback) {
        executor.execute(() -> {
            try {
                firestore.collection(FirebaseConfigJava.COLLECTION_DRIVING_RECORDS)
                    .whereEqualTo("user_id", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        float totalCO2Saved = 0;
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            if (document.contains("co2_saved")) {
                                totalCO2Saved += document.getDouble("co2_saved").floatValue();
                            }
                        }
                        
                        notifySuccess(callback, totalCO2Saved);
                    })
                    .addOnFailureListener(e -> {
                        notifyError(callback, e);
                    });
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    public void getDrivingRecordsByUserId(String userId, Callback<List<DrivingRecord>> callback) {
        getUserDrivingHistory(userId, callback);
    }

    private <T> void notifySuccess(Callback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void notifyError(Callback<T> callback, Exception e) {
        mainHandler.post(() -> callback.onError(e));
    }
}
