package com.PokeMeng.OldManGO.Medicine;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.PokeMeng.OldManGO.Medicine.ui.SharedViewModel;
import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.databinding.MMain5Binding;
import com.google.firebase.FirebaseApp;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity5 extends AppCompatActivity {

    private MMain5Binding binding;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private static final String MEDICINE_KEY = "medicines_list";

    // 权限请求代码
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM = 1;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 2;

    private static final String SHARED_PREF_NAME = "MedicinePrefs"; // 偏好设置名称

    private SharedViewModel viewModel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MMain5Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        FirebaseApp.initializeApp(this);

        // 创建 SavedStateHandle 实例
        SavedStateHandle savedStateHandle = new SavedStateHandle();

        // 使用 SharedViewModelFactory 创建 SharedViewModel 实例
        SharedViewModelFactory factory = new SharedViewModelFactory(this, savedStateHandle); // 传递 Context 和 SavedStateHandle

        // 使用 ViewModelProvider 初始化 SharedViewModel
        viewModel = new ViewModelProvider(this, factory).get(SharedViewModel.class);



        // 这里可以监听 LiveData 数据的变化
        viewModel.getMedicines().observe(this, medicines -> {
            // 更新 UI
        });

        // 初始化 SharedPreferences 和 Gson
        sharedPreferences = getSharedPreferences("MedicinePreferences", MODE_PRIVATE);
        gson = new Gson();

        // Example: 保存药品列表
        ArrayList<Medicine> medicines = new ArrayList<>();
        saveMedicineList(medicines);

        // Example: 获取药品列表
        ArrayList<Medicine> savedMedicines = getMedicineList();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main5);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        createNotificationChannel();

        // 请求权限逻辑
        requestPermissions();

        // 处理 Intent 以打开特定 Fragment
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }


    // 保存药品列表到 SharedPreferences
    private void saveMedicineList(ArrayList<Medicine> medicines) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(medicines);
        editor.putString(MEDICINE_KEY, json);
        editor.apply(); // 异步保存数据
    }

    // 从 SharedPreferences 中获取药品列表
    private ArrayList<Medicine> getMedicineList() {
        String json = sharedPreferences.getString(MEDICINE_KEY, null);
        Type type = new TypeToken<ArrayList<Medicine>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void requestPermissions() {
        // 请求精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, REQUEST_CODE_SCHEDULE_EXACT_ALARM);
            }
        }

        // 请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("fragment")) {
            String fragmentToOpen = intent.getStringExtra("fragment");
            Log.d("MainActivity5", "Opening fragment: " + fragmentToOpen);
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main5);

            // 清除回退栈，确保导航逻辑不混淆
            navController.popBackStack(R.id.navigation_home, false); // 清除到HomeFragment的回退栈
            navController.popBackStack(R.id.navigation_dashboard, false); // 清除到DashboardFragment的回退栈

            if ("DashboardFragment".equals(fragmentToOpen)) {
                navController.navigate(R.id.navigation_dashboard);
            } else {
                navController.navigate(R.id.navigation_home);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SCHEDULE_EXACT_ALARM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity5", "允許設置鬧鐘提醒");
            } else {
                Log.d("MainActivity5", "鬧鐘提醒權限已被拒絕");
            }
        } else if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity5", "可以發送通知");
            } else {
                Log.d("MainActivity5", "通知權限已被拒絕");
            }
        }
    }

    private void createNotificationChannel() {
        Log.d("MainActivity5", "Notification channel created");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "medicine_reminder_channel",
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保每次活动恢复时都不会意外重置 Fragment
        if (getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main5) == null) {
            // 这里可以不需要再次加载 HomeFragment，Fragment 管理应该由 Navigation 控制
        }
    }

}

