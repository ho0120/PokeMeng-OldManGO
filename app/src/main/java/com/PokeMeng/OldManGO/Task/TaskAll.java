package com.PokeMeng.OldManGO.Task;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.PokeMeng.OldManGO.Challenge.ChallengeAll;
import com.PokeMeng.OldManGO.DailyCheckIn.CheckIn;
import com.PokeMeng.OldManGO.Game.GameMain;
import com.PokeMeng.OldManGO.Medicine.MainActivity5;
import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.Video.video_main;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.hdev.calendar.bean.DateInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TaskAll extends AppCompatActivity {
    private DateInfo nowChooseDate = new DateInfo(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    ArrayList<String> taskList = new ArrayList<>();
    private ArrayList<DateInfo> dateList;
    TaskAdapter adapter;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.task_all);
        setupWindowInsets();
        loadTodaySchedule();
        setupButtons();
        checkIfRewardClaimed(); // 檢查是否已經領取過獎勵
    }
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void setupButtons() {
        findViewById(R.id.TaskAll_returnButton).setOnClickListener(v -> finish());
        findViewById(R.id.TaskAll_addImage).setOnClickListener(v -> registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> Toast.makeText(this, result.getResultCode() == Activity.RESULT_OK ? "成功新增" : "取消新增", Toast.LENGTH_LONG).show()).launch(new Intent(this, TaskAdd.class)));
        findViewById(R.id.TaskAll_scheduledButton).setOnClickListener(v -> startActivity(new Intent(this, TaskScheduled.class)));
        findViewById(R.id.TaskAll_cheatButton).setOnClickListener(v -> {
            for (int i = 0; i < taskList.size(); i++) {
                setTaskStatusTrue(i);
                adapter.saveTaskStatus();
            }
            adapter.notifyDataSetChanged();
        });
        setupDateSpinner();
    }
    private void setTaskStatusTrue(int position) {
        if (adapter.taskStatus == null) return;
        int index = adapter.taskStatus.getTaskName().indexOf(taskList.get(position));
        if (index != -1) {
            List<Boolean> taskStatuses = new ArrayList<>(adapter.taskStatus.getTaskStatus());
            taskStatuses.set(index, true);
            adapter.taskStatus.setTaskStatus(taskStatuses);
        }
    }
    private void setupDateSpinner() {
        loadDateSpinner();
        ((Spinner)findViewById(R.id.TaskAll_dateSpinner)).setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                taskList.clear();
                nowChooseDate = dateList.get(position);
                loadScheduledTasks(nowChooseDate);
                checkAndLoadTaskStatus();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    private void loadDateSpinner() {
        dateList = new ArrayList<>();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance().collection("Users").document(userId).collection("TaskDetails").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                processTaskDocuments(task.getResult().getDocuments());
                //如果當天沒有任務，則加入當天日期
                if (!dateList.contains(nowChooseDate)) dateList.add(nowChooseDate);
                sortAndSetDateSpinner();
                checkAndLoadTaskStatus(); // 重新載入當天任務
            } else
                Log.w("TaskRead", "Error getting documents.", task.getException());
        });
    }

    private void processTaskDocuments(List<DocumentSnapshot> documents) {
        for (DocumentSnapshot document : documents) {
            Object taskDatesObj = document.get("任務日期");
            if (taskDatesObj instanceof List<?> taskDates) {
                for (Object timestampObj : taskDates)
                    if (timestampObj instanceof Timestamp)
                        addDateInfoFromTimestamp((Timestamp) timestampObj);
            }
        }
    }

    private void addDateInfoFromTimestamp(Timestamp timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(timestamp.toDate());
        DateInfo dateInfo = new DateInfo(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        if (!dateList.contains(dateInfo)) dateList.add(dateInfo);
    }

    private void sortAndSetDateSpinner() {
        dateList.sort(this::compareDates);
        ArrayList<String> dateListString = new ArrayList<>();
        for (DateInfo dateInfo : dateList)
            dateListString.add(dateInfo.getYear() + "/" + dateInfo.getMonth() + "/" + dateInfo.getDay());
        Spinner dateSpinner = findViewById(R.id.TaskAll_dateSpinner);
        dateSpinner.setAdapter(new CustomSpinnerAdapter(this, dateListString, dateList.indexOf(nowChooseDate), Color.WHITE));
        dateSpinner.setSelection(dateList.indexOf(nowChooseDate));
    }
    private int compareDates(DateInfo d1, DateInfo d2) {
        if (d1.getYear() != d2.getYear()) return d1.getYear() - d2.getYear();
        if (d1.getMonth() != d2.getMonth()) return d1.getMonth() - d2.getMonth();
        return d1.getDay() - d2.getDay();
    }
    //檢查並載入任務狀態
    private void checkAndLoadTaskStatus() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(nowChooseDate.toCalendar().getTime());
        db.collection("Users").document(currentUser.getUid())
                .collection("TaskStatus").document(formattedDate)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        TaskStatus savedStatus = task.getResult().toObject(TaskStatus.class);
                        if (savedStatus != null) adapter.taskStatus = savedStatus;
                        else initializeDefaultTaskStatus();
                    } else initializeDefaultTaskStatus();
            adapter.notifyDataSetChanged();
        });
    }
    //當沒有任務狀態時，初始化為預設狀態
    private void initializeDefaultTaskStatus() {
        List<String> taskNames = new ArrayList<>(Arrays.asList(taskList.toArray(new String[0])));
        List<Boolean> defaultStatus = new ArrayList<>(Collections.nCopies(taskList.size(), false));
        adapter.taskStatus = new TaskStatus(taskNames, defaultStatus);
    }
    //載入今日任務，初始化adapter並顯示在ListView上
    private void loadTodaySchedule() {
        DateInfo today = new DateInfo(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        ListView listView = findViewById(R.id.TaskAll_todayList);
        adapter = new TaskAdapter(this, taskList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        loadScheduledTasks(today);
    }
    //載入已排定任務
    private void loadScheduledTasks(DateInfo compareDate) {
        //載入固定任務
        taskList.addAll(Arrays.asList("每日健走150步", "每日簽到", "用藥提醒查看", "今日已完成用藥", "觀看運動影片", "玩遊戲(防失智)", "查看運動挑戰"));
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        //載入自訂任務
        FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid()).collection("TaskDetails").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult())
                    processDocument(document, compareDate);
                adapter.notifyDataSetChanged();
            } else Log.w("TaskRead", "Error getting documents.", task.getException());
        });
    }

    private void processDocument(QueryDocumentSnapshot document, DateInfo compareDate) {
        String taskName = document.getString("任務名稱");
        Object taskDatesObj = document.get("任務日期");
        if (taskDatesObj instanceof List<?> taskDates) {
            for (Object timestampObj : taskDates) {
                if (timestampObj instanceof Timestamp) {
                    DateInfo dateInfo = getDateInfoFromTimestamp((Timestamp) timestampObj);
                    if (compareDateInfo(Collections.singletonList(dateInfo), compareDate)) {
                        taskList.add(taskName != null ? taskName : "null task name");
                        break;
                    }
                }
            }
        }
    }

    private DateInfo getDateInfoFromTimestamp(Timestamp timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(timestamp.toDate());
        return new DateInfo(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }
    public boolean compareDateInfo(List<DateInfo> dateInfoList, DateInfo compareDate) {
        for (DateInfo dateInfo : dateInfoList)  //日期格式化：http://www.cftea.com/c/2017/03/6865.asp
            if (dateInfo.getYear() == compareDate.getYear() && dateInfo.getMonth() == compareDate.getMonth() && dateInfo.getDay() == compareDate.getDay())
                return true;
        return false;
    }
    //確認是否已經領取過獎勵
    private boolean checkIfRewardClaimed() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return false;
        }
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(System.currentTimeMillis());
        DocumentReference rewardRef = db.collection("Users").document(currentUser.getUid())
                .collection("hasGetReward").document(formattedDate);
        final boolean[] hasClaimedReward = {false};
        rewardRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists() && document.contains("FullCompleted")) {
                    hasClaimedReward[0] = Boolean.TRUE.equals(document.getBoolean("FullCompleted"));
                    if (hasClaimedReward[0]) ((TextView)findViewById(R.id.TaskAll_hintText)).setText("已領取全選獎勵！");
                }
            } else {
                Log.w("FireStore", "Error getting reward document", task.getException());
            }
        });
        fetchAndDisplayUserPoints(); // 查詢用戶積分並顯示
        return hasClaimedReward[0];
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadDateSpinner();
        checkAndLoadTaskStatus();
    }
    public class TaskAdapter extends ArrayAdapter<String> {
        List<String> tasksList;
        public TaskStatus taskStatus;
        public TaskAdapter(@NonNull Context context, List<String> list) {
            super(context, R.layout.task_all_listview, list);
            tasksList = list;
        }
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            convertView = convertView == null ? LayoutInflater.from(getContext()).inflate(R.layout.task_all_listview, parent, false) : convertView;
            CheckedTextView checkedTextView = convertView.findViewById(R.id.AllList_checkedText);
            boolean isChecked = isTaskChecked(tasksList.get(position));
            checkedTextView.setText(tasksList.get(position));
            checkedTextView.setChecked(isChecked);
            checkedTextView.setBackgroundResource(isChecked ? R.drawable.task_all_listview_background_strikethrough : 0);
            convertView.findViewById(R.id.AllList_chooseImage).setVisibility(position >= 7 ? View.GONE : View.VISIBLE);
            convertView.findViewById(R.id.AllList_chooseImage).setOnClickListener(v -> changeActivity(position));
            convertView.setOnClickListener(v -> handleItemClick(position));
            return convertView;
        }
        private boolean isTaskChecked(String taskName) {
            if (taskStatus == null) return false;
            int index = taskStatus.getTaskName().indexOf(taskName);
            return index != -1 && taskStatus.getTaskStatus().get(index);
        }
        private void handleItemClick(int position) {
            DateInfo today = new DateInfo(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            if (position < 7)
                Toast.makeText(getContext(), "此為固定任務", Toast.LENGTH_SHORT).show();
            else if(!compareDateInfo(Collections.singletonList(dateList.get(((Spinner)findViewById(R.id.TaskAll_dateSpinner)).getSelectedItemPosition())),today))
                Toast.makeText(getContext(), "只能勾選當日完成的任務喔", Toast.LENGTH_SHORT).show();
            else updateTaskStatus(position);
        }
        private void changeActivity(int position) {
            switch (position){
                case 0:
                case 6:
                    startActivity(new Intent(getContext(), ChallengeAll.class));
                    break;
                case 1:
                    startActivity(new Intent(getContext(), CheckIn.class));
                    break;
                case 2:
                case 3:
                    startActivity(new Intent(getContext(), MainActivity5.class));
                    break;
                case 4:
                    startActivity(new Intent(getContext(), video_main.class));
                    break;
                case 5:
                    startActivity(new Intent(getContext(), GameMain.class));
                    break;
            }
        }
        private void updateTaskStatus(int position) {
            Log.d("TaskAdapter", "Updating task status for position: " + position);
            String taskName = tasksList.get(position);
            //查找任務名稱在taskStatus中的位置
            int taskIndex = taskStatus.getTaskName().indexOf(taskName);
            //如果找不到，則新增任務名稱並設置狀態為true
            if (taskIndex == -1) {
                List<String> taskNames = new ArrayList<>(taskStatus.getTaskName());
                taskNames.add(taskName);
                List<Boolean> newTaskStatuses = new ArrayList<>(taskStatus.getTaskStatus());
                newTaskStatuses.add(true);
                taskStatus.setTaskName(taskNames);
                taskStatus.setTaskStatus(newTaskStatuses);
            }
            //如果找到，則切換任務狀態
            else {
                List<Boolean> taskStatuses = new ArrayList<>(taskStatus.getTaskStatus());
                taskStatuses.set(taskIndex, !taskStatuses.get(taskIndex));
                taskStatus.setTaskStatus(taskStatuses);
            }
            saveTaskStatus();
            notifyDataSetChanged();
        }
        private void saveTaskStatus() {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (currentUser == null) {
                Log.w("TaskRead", "No current user found.");
                return;
            }
            long currentDate = System.currentTimeMillis();
            String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate);
            db.collection("Users").document(currentUser.getUid())
                    .collection("TaskStatus").document(formattedDate).set(taskStatus, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("FireStore", "日期狀態成功上傳!"))
                    .addOnFailureListener(e -> Log.w("FireStore", "上船日期狀態失敗", e));
            checkAndAddPointsIfAllTasksCompleted();
        }
    }
    private void checkAndAddPointsIfAllTasksCompleted() {
        Log.d("TaskAll", "Checking if all tasks are completed");
        if (adapter.taskStatus == null) return;
        boolean allTasksCompleted = true;
        for (Boolean status : adapter.taskStatus.getTaskStatus())
            if (!status) {
                allTasksCompleted = false;
                break;
            }
        if (allTasksCompleted && !checkIfRewardClaimed()) {
            addPoints();
            markRewardAsClaimed();
        }
    }

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
    //標記獎勵已領取並上傳
    private void markRewardAsClaimed() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        long currentDate = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate);
        DocumentReference rewardRef = db.collection("Users").document(currentUser.getUid())
                .collection("hasGetReward").document(formattedDate);
        rewardRef.set(Collections.singletonMap("FullCompleted", true), SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FireStore", "Reward claim status updated!"))
                .addOnFailureListener(e -> Log.w("FireStore", "Error updating reward claim status", e));
        checkIfRewardClaimed();
    }
    private void fetchAndDisplayUserPoints() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        db.collection("Users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                Long currentPoints = document.getLong("points");
                if (currentPoints == null) currentPoints = 0L;
                TextView pointsTextView = findViewById(R.id.TaskAll_pointsText);
                pointsTextView.setText(getResources().getString(R.string.TaskAll_all_points, currentPoints));
            } else {
                Log.w("FireStore", "Error getting user document", task.getException());
            }
        });
    }
    //下列黃色的是序列化的屬性，加了之後Firebase才知道包含哪些屬性
    @IgnoreExtraProperties
    public static class TaskStatus {
        @PropertyName("task_name")
        private List<String> taskName;
        @PropertyName("is_done")
        private List<Boolean> isDone;

        @SuppressWarnings("unused")
        // 呼叫 DataSnapshot.getValue(TaskStatus.class) 所需的預設建構子(簡單說就是刪了會壞掉)
        public TaskStatus() {}
        public TaskStatus(List<String> taskName, List<Boolean> isDone) {
            this.taskName = taskName;
            this.isDone = isDone;
        }
        @PropertyName("task_name")
        public List<String> getTaskName() { return taskName;}
        @PropertyName("is_done")
        public List<Boolean> getTaskStatus() { return isDone;}
        @PropertyName("task_name")
        public void setTaskName(List<String> taskName) { this.taskName = taskName;}
        @PropertyName("is_done")
        public void setTaskStatus(List<Boolean> done) { isDone = done;}
    }
    static class CustomSpinnerAdapter extends ArrayAdapter<String> {
        Context context;
        List<String> items;
        int specialItemIndex,specialItemColor;
        public CustomSpinnerAdapter(@NonNull Context context, @NonNull List<String> items, int specialItemIndex, int specialItemColor) {
            super(context, android.R.layout.simple_spinner_item, items);
            this.context = context;
            this.items = items;
            this.specialItemIndex = specialItemIndex;
            this.specialItemColor = specialItemColor;
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return Objects.requireNonNull(createView(position, convertView));
        }
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createView(position, convertView);
        }
        private View createView(int position, @Nullable View convertView) {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(context);
                textView.setPadding(16, 16, 16, 16);
                textView.setTextSize(18);
                textView.setTextColor(Color.BLACK);
            } else textView = (TextView) convertView;
            textView.setText(items.get(position));
            textView.setBackgroundColor(position == specialItemIndex ? specialItemColor : 0);
            return textView;
        }
    }
}
