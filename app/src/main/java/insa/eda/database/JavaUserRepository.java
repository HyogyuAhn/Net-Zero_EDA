package insa.eda.database;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import insa.eda.database.models.User;

public class JavaUserRepository {
    private static final String TAG = "JavaUserRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final Executor executor;
    private final Handler mainHandler;

    public JavaUserRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public void createUser(User user, String password, Callback<String> callback) {
        executor.execute(() -> {
            auth.createUserWithEmailAndPassword(user.getEmail(), password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            user.setUid(uid);

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", user.getName());
                            userData.put("email", user.getEmail());
                            userData.put("phone", user.getPhone());
                            userData.put("created_at", com.google.firebase.Timestamp.now());
                            
                            userData.put("total_drives", 0);
                            userData.put("total_distance", 0.0f);
                            userData.put("total_saved_co2", 0.0f);
                            userData.put("avg_eco_score", 80);

                            firestore.collection(FirebaseConfig.COLLECTION_USERS)
                                .document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    createInitialDrivingRecords(uid);
                                    notifySuccess(callback, uid);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore 사용자 데이터 저장 실패", e);
                                    notifyError(callback, e);
                                });
                        } else {
                            notifyError(callback, new Exception("사용자 생성 실패 (FirebaseUser is null)"));
                        }
                    } else {
                        notifyError(callback, task.getException() != null ?
                                task.getException() : new Exception("사용자 생성 실패"));
                    }
                });
        });
    }

    public void authenticateUser(String email, String password, Callback<User> callback) {
        executor.execute(() -> {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            firestore.collection(FirebaseConfig.COLLECTION_USERS)
                                .document(firebaseUser.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String uid = firebaseUser.getUid();
                                        String name = documentSnapshot.getString("name");
                                        String userEmail = documentSnapshot.getString("email");
                                        String phone = documentSnapshot.getString("phone");

                                        User user = new User();
                                        user.setUid(uid);
                                        user.setName(name);
                                        user.setEmail(userEmail);
                                        user.setPhone(phone);

                                        notifySuccess(callback, user);
                                    } else {
                                        notifyError(callback, new Exception("사용자 정보를 찾을 수 없습니다"));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    notifyError(callback, e);
                                });
                        } else {
                            notifyError(callback, new Exception("인증은 성공했지만 사용자 정보를 찾을 수 없습니다"));
                        }
                    } else {
                        notifyError(callback, task.getException() != null ? 
                                   task.getException() : new Exception("인증 실패"));
                    }
                });
        });
    }

    public void getUserByEmail(String email, Callback<User> callback) {
        executor.execute(() -> {
            firestore.collection(FirebaseConfig.COLLECTION_USERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String uid = documentSnapshot.getId();
                        String name = documentSnapshot.getString("name");
                        String userEmail = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");

                        User user = new User();
                        user.setUid(uid);
                        user.setName(name);
                        user.setEmail(userEmail);
                        user.setPhone(phone);

                        notifySuccess(callback, user);
                    } else {
                        notifySuccess(callback, null);
                    }
                })
                .addOnFailureListener(e -> {
                    notifyError(callback, e);
                });
        });
    }

    public void updateUser(User user, Callback<Boolean> callback) {
        executor.execute(() -> {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", user.getName());
                updates.put("phone", user.getPhone());
                
                firestore.collection(FirebaseConfig.COLLECTION_USERS)
                    .document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        notifySuccess(callback, true);
                    })
                    .addOnFailureListener(e -> {
                        notifyError(callback, e);
                    });
            } else {
                notifyError(callback, new Exception("사용자가 로그인되어 있지 않습니다"));
            }
        });
    }

    private <T> void notifySuccess(Callback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void notifyError(Callback<?> callback, Exception e) {
        mainHandler.post(() -> {
            callback.onError(e);
        });
    }
    
    private void createInitialDrivingRecords(String userId) {
        Calendar calendar = Calendar.getInstance();
        CollectionReference drivingCollection = firestore.collection(FirebaseConfig.COLLECTION_DRIVING_RECORDS);
        
        for (int i = 0; i < 5; i++) {
            int daysAgo = new Random().nextInt(3) + 1;
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
            
            Date startTime = calendar.getTime();
            
            int driveDurationMinutes = new Random().nextInt(41) + 20;
            calendar.add(Calendar.MINUTE, driveDurationMinutes);
            Date endTime = calendar.getTime();
            
            float distance = 5.0f + new Random().nextFloat() * 25.0f;
            
            float avgSpeed = (distance / (driveDurationMinutes / 60.0f));
            
            int ecoScore = new Random().nextInt(36) + 60;
            
            float co2Emission = distance * 0.147f;
            
            float fuelEfficiency = 8.0f + new Random().nextFloat() * 4.0f;
            
            String recordId = drivingCollection.document().getId();
            
            Map<String, Object> record = new HashMap<>();
            record.put("userId", userId);
            record.put("recordId", recordId);
            record.put("startTime", startTime);
            record.put("endTime", endTime);
            record.put("duration", driveDurationMinutes * 60);
            record.put("distance", distance);
            record.put("avgSpeed", avgSpeed);
            record.put("ecoScore", ecoScore);
            record.put("co2Emission", co2Emission);
            record.put("fuelEfficiency", fuelEfficiency);
            record.put("rapidAccelerations", new Random().nextInt(5));
            record.put("hardBrakings", new Random().nextInt(4));
            record.put("sharpTurns", new Random().nextInt(3));
            record.put("idlingMinutes", new Random().nextInt(6));
            record.put("createdAt", new Date());
            
            drivingCollection.document(recordId).set(record);
            
            calendar = Calendar.getInstance();
        }
    }
}
