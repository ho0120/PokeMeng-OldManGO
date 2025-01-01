package com.PokeMeng.OldManGO;

import android.util.Log;

import com.PokeMeng.OldManGO.Task.TaskAll;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskManager {
    static Map<String, Boolean> cachedTaskResults = new HashMap<>();
    FirebaseFirestore db;
    String userId;

    // 【index】:【taskType】
    // 0:Walked150
    // 1:CheckIn
    // 2:CheckedMedicine
    // 3:CompletedMedicine
    // 4:WatchedVideo
    // 5:PlayedGame
    // 6:CheckChallenge

    public TaskManager(FirebaseFirestore db, String userId) {
        this.db = db;
        this.userId = userId;
    }

    public void checkAndCompleteTask(String taskType, OnTaskCheckCompleteListener listener) {
        if (Boolean.TRUE.equals(cachedTaskResults.get(taskType))) {
            listener.onComplete(true);
            return;
        }
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(System.currentTimeMillis());
        DocumentReference taskRef = db.collection("Users").document(userId)
                .collection("hasGetReward").document(formattedDate);
        taskRef.get().addOnCompleteListener(task -> {
            boolean isTaskCompleted = false;
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                Boolean taskCompleted = document.getBoolean(taskType);
                if (taskCompleted != null && taskCompleted) {
                    isTaskCompleted = true;
                    Log.d("FireStore", taskType + " already completed for today.");
                } else {
                    Log.d("FireStore", taskType + " not completed yet.");
                }
            } else {
                Log.w("FireStore", "Error getting task document", task.getException());
            }
            cachedTaskResults.put(taskType, isTaskCompleted);
            listener.onComplete(isTaskCompleted);
        });
    }

    public void updateTaskStatusForSteps(int index) {
        long currentDate = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate);
        DocumentReference taskStatusRef = db.collection("Users").document(userId)
                .collection("TaskStatus").document(formattedDate);
        taskStatusRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                TaskAll.TaskStatus taskStatus = document.toObject(TaskAll.TaskStatus.class);
                if (taskStatus != null && taskStatus.getTaskStatus() != null && !taskStatus.getTaskStatus().isEmpty()) {
                    List<Boolean> taskStatuses = new ArrayList<>(taskStatus.getTaskStatus());
                    taskStatuses.set(index, true);
                    taskStatus.setTaskStatus(taskStatuses);
                    taskStatusRef.set(taskStatus, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.d("FireStore", "Task status successfully updated!"))
                            .addOnFailureListener(e -> Log.w("FireStore", "Error updating task status", e));
                }
                else {
                    initializeDefaultTaskStatus(taskStatusRef, index);
                }
            } else {
                Log.w("FireStore", "Error getting task status document", task.getException());
                initializeDefaultTaskStatus(taskStatusRef, index);
            }
        });
    }
    private void initializeDefaultTaskStatus(DocumentReference taskStatusRef, int index) {
        List<Boolean> defaultStatus = new ArrayList<>(Collections.nCopies(7, false));
        defaultStatus.set(index, true);
        TaskAll.TaskStatus defaultTaskStatus = new TaskAll.TaskStatus(Arrays.asList("每日健走150步", "每日簽到", "用藥提醒查看", "今日已完成用藥", "觀看運動影片", "玩遊戲(防失智)", "查看運動挑戰"), defaultStatus);
        taskStatusRef.set(defaultTaskStatus, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FireStore", "Default task status successfully initialized!"))
                .addOnFailureListener(e -> Log.w("FireStore", "Error initializing default task status", e));
    }
    public void markTaskAsCompleted(String taskType) {
        long currentDate = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate);
        DocumentReference rewardRef = db.collection("Users").document(userId)
                .collection("hasGetReward").document(formattedDate);
        rewardRef.set(Collections.singletonMap(taskType, true), SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FireStore", taskType + " completion status updated!"))
                .addOnFailureListener(e -> Log.w("FireStore", "Error updating " + taskType + " completion status", e));
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
                userRef.set(Collections.singletonMap("points", 10))
                        .addOnSuccessListener(aVoid -> Log.d("FireStore", "Document successfully created with initial points!"))
                        .addOnFailureListener(e -> Log.w("FireStore", "Error creating document", e));
            }
        });
    }
    public void isTaskCompletedToday(String taskType, OnTaskCheckCompleteListener listener) {
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(System.currentTimeMillis());
        DocumentReference taskRef = db.collection("Users").document(userId)
                .collection("hasGetReward").document(formattedDate);
        taskRef.get().addOnCompleteListener(task -> {
            boolean isTaskCompleted = false;
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                Boolean taskCompleted = document.getBoolean(taskType);
                if (taskCompleted != null && taskCompleted) {
                    isTaskCompleted = true;
                    Log.d("FireStore", taskType + " already completed for today.");
                } else {
                    Log.d("FireStore", taskType + " not completed yet.");
                }
            } else {
                Log.w("FireStore", "Error getting task document", task.getException());
            }
            listener.onComplete(isTaskCompleted);
        });
    }
    public interface OnTaskCheckCompleteListener {
        void onComplete(boolean result);
    }
}