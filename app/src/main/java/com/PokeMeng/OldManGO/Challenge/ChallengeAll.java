package com.PokeMeng.OldManGO.Challenge;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.TaskManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChallengeAll extends AppCompatActivity implements SensorEventListener{
    String TAG = "計步器";
    int mSteps;
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> { });
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    TaskManager taskManager;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    int currentActivityGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.challenge_all);
        setupWindowInsets();
        getNowStep();
        checkAndLoadActivities(); // 新增的函式调用
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        taskManager = new TaskManager(FirebaseFirestore.getInstance(), currentUser.getUid());
        taskManager.checkAndCompleteTask("CheckChallenge", result -> {
            if (!result) {
                taskManager.updateTaskStatusForSteps(6);
                taskManager.markTaskAsCompleted("CheckChallenge");
            }
        });
    }
    private void checkAndLoadActivities() {
        db.collection("Activities").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                List<Activity> activities = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Activity activity = document.toObject(Activity.class);
                    activities.add(activity);
                }
                if (activities.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "沒有活動", Toast.LENGTH_SHORT).show();
                    return;
                }
                loadActivities(activities);
            } else {
                Toast.makeText(getApplicationContext(), "沒有活動", Toast.LENGTH_SHORT).show();
            }
        });
    }

    String passStartDate;
    String passEndDate;
    Activity currentActivity = null;
    private void loadActivities(List<Activity> activities) {
        Date today = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
        String currentYear = yearFormat.format(today);

        Activity nextActivity = null;

        for (Activity activity : activities) {
            String activityYear = yearFormat.format(activity.getDate());
            if (activityYear.equals(currentYear) && activity.getDate().before(today)) {
                if (currentActivity == null || activity.getDate().after(currentActivity.getDate())) {
                    currentActivity = activity;
                }
            } else if (activity.getDate().after(today)) {
                if (nextActivity == null || activity.getDate().before(nextActivity.getDate())) {
                    nextActivity = activity;
                }
            }
        }

        if (currentActivity != null) {
            String currentActivityStartDate = dateFormat.format(currentActivity.getDate());
            String currentActivityEndDate = nextActivity != null ? dateFormat.format(new Date(nextActivity.getDate().getTime() - 1)) : dateFormat.format(today);
            ((TextView) findViewById(R.id.challenge_nowTitleText)).setText(currentActivity.getName());
            ((TextView) findViewById(R.id.challenge_nowDateText)).setText(getString(R.string.ChallengeAll_illustrateText, currentActivityStartDate, currentActivityEndDate));
            SimpleDateFormat passDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            passStartDate = passDateFormat.format(currentActivity.getDate());
            passEndDate = nextActivity != null ? passDateFormat.format(new Date(nextActivity.getDate().getTime() - 1)) : passDateFormat.format(today);
            // Pass the goal, name, and date range to ChallengeNow activity
            Activity finalCurrentActivity = currentActivity;
            findViewById(R.id.challenge_nowLayout).setOnClickListener(v -> {
                Intent intent = new Intent(this, ChallengeNow.class);
                intent.putExtra("steps", mSteps);
                intent.putExtra("goal", finalCurrentActivity.getGoal());
                intent.putExtra("name", finalCurrentActivity.getName());
                intent.putExtra("simpleStartDate", currentActivityStartDate);
                intent.putExtra("simpleEndDate", currentActivityEndDate);
                intent.putExtra("startDate", passStartDate);
                intent.putExtra("endDate", passEndDate);
                startActivity(intent);
            });
            // Store the current activity's goal in a variable
            currentActivityGoal = finalCurrentActivity.getGoal();
        }

        if (nextActivity != null) {
            String nextActivityStartDate = dateFormat.format(nextActivity.getDate());
            Activity followingActivity = null;
            for (Activity activity : activities) {
                if (activity.getDate().after(nextActivity.getDate())) {
                    if (followingActivity == null || activity.getDate().before(followingActivity.getDate())) {
                        followingActivity = activity;
                    }
                }
            }
            String nextActivityEndDate = followingActivity != null ? dateFormat.format(new Date(followingActivity.getDate().getTime() - 1)) : "";
            ((TextView) findViewById(R.id.challenge_nextTitleText)).setText(nextActivity.getName());
            ((TextView) findViewById(R.id.challenge_nextDateText)).setText(getString(R.string.ChallengeAll_illustrateText, nextActivityStartDate, nextActivityEndDate));
        }
        checkSensors();
        registerSensors();
    }
    /*
    private void initNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "CurrentStep");
        mBuilder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("今日步數" + mSteps + " 步")
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setWhen(System.currentTimeMillis())//通知產生的時間，會在通知訊息中顯示
                .setPriority(Notification.DEFAULT_ALL)//設定該通知優先權
                .setAutoCancel(false)//設定這個標誌當使用者點擊面板就可以讓通知將自動取消
                .setOngoing(true)//ture，設定他為一個正在進行的通知。他們通常是用來表示一個後台任務,用戶積極參與(如播放音樂)或以某種方式正在等待,因此佔用設備(如一個文件下載,同步操作,主動網絡連接)
                .setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = mBuilder.build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground((Service) getApplicationContext(), 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
        }
        Log.d(TAG, "initNotification()");
    }*/
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.challenge_returnButton).setOnClickListener(v ->finish());
        //管理者機制，如果UID在AdminUID中，則可以新增活動
        //String[] AdminUID = getResources().getStringArray(R.array.AdminUID);
        //for (String uid : AdminUID) {
            //if (uid.equals(currentUser.getUid())) {
                findViewById(R.id.challenge_doggyImage).setOnClickListener(v -> createActivity());
            //}
        //}
    }

    private void createActivity() {
        View view = getLayoutInflater().inflate(R.layout.challenge_all_add,findViewById(R.id.main),false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("新增活動")
                .setView(view)
                .setPositiveButton("確定", (dialog, which) -> {
                    String activityName = ((TextView) view.findViewById(R.id.ChallengeAdd_nameEdit)).getText().toString();
                    String activityDate = ((TextView) view.findViewById(R.id.ChallengeAdd_dateEdit)).getText().toString();
                    String activityGoal = ((TextView) view.findViewById(R.id.ChallengeAdd_goalEdit)).getText().toString();
                    if (activityName.isEmpty() || activityDate.isEmpty() || activityGoal.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "請輸入活動名稱、日期和目標步數", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()); // Adjust the date format to match the input
                    Date date;
                    try { date = dateFormat.parse(activityDate);} catch (ParseException e) { throw new RuntimeException(e);}
                    Activity newActivity = new Activity(date, Integer.parseInt(activityGoal), activityName);
                    String documentName = new SimpleDateFormat("yyyy", Locale.getDefault()).format(date)+"-" +activityName;
                    db.collection("Activities").document(documentName).set(newActivity, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Activity successfully created!"))
                            .addOnFailureListener(e -> Log.w(TAG, "Error creating activity", e));
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    public static class Activity {
        private String name;
        private int goal;
        private Date date;
        public Activity() {}// No-argument constructor required for FireStore serialization
        public Activity(Date date, int goal, String name) {
            this.date = date;
            this.goal = goal;
            this.name = name;
        }
        public String getName() { return name;}
        public void setName(String name) { this.name = name;}
        public Date getDate() { return date;}
        public void setDate(Date date) { this.date = date;}
        public int getGoal() { return goal;}
        @SuppressWarnings("unused")
        public void setGoal(int goal) { this.goal = goal;}
    }
    private void getNowStep() {
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(System.currentTimeMillis());
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        db.collection("Users").document(currentUser.getUid()).collection("StepList").document(formattedDate).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                ChallengeHistoryStep challengeHistoryStep = task.getResult().toObject(ChallengeHistoryStep.class);
                if (challengeHistoryStep != null) {
                    Log.d(TAG, "Step number: " + challengeHistoryStep.getStepNumber());
                    mSteps = challengeHistoryStep.getStepNumber();
                }
            } else {
                Log.w(TAG, "Error getting document or document does not exist.", task.getException());
                ChallengeHistoryStep newStep = new ChallengeHistoryStep(0); // Default step number is 0
                db.collection("Users").document(currentUser.getUid()).collection("StepList").document(formattedDate).set(newStep)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "New step document created with default values."))
                        .addOnFailureListener(e -> Log.w(TAG, "Error creating new step document", e));
                mSteps = 0;
            }
            updateStepText();
        });
    }
    private void updateStepText() {
        ((TextView) findViewById(R.id.challenge_myStepText)).setText(getResources().getString(R.string.ChallengeAll_myStepText, mSteps));
    }
    private void checkSensors(){ //一個簡單的Android計步器：https://blog.csdn.net/TDSSS/article/details/125879573
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Handle devices with API level larger than 29
            String[] ACTIVITY_RECOGNITION_PERMISSION = { Manifest.permission.ACTIVITY_RECOGNITION }; //檢查許可權
            if (ContextCompat.checkSelfPermission(this, ACTIVITY_RECOGNITION_PERMISSION[0]) != PackageManager.PERMISSION_GRANTED) // 許可權是否已經 授權 GRANTED---授權  DENIED---拒絕
                ActivityCompat.requestPermissions(this, ACTIVITY_RECOGNITION_PERMISSION, 321); // 如果沒有授予該許可權，就去提示用戶請求自動開啟許可權
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321 && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            showPermissionDialog();
    }
    private void showPermissionDialog() {
        new AlertDialog.Builder(this) //提示用戶手動開啟許可權
                .setTitle("健康運動許可權").setMessage("健康運動許可權不可用")
                .setPositiveButton("立即開啟", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null)); // 跳轉到應用設置介面
                    activityResultLauncher.launch(intent);
                }).setNegativeButton("取消", (dialog, which) -> {
                    Toast.makeText(getApplicationContext(), "沒有獲得許可權，應用無法運行！", Toast.LENGTH_SHORT).show();
                    finish();
                }).setCancelable(false).show();
    }
    private void registerSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // 獲取SensorManager管理器實例
        registerSensor(sensorManager, Sensor.TYPE_STEP_COUNTER);
        registerSensor(sensorManager, Sensor.TYPE_STEP_DETECTOR);
    }
    private void registerSensor(SensorManager sensorManager, int sensorType) {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType); // 獲取計步器sensor
        if (sensor != null) sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        else {
            Toast.makeText(this, "沒有感測器", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No sensor found for type: " + sensorType);
        }
    }
    private void updateStepList() {
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        String userId = currentUser.getUid();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(System.currentTimeMillis());
        ChallengeHistoryStep newStep = new ChallengeHistoryStep(mSteps);
        db.collection("Users").document(userId).collection("StepList").document(formattedDate).set(newStep, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Step successfully updated!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating step", e));
    }
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Future<Integer> getStepsForPeriodAsync(String startDate, String endDate) {
        return executorService.submit(() -> {
            Log.d("FireStore", startDate + " " + endDate);
            if (currentUser == null) {
                Log.w("TaskRead", "No current user found.");
                return -1;
            }
            Log.d("FireStore", "getStepsForPeriod called");
            String userId = currentUser.getUid();
            Task<QuerySnapshot> task = db.collection("Users").document(userId).collection("StepList")
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), startDate)
                    .whereLessThanOrEqualTo(FieldPath.documentId(), endDate)
                    .get();
            try {
                Tasks.await(task);
                if (task.isSuccessful() && task.getResult() != null) {
                    int totalSteps = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        totalSteps += document.toObject(ChallengeNow.ChallengeHistoryStep.class).getStepNumber();
                    }
                    Log.d("FireStore", "Total steps for period: " + totalSteps);
                    return totalSteps;
                } else {
                    Log.w("FireStore", "Error getting documents.", task.getException());
                    return -1;
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FireStore", "Error waiting for task", e);
                return -1;
            }
        });
    }
    /*private int getStepsForPeriodSync(String startDate, String endDate) {
        Log.d("FireStore", startDate + " " + endDate);
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return -1;
        }
        Log.d("FireStore", "getStepsForPeriod called");
        String userId = currentUser.getUid();
        Task<QuerySnapshot> task = db.collection("Users").document(userId).collection("StepList")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), startDate)
                .whereLessThanOrEqualTo(FieldPath.documentId(), endDate)
                .get();
        try {
            Tasks.await(task);
            if (task.isSuccessful() && task.getResult() != null) {
                int totalSteps = 0;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    totalSteps += document.toObject(ChallengeNow.ChallengeHistoryStep.class).getStepNumber();
                }
                Log.d("FireStore", "Total steps for period: " + totalSteps);
                return totalSteps;
            } else {
                Log.w("FireStore", "Error getting documents.", task.getException());
                return -1;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("FireStore", "Error waiting for task", e);
            return -1;
        }
    }*/
    private void addPoints() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("Users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                Long currentPoints = document.getLong("points");
                if (currentPoints == null) currentPoints = 0L;
                userRef.set(Collections.singletonMap("points", currentPoints + 10), SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d("FireStore", "Points successfully updated!"))
                        .addOnFailureListener(e -> Log.w("FireStore", "Error updating points", e));
            } else {
                Log.w("FireStore", "Error getting user document", task.getException());
                // 如果用戶文檔不存在，則創建一個新文檔
                userRef.set(Collections.singletonMap("points", 5))
                        .addOnSuccessListener(aVoid -> Log.d("FireStore", "Document successfully created with initial points!"))
                        .addOnFailureListener(e -> Log.w("FireStore", "Error creating document", e));
            }
        });
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] == 1.0f) mSteps++;
        updateStepText();
        if (mSteps != 0) updateStepList();
        sendBroadcast(new Intent("com.PokeMeng.OldManGO.STEP_UPDATE"));
        Log.i(TAG, "Detected step changes:" + event.values[0]);
        if (currentActivityGoal == 0) {
            Toast.makeText(this, "目標步數不可為0，請查找問題出處", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mSteps >= 150) {
            taskManager.checkAndCompleteTask("Walked150", result -> {
                if (!result) {
                    taskManager.updateTaskStatusForSteps(0);
                    taskManager.markTaskAsCompleted("Walked150");
                }
            });
        }

        if (passStartDate != null && passEndDate != null) {
            new Thread(() -> {
                try {
                    int totalSteps = getStepsForPeriodAsync(passStartDate, passEndDate).get();
                    runOnUiThread(() -> {
                        if (totalSteps >= currentActivityGoal) {
                            String yearTaskName = new SimpleDateFormat("yyyy", Locale.getDefault()).format(currentActivity.getDate()) + "-" + currentActivity.getName();
                            DocumentReference activityRef = db.collection("Activities").document(yearTaskName);
                            activityRef.get().addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    List<String> finishUsers = (List<String>) task.getResult().get("finishUser");
                                    if (finishUsers == null || !finishUsers.contains(currentUser.getUid())) {
                                        activityRef.update("finishUser", FieldValue.arrayUnion(currentUser.getUid()))
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "User added to finishUser list");
                                                    addPoints();
                                                })
                                                .addOnFailureListener(e -> Log.w(TAG, "Error adding user to finishUser list", e));
                                    } else {
                                        Log.d(TAG, "User is already in the finishUser list");
                                    }
                                } else {
                                    Log.w(TAG, "Error getting activity document", task.getException());
                                }
                            });
                        }
                    });
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error getting steps for period", e);
                }
            }).start();
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG,"onAccuracyChanged");
    }
    /*
    private static Boolean cachedChallengeResult = null;
    private void checkAndCompleteChallenge(OnChallengeCheckCompleteListener listener) {
        if (Boolean.TRUE.equals(cachedChallengeResult)) {
            listener.onComplete(true);
            return;
        }
        String userId = "your_user_id"; // Replace with actual user ID
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(System.currentTimeMillis());
        DocumentReference challengeRef = db.collection("Users").document(userId)
                .collection("hasGetReward").document(formattedDate);
        challengeRef.get().addOnCompleteListener(task -> {
            boolean isChallengeCompleted = false;
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                Boolean challengeCompleted = document.getBoolean("ChallengeCompleted");
                if (challengeCompleted != null && challengeCompleted) {
                    isChallengeCompleted = true;
                    Log.d("FireStore", "Challenge already completed for today.");
                } else Log.d("FireStore", "Challenge not completed yet.");
            } else Log.w("FireStore", "Error getting challenge document", task.getException());
            cachedChallengeResult = isChallengeCompleted;
            listener.onComplete(isChallengeCompleted);
        });
    }
    private void updateTaskStatusForSteps() {
        String userId = "your_user_id"; // 替換為實際的用戶ID
        long currentDate = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate);
        // 更新 TaskStatus 集合中的第1個欄位為 true
        DocumentReference taskStatusRef = db.collection("Users").document(userId)
                .collection("TaskStatus").document(formattedDate);
        taskStatusRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                TaskAll.TaskStatus taskStatus = document.toObject(TaskAll.TaskStatus.class);
                if (taskStatus != null && taskStatus.getTaskStatus() != null && !taskStatus.getTaskStatus().isEmpty()) {
                    List<Boolean> taskStatuses = new ArrayList<>(taskStatus.getTaskStatus());
                    taskStatuses.set(6, true);
                    taskStatus.setTaskStatus(taskStatuses);
                    taskStatusRef.set(taskStatus, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.d("FireStore", "Task status successfully updated!"))
                            .addOnFailureListener(e -> Log.w("FireStore", "Error updating task status", e));
                }
            } else Log.w("FireStore", "Error getting task status document", task.getException());
        });
    }
    private void markChallengeAsCompleted() {
        FirebaseFireStore db = FirebaseFireStore.getInstance();
        String userId = "your_user_id"; // 替換為實際的用戶ID
        long currentDate = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate);
        // 更新 hasGetReward 集合
        DocumentReference rewardRef = db.collection("Users").document(userId)
                .collection("hasGetReward").document(formattedDate);
        rewardRef.set(Collections.singletonMap("ChallengeCompleted", true), SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FireStore", "Challenge completion status updated!"))
                .addOnFailureListener(e -> Log.w("FireStore", "Error updating challenge completion status", e));
        // 添加 10 積分
        DocumentReference userRef = db.collection("Users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                Long currentPoints = document.getLong("points");
                if (currentPoints == null) currentPoints = 0L;
                userRef.set(Collections.singletonMap("points", currentPoints + 10), SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d("FireStore", "Points successfully updated!"))
                        .addOnFailureListener(e -> Log.w("FireStore", "Error updating points", e));
            } else {
                Log.w("FireStore", "Error getting user document", task.getException());
                // 如果用戶文檔不存在，則創建一個新文檔
                userRef.set(Collections.singletonMap("points", 10))
                        .addOnSuccessListener(aVoid -> Log.d("FireStore", "Document successfully created with initial points!"))
                        .addOnFailureListener(e -> Log.w("FireStore", "Error creating document", e));
            }
        });
    }
    interface OnChallengeCheckCompleteListener {
        void onComplete(boolean result);
    }*/
    /*

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }*/
}