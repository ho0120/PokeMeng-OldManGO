package com.PokeMeng.OldManGO.DailyCheckIn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.PokeMeng.OldManGO.MainActivity;
import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.TaskManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Calendar;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;

import android.content.res.Configuration;

import java.util.Locale;

import com.hankcs.hanlp.HanLP;


public class CheckIn extends AppCompatActivity {

    private Button button;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 設置應用全局的Locale為繁體中文
        Locale locale = Locale.TAIWAN;
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        //getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        getResources().getConfiguration().locale = locale;
        getResources().updateConfiguration(getResources().getConfiguration(), getResources().getDisplayMetrics());

        setContentView(R.layout.check_in);


        button = findViewById(R.id.button);
        button.setOnClickListener(v -> markDailyCheckIn());
        findViewById(R.id.back).setOnClickListener(v -> finish());

        // 指定 FireStore 集合名稱

        // 獲取當前用戶
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar currentCalendar = Calendar.getInstance();
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        int dayOfMonth = currentCalendar.get(Calendar.DAY_OF_MONTH);
        updateDateDisplay(year, month, dayOfMonth);
        button.setEnabled(isToday(year, month, dayOfMonth));  // 今天可以簽到
        ((CalendarView)findViewById(R.id.calendarView)).setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                selectedDate=Integer.toString(year)+Integer.toString(month+1)+Integer.toString(dayOfMonth);
                updateDateDisplay(year, month , dayOfMonth);
                // 檢查選擇的日期是否為今天
                if (isToday(year, month, dayOfMonth)) {
                    button.setEnabled(true);  // 今天可以簽到
                    checkIfAlreadyCheckedIn(selectedDate); // 只在今天才進行簽到檢查
                } else {
                    button.setEnabled(false); // 其他日子不能簽到
                    Toast.makeText(CheckIn.this, "無法對過去或未來的日期簽到", Toast.LENGTH_SHORT).show();
                }
                selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
                updateDateDisplay(year, month , dayOfMonth);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTaskStatusForToday();

                if (selectedDate != null) {
                    addTaskStatusForToday();  // 簽到操作
                } else {
                    Toast.makeText(CheckIn.this, "請先選擇日期", Toast.LENGTH_SHORT).show();
                    checkIfAlreadyCheckedIn(selectedDate); // 檢查是否已簽到
                }
            }
        });

        //databaseReference= FirebaseDatabase.getInstance().getReference("CalendarView");
    }

    // 應用啟動時檢查今天的簽到狀態
    //  @Override
//    protected void onStart() {
//        super.onStart();
//        Calendar today = Calendar.getInstance();
//        selectedDate = today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-" + today.get(Calendar.DAY_OF_MONTH);
//        checkIfAlreadyCheckedIn(selectedDate); // 檢查今天的簽到狀態
//    }

//    private String getFormattedDate(Calendar calendar) {
//        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.getTime());
//    }
//
//    private String getFormattedDate(int year, int month, int dayOfMonth) {
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(year, month, dayOfMonth);
//        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.getTime());
//    }

    private void updateDateDisplay(int year, int month, int dayOfMonth) {
        selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;

        // 使用lunar-java庫進行公曆轉農曆
        Solar solar = new Solar(year, month+1, dayOfMonth);
        Lunar lunar = solar.getLunar();  // 轉換為農曆日期

        String lunarFestival = lunar.getFestivals().toString();
        String solarFestival = solar.getFestivals().toString();
        String jieQi = lunar.getJieQi();

        // 如果 lunar.toString() 也是簡體中文，則轉換
        String lunarDate = HanLP.convertToTraditionalChinese(lunar.toString());

        // 使用 HanLP 進行簡繁轉換
        lunarFestival = HanLP.convertToTraditionalChinese(lunarFestival);
        solarFestival = HanLP.convertToTraditionalChinese(solarFestival);
        jieQi = HanLP.convertToTraditionalChinese(jieQi);

        // 自定義節日修改
        if (month == 4 && dayOfMonth == 1) { // 如果是 4/1
            solarFestival = "愚人節"; // 修改為愚人節
        }
        if (month == 3 && dayOfMonth == 4) { // 如果是 4/4
            solarFestival = "兒童節"; // 修改為兒童節
        }
        if (month == 4 && dayOfMonth == 1) { // 如果是 5/1
            solarFestival = "勞動節"; // 修改為勞動節
        }
        if (month == 4 && dayOfMonth == 4) { // 如果是 5/4
            solarFestival = "青年節"; // 修改為青年節
        }
        if (month == 8 && dayOfMonth == 28) { // 如果是 9/28
            solarFestival = "教師節"; // 修改為教師節
        }
        if (month == 9 && dayOfMonth == 10) { // 如果是 10/10
            solarFestival = "國慶節"; // 修改為國慶節
        }
        if (month == 10 && dayOfMonth == 31) { // 如果是 10/31
            solarFestival = "萬聖節前夜"; // 修改為萬聖節前夜
        }
        if (month == 11 && dayOfMonth == 1) { // 如果是 11/1
            solarFestival = "萬聖節"; // 修改為萬聖節
        }
        if (month == 12 && dayOfMonth == 24) { // 如果是 12/24
            solarFestival = "平安夜"; // 修改為平安夜
        }
        if (month == 12 && dayOfMonth == 25) { // 如果是 12/25
            solarFestival = "聖誕節"; // 修改為聖誕節
        }
        else if (month == 8 && dayOfMonth == 10) { // 如果是 8/10
            solarFestival = ""; // 清空或顯示其他資訊
        } else if (month == 5 && dayOfMonth == 1) { // 如果是 6/1
            solarFestival = ""; // 不顯示兒童節
        } else if (month == 6 && dayOfMonth == 1) { // 如果是 7/1
            solarFestival = ""; // 不顯示建黨節
        } else if (month == 7 && dayOfMonth == 1) { // 如果是 8/1
            solarFestival = ""; // 不顯示建軍節
        } else if (month == 9 && dayOfMonth == 1) { // 如果是 8/1
            solarFestival = ""; // 不顯示建軍節
        }

// 構建顯示字串
        String displayText = "公曆: " + selectedDate + "\n農曆: " + lunar.toString();

// 如果有農曆節日則顯示
        if (!lunarFestival.isEmpty()) {
            displayText += "\n農曆節日: " + lunarFestival;
        }
        // 檢查節氣是否為清明
        if ("清明".equals(jieQi)) {
            displayText += "\n農曆節日: [清明節]"; // 在農曆節日中顯示清明
        }

// 如果有公曆節日則顯示
        if (!solarFestival.isEmpty()) {
            displayText += "\n公曆節日: " + solarFestival;
        } else {
            displayText += "\n公曆節日: []"; // 顯示空的公曆節日
        }


// 如果有節氣則顯示
        if (!jieQi.isEmpty()) {
            displayText += "\n節氣: " + jieQi;
        }
// 更新TextView顯示結果
        ((TextView)findViewById(R.id.dateDisplay)).setText(displayText);

    }

    // 檢查所選日期是否為今天
    private boolean isToday(int year, int month, int dayOfMonth) {
        Calendar today = Calendar.getInstance();
        return (year == today.get(Calendar.YEAR)) &&
                (month == today.get(Calendar.MONTH)) &&
                (dayOfMonth == today.get(Calendar.DAY_OF_MONTH));
    }

    // 標記為已簽到 TODO 請改成調用TaskManager的方法
    private void markAsCheckedIn(String date) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid(); // 獲取當前用戶的 UID
            // 獲取當前時間
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
            String checkTime = String.format("%02d:%02d:%02d", hour, minute, second); // 轉換成 "HH:mm:ss" 格式

            // 建立簽到資料
            Map<String, Object> checkInData = new HashMap<>();
            checkInData.put("date", date); // 簽到日期
            checkInData.put("timestamp", FieldValue.serverTimestamp()); // 伺服器時間戳記
            checkInData.put("status", "已簽到"); // 簽到狀態
            checkInData.put("check_time", checkTime); // 簽到時間

            // 將資料寫入 Firestore
            DocumentReference userRef = FirebaseFirestore.getInstance().collection("dailyCheckIns").document(userId);
            // 更新文檔內的某一天的簽到紀錄
            userRef.update("dailyCheckIns." + date, checkInData)  // 使用 'dailyCheckIns.日期' 作為欄位
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CheckIn.this, "簽到成功", Toast.LENGTH_SHORT).show();
                        button.setEnabled(false);
                        //updateTaskStatus(userId, date); // 更新每日任務狀態
                    })
                    .addOnFailureListener(e -> {
                        if (e.getMessage().contains("No document to update")) {
                            // 如果文檔不存在，則創建該文檔並新增簽到紀錄
                            Map<String, Object> newUserData = new HashMap<>();
                            newUserData.put("dailyCheckIns." + date, checkInData);  // 使用 'dailyCheckIns.日期'

                            userRef.set(newUserData)
                                    .addOnSuccessListener(aVoid1 -> {
                                        //Toast.makeText(Ca.this, "文檔創建並簽到成功", Toast.LENGTH_SHORT).show();
                                        //updateTaskStatus(userId, date); // 更新每日任務狀態
                                    })
                                    .addOnFailureListener(e1 -> {
                                        //Toast.makeText(Ca.this, "簽到失敗: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(CheckIn.this, "簽到失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(CheckIn.this, "用戶未登入", Toast.LENGTH_SHORT).show();
        }
    }
    //  檢查是否已簽到 TODO 請改成調用TaskManager的方法
    private void checkIfAlreadyCheckedIn(String date) {
        //FirebaseUser currentUser = auth.getCurrentUser();
        //String userId = currentUser.getUid();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String documentId = userId + "_" + date;
            Log.d("CheckIn", "Checking document: " + documentId);
            FirebaseFirestore.getInstance().collection("DailyCheckIn").document(documentId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Toast.makeText(CheckIn.this, "今天已經簽到過", Toast.LENGTH_SHORT).show();
                    button.setEnabled(false);
                } else {
                    markAsCheckedIn(date);
                    button.setEnabled(true); // 如果沒有簽到過且是今天，按鈕啟用
                }
            }).addOnFailureListener(e -> Toast.makeText(CheckIn.this, "檢查失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
        }

    }

    private void markDailyCheckIn() {
        /* TODO 請更新
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = "your_user_id";
            long currentTime = System.currentTimeMillis();  // 獲取當前時間的時間戳
            String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentTime);  // 格式化當前日期為 "yyyyMMdd"
            // 獲取指定用戶的 TaskStatus 集合中的文檔引用
            DocumentReference taskStatusRef = db.collection("Users").document(userId)
                    .collection("TaskStatus").document(formattedDate);

        }*/
    }

    private void addTaskStatusForToday() {
        //TODO 請改成調用TaskManager的方法
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        Long currentDate = System.currentTimeMillis();
        String formattedDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDate);
        // 獲取用戶文檔引用
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("Users").document(userId);
        // 獲取 TaskStatus 集合的引用
        CollectionReference taskStatusRef = userRef.collection("TaskStatus");
        // 初始化 is_done 和 task_name 陣列
        List<Boolean> isDone = Arrays.asList(false, true, false, false, false, false, false); // 初始狀態全部設為 false
        List<String> taskNames = Arrays.asList("每日健走150步", "每日簽到", "用藥提醒查看", "今日已完成用藥", "觀看運動影片", "玩遊戲(防失智)", "查看運動挑戰");
        // 建立存入的數據
        Map<String, Object> taskStatusData = new HashMap<>();
        taskStatusData.put("is_done", isDone); // 任務完成狀態
        taskStatusData.put("task_name", taskNames); // 任務名稱

        // 新增子集合，這裡我們新增一個文檔，名稱為當前日期格式
        taskStatusRef.document(formattedDate).set(taskStatusData)
                .addOnSuccessListener(aVoid -> Log.d("FireStore", "Task status successfully added for date: " + formattedDate))
                .addOnFailureListener(e -> Log.w("FireStore", "Error adding task status", e));
        // 簽到並獲得積分，然後禁用按鈕
        TaskManager taskManager = new TaskManager(FirebaseFirestore.getInstance(), userId);
        //TaskManager taskManager = new TaskManager(FirebaseFirestore.getInstance(), "your_user_id");
        taskManager.checkAndCompleteTask(":CheckIn", result -> {
            if (!result) {
                Log.d("FireStore", "ChallengeCompleted not completed yet.");
                taskManager.updateTaskStatusForSteps(1);
                taskManager.markTaskAsCompleted(":CheckIn");
                // 禁用按鈕
                button.setEnabled(false);
            }
            else Log.d("FireStore", "ChallengeCompleted already completed for today.");
            button.setEnabled(false); // 如果已簽到，禁用按鈕
        });

    }

//    public class SomeOtherInterface {
//        private TaskManager taskManager;
//
//        public SomeOtherInterface() {
//            // Initialize Firebase Firestore
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            String userId = "your_user_id"; // Replace with actual user ID
//
//            // Initialize TaskManager
//            taskManager = new TaskManager(db, userId);
//        }
//
//        public void checkTaskStatus(String taskType) {
//            taskManager.isTaskCompletedToday(taskType, new TaskManager.OnTaskCheckCompleteListener() {
//                @Override
//                public void onComplete(boolean result) {
//                    if (result) {
//                        // Task is completed
//                        System.out.println(taskType + " is completed for today.");
//                    } else {
//                        // Task is not completed
//                        System.out.println(taskType + " is not completed for today.");
//                    }
//                }
//            });
//        }
//    }
}
