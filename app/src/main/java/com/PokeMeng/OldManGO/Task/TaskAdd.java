package com.PokeMeng.OldManGO.Task;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.PokeMeng.OldManGO.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hdev.calendar.bean.DateInfo;
import com.hdev.calendar.view.MultiCalendarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskAdd extends AppCompatActivity {
    List<DateInfo> dateInfoList;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.task_add);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.TaskAdd_returnButton).setOnClickListener(v -> finish());
        findViewById(R.id.TaskAdd_chooseButton).setOnClickListener(v -> chooseDate());
        findViewById(R.id.TaskAdd_saveButton).setOnClickListener(v -> saveTask());
    }

    private void chooseDate() {
        //自定義日歷元件：https://blog.csdn.net/coffee_shop/article/details/130709029
        View dialogView = getLayoutInflater().inflate(R.layout.task_add_calendar, null);
        MultiCalendarView mCalendarView = dialogView.findViewById(R.id.TaskAdd_CalendarView);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("選擇日期").setView(dialogView).setMessage("請選擇日期")
                .setPositiveButton("確定", (dialog1, which) -> {
                    dateInfoList = mCalendarView.getSelectedDateList();
                    List<String> dateList = new ArrayList<>();
                    for (DateInfo dateInfo : dateInfoList)
                        dateList.add(dateInfo.getYear() + "/" + dateInfo.getMonth() + "/" + dateInfo.getDay());
                    ((TextView)findViewById(R.id.TaskAdd_dateEdit)).setText(dateList.isEmpty() ? "" : dateList.toString());
                }).create();    //currentTimeMillis()：https://blog.csdn.net/qq_37370132/article/details/107905587
        mCalendarView.setDateRange(System.currentTimeMillis(), System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000, System.currentTimeMillis());
        mCalendarView.setOnMultiDateSelectedListener((view, selectedDays, selectedDates) -> {
            dialog.setMessage(selectedDates.isEmpty() ? "請選擇日期" : "已選擇" + selectedDates.size() + "天");
            return null;
        });
        dialog.show();
    }

    private void saveTask() {
        EditText editText = findViewById(R.id.TaskAdd_nameEdit);
        EditText dateEdit = findViewById(R.id.TaskAdd_dateEdit);
        String taskName = editText.getText().toString();
        if (taskName.isEmpty()) {
            editText.setError("記得填任務名稱！");
            editText.requestFocus();
            return;
        }
        if (dateEdit.getText().toString().isEmpty()) {
            dateEdit.setEnabled(true);
            dateEdit.setError("您忘了選日期");
            dateEdit.requestFocus();
            new Handler().postDelayed(() -> dateEdit.setEnabled(false), 1500);
            return;
        }
        List<Timestamp> dateTimestamps = new ArrayList<>();
        for (DateInfo dateInfo : dateInfoList) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(dateInfo.getYear(), dateInfo.getMonth() - 1, dateInfo.getDay(), 0, 0, 0);
            dateTimestamps.add(new Timestamp(calendar.getTime()));
        }
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("任務名稱", taskName);
        taskData.put("任務日期", dateTimestamps);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        String userId = currentUser.getUid();
        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                db.collection("Users").document(userId).collection("TaskDetails").add(taskData)
                        .addOnSuccessListener(documentReference -> {
                            setResult(RESULT_OK, getIntent());
                            finish();
                        }).addOnFailureListener(e -> Toast.makeText(this, "Error saving task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Map<String, Object> userData = new HashMap<>();
                userData.put("gmail", currentUser.getEmail());
                userData.put("name", currentUser.getDisplayName());
                db.collection("Users").document(userId).set(userData)
                        .addOnSuccessListener(aVoid -> db.collection("Users").document(userId).collection("TaskDetails").add(taskData)
                                .addOnSuccessListener(documentReference -> {
                                    setResult(RESULT_OK, getIntent());
                                    finish();
                                }).addOnFailureListener(e -> Toast.makeText(this, "Error saving task: " + e.getMessage(), Toast.LENGTH_SHORT).show())).addOnFailureListener(e -> Toast.makeText(this, "Error creating user document: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error fetching user document: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}