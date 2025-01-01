package com.PokeMeng.OldManGO.Challenge;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.PokeMeng.OldManGO.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChallengeNow extends AppCompatActivity {

    StepUpdateReceiver stepUpdateReceiver = new StepUpdateReceiver();
    private boolean isReceiverRegistered = false;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.challenge_now);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.now_checkButton).setOnClickListener(v -> showHistory());
        findViewById(R.id.now_returnButton).setOnClickListener(v -> finish());
        Intent intent = getIntent();
        getStepsForPeriod(intent.getStringExtra("startDate"), intent.getStringExtra("endDate"), totalSteps -> setNowStep(totalSteps, intent.getIntExtra("goal", 0), 2000));
        ((TextView)findViewById(R.id.now_titleText)).setText(intent.getStringExtra("name"));
        ((TextView)findViewById(R.id.now_illustrateText)).setText(getResources().getString(R.string.ChallengeNow_illustrateText,getString(R.string.ChallengeAll_illustrateText, intent.getStringExtra("simpleStartDate"), intent.getStringExtra("simpleEndDate"))));
        registerReceiver(stepUpdateReceiver, new IntentFilter("com.PokeMeng.OldManGO.STEP_UPDATE"), Context.RECEIVER_EXPORTED); // 註冊廣播接收器
        isReceiverRegistered = true;
    }
    private void checkFinish(int nowStep, int goal) {
        if(nowStep >= goal) findViewById(R.id.now_finishImage).setVisibility(View.VISIBLE);
        else findViewById(R.id.now_finishImage).setVisibility(View.GONE);
    }
    private void setNowStep(int step_now, int step_goal, int duration) {
        if (step_goal == 0) {
            Toast.makeText(this, "目標步數不可為0，請查找問題出處", Toast.LENGTH_SHORT).show();
            return;
        }
        ChallengeNowProgressBar challengeNowProgressBar = findViewById(R.id.now_nowCircularProgressBar);
        challengeNowProgressBar.setText("目前步數：" + step_now + "步" + "\n" + "目標步數：" + step_goal + "步");
        float progress = (float) step_now / step_goal * 100;
        progress = Math.max(0, Math.min(progress, 100)); // Ensure progress is between 0 and 100
        challengeNowProgressBar.setProgress(progress);
        challengeNowProgressBar.setDuration(duration);
        checkFinish(step_now, step_goal);
    }
    private void getStepList(FireStoreCallback callback) {
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        String userId = currentUser.getUid();
        db.collection("Users").document(userId).collection("StepList").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<ChallengeHistoryStep> taskList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    ChallengeHistoryStep challengeHistoryStep = document.toObject(ChallengeHistoryStep.class);
                    challengeHistoryStep.setStepDate(document.getId()); // Set the document ID as the step date
                    taskList.add(challengeHistoryStep);
                }
                callback.onCallback(taskList);
            } else Log.w("FireStore", "Error getting documents.", task.getException());
        });
    }
    private void getStepsForPeriod(String startDate, String endDate, FireStoreCallback2 callback) {
        Log.d("FireStore", startDate + " " + endDate);
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            Toast.makeText(this, "資料錯誤", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startDate == null || endDate == null) {
            Log.w("FireStore", "Start date or end date is null.");
            Toast.makeText(this, "日期錯誤", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("FireStore", "getStepsForPeriod called");
        String userId = currentUser.getUid();
        db.collection("Users").document(userId).collection("StepList")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), startDate)
                .whereLessThanOrEqualTo(FieldPath.documentId(), endDate)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int totalSteps = 0;
                        for (QueryDocumentSnapshot document : task.getResult())
                            totalSteps += document.toObject(ChallengeHistoryStep.class).getStepNumber();
                        callback.onCallback(totalSteps);
                        Log.d("FireStore", "Total steps for period: " + totalSteps);
                    } else
                        Log.w("FireStore", "Error getting documents.", task.getException());
                });
    }
    private void showHistory() {
        View dialogView = getLayoutInflater().inflate(R.layout.challenge_now_history, null);
        setHistory(dialogView);
        new AlertDialog.Builder(this).setTitle("歷史紀錄").setView(dialogView).setPositiveButton("確定", null).create().show();
    }
    private void setHistory(View dialogView) {
        getStepList(messages -> {
            ArrayAdapter<ChallengeHistoryStep> adapter = new ArrayAdapter<>(this, R.layout.challenge_now_history_setview, messages) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    convertView = convertView == null ? LayoutInflater.from(getContext()).inflate(R.layout.challenge_now_history_setview, parent, false) : convertView;
                    ChallengeHistoryStep currentMessage = getItem(position);
                    ((TextView) convertView.findViewById(R.id.Listview_timeText)).setText(Objects.requireNonNull(currentMessage).getStepDate());
                    ((TextView) convertView.findViewById(R.id.Listview_numberText)).setText(getString(R.string.ChallengeNow_DateStep, Objects.requireNonNull(currentMessage).getStepNumber())); // Set their text
                    return convertView;
                }
            };
            ((ListView) dialogView.findViewById(R.id.history_listview)).setAdapter(adapter);
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) unregisterReceiver(stepUpdateReceiver); // 取消註冊廣播接收器
    }
    private class StepUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Broadcast received");
            if (intent != null && "com.PokeMeng.OldManGO.STEP_UPDATE".equals(intent.getAction())) {
                getStepsForPeriod(getIntent().getStringExtra("startDate"), getIntent().getStringExtra("endDate"), totalSteps -> setNowStep(totalSteps, getIntent().getIntExtra("goal", 0), 1));
            }
        }
    }
    private interface FireStoreCallback {
        void onCallback(List<ChallengeHistoryStep> list);
    }
    private interface FireStoreCallback2 {
        void onCallback(int totalSteps);
    }
    public static class ChallengeHistoryStep {
        private String stepDate;
        int stepNumber;
        //序列化失敗處理-無參數建構子https://stackoverflow.com/questions/60389906/could-not-deserialize-object-does-not-define-a-no-argument-constructor-if-you：
        @SuppressWarnings("unused") public ChallengeHistoryStep() {}
        @SuppressWarnings("unused") public ChallengeHistoryStep(int stepNumber) { this.stepNumber = stepNumber;}
        public int getStepNumber() { return stepNumber;}
        public String getStepDate() { return stepDate;}
        public void setStepDate(String stepDate) { this.stepDate = stepDate;}
    }




    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        // Start or resume any tasks that need to be done when the activity is visible
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Resume any paused tasks or refresh the UI
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        // Pause any ongoing tasks or release resources that are not needed when the activity is not in the foreground
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        // Stop any tasks that should not run when the activity is not visible
    }

}