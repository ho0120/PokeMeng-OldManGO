package com.PokeMeng.OldManGO.Location;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.PokeMeng.OldManGO.Personal.Personal;
import com.PokeMeng.OldManGO.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MapMainActivity extends AppCompatActivity {
    FirebaseFirestore firestore;
    FirebaseDatabase database;

    private String destinationAddress; // 儲存Firebase中取得的地址
    private FusedLocationProviderClient fusedLocationClient;

    String latitudeOne = "22.6581";  // 指定的緯度
    String longititudeOne = "120.5115";  // 指定的經度
    String latitudeTwo = "22.6581";  // 另一個位置的緯度
    String longititudeTwo = "120.5115";  // 另一個位置的經度
    double currentLatitude;
    double currentLongitude;
    private String userId;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_main);


        MaterialButton directionOneBtn = findViewById(R.id.directionOneBtn);


        database=FirebaseDatabase.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // 初始化 FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 取得使用者填寫的地址
        fetchUserAddressFromFirebase();

        // 請求位置權限並取得當前位置
        requestLocationPermission();



        directionOneBtn.setOnClickListener(v -> {
            if (currentLatitude != 0 && currentLongitude != 0) {
                directionFromCurrentMap(latitudeOne, longititudeOne);
            } else {
                Toast.makeText(MapMainActivity.this, "無法取得當前位置", Toast.LENGTH_SHORT).show();
            }
        });


        Button button6 = findViewById(R.id.historybutton);
        button6.setOnClickListener(v -> fetchLocationHistory());

        // 自動加載最後的位置
        fetchLastLocation();
        // 初始化 Firebase Authentication
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid(); // 獲取當前使用者的 UID
        } else {
            Toast.makeText(this, "未登入，請重新登入", Toast.LENGTH_SHORT).show();
            finish(); // 返回登入畫面或結束當前活動
        }

        // 其他初始化代碼
        database = FirebaseDatabase.getInstance();
        firestore = FirebaseFirestore.getInstance();

        requestLocationPermission();
    }



    private void fetchLocationHistory() {
        if (userId == null) {
            Toast.makeText(this, "無法取得使用者 UID", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("Users").document(userId).collection("LocationHistory")
                .orderBy("timestamp")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> locationHistory = new ArrayList<>();
                        for (var document : task.getResult()) {
                            Double latitude = document.getDouble("latitude");
                            Double longitude = document.getDouble("longitude");
                            Long timestamp = document.getLong("timestamp");

                            if (latitude != null && longitude != null && timestamp != null) {
                                String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(new Date(timestamp));
                                String locationRecord = "緯度: " + latitude + ", 經度: " + longitude + " (時間: " + dateTime + ")";
                                locationHistory.add(locationRecord);
                            }
                        }

                        ListView locationHistoryListView = findViewById(R.id.locationHistoryListView);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MapMainActivity.this, android.R.layout.simple_list_item_1, locationHistory);
                        locationHistoryListView.setAdapter(adapter);

                        if (locationHistory.isEmpty()) {
                            Toast.makeText(MapMainActivity.this, "沒有歷史位置紀錄", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MapMainActivity.this, "讀取 Firestore 資料失敗", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 從 Firebase 提取最新位置
    private void fetchLastLocation() {
//        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
//        String userId = "elderly_user_id";  // 假設這是老人的 ID
//
//        mDatabase.child("locations").child(userId)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        if (dataSnapshot.exists()) {
//                            // 提取緯度、經度和時間戳
//                            double latitude = dataSnapshot.child("latitude").getValue(Double.class);
//                            double longitude = dataSnapshot.child("longitude").getValue(Double.class);
//                            long timestamp = dataSnapshot.child("timestamp").getValue(Long.class);
//
//                            // 將時間戳轉換為可讀的格式
//                            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//                                    .format(new Date(timestamp));
//
//                            // 顯示位置和時間
//                            Toast.makeText(MapMainActivity.this, "最後位置: " + latitude + ", " + longitude + "\n時間: " + dateTime, Toast.LENGTH_LONG).show();
//                        } else {
//                            Toast.makeText(MapMainActivity.this, "無法取得位置紀錄", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                        Toast.makeText(MapMainActivity.this, "讀取失敗", Toast.LENGTH_SHORT).show();
//                    }
//                });
        String userId = "elderly_user_id";  // 假設這是老人的 ID

        // 從 Firestore 取得歷史位置紀錄
        firestore.collection("Users").document(userId).collection("LocationHistory")
                .orderBy("timestamp")  // 根據時間排序
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> locationHistory = new ArrayList<>();
                        for (var document : task.getResult()) {
                            Double latitude = document.getDouble("latitude");
                            Double longitude = document.getDouble("longitude");
                            Long timestamp = document.getLong("timestamp");

                            if (latitude != null && longitude != null && timestamp != null) {
                                // 將時間戳轉換為可讀的格式
                                String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(new Date(timestamp));
                                String locationRecord = "緯度: " + latitude + ", 經度: " + longitude + " (時間: " + dateTime + ")";
                                locationHistory.add(locationRecord);
                            }
                        }

                        // 將歷史紀錄顯示在 ListView 中
                        ListView locationHistoryListView = findViewById(R.id.locationHistoryListView);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MapMainActivity.this, android.R.layout.simple_list_item_1, locationHistory);
                        locationHistoryListView.setAdapter(adapter);

                        if (locationHistory.isEmpty()) {
                            Toast.makeText(MapMainActivity.this, "沒有歷史位置紀錄", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MapMainActivity.this, "讀取 Firestore 資料失敗", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 儲存位置到 Firebase
    private void saveLocationToFirebase(double latitude, double longitude) {
//        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
//
//        // 創建一個包含位置和時間的物件
//        HashMap<String, Object> locationData = new HashMap<>();
//        locationData.put("latitude", latitude);
//        locationData.put("longitude", longitude);
//        locationData.put("timestamp", System.currentTimeMillis()); // 儲存當前的時間戳
//
//        // 儲存到指定的 "locations" 節點下，並生成唯一的key來保存每次的紀錄
//        String userId = "elderly_user_id";  // 假設這是老人的 ID
//        String recordKey = mDatabase.child("locations").child(userId).push().getKey(); // 生成唯一key
//        mDatabase.child("locations").child(userId).child(recordKey).setValue(locationData)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(MapMainActivity.this, "位置已儲存", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(MapMainActivity.this, "無法儲存位置", Toast.LENGTH_SHORT).show();
//                    }
//                });
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        // 創建一個包含位置和時間的物件
//        HashMap<String, Object> locationData = new HashMap<>();
//        locationData.put("latitude", latitude);
//        locationData.put("longitude", longitude);
//        locationData.put("timestamp", System.currentTimeMillis()); // 儲存當前的時間戳
//
//        // 儲存到 Firebase Realtime Database
//        String userId = "elderly_user_id";  // 假設這是老人的 ID
//        String recordKey = mDatabase.child("locations").child(userId).push().getKey(); // 生成唯一key
//        mDatabase.child("locations").child(userId).child(recordKey).setValue(locationData)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(MapMainActivity.this, "位置已儲存到 Realtime Database", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(MapMainActivity.this, "無法儲存到 Realtime Database", Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//        // 同時儲存到 Firestore
//        firestore.collection("Users").document(userId).collection("LocationHistory").document(recordKey)
//                .set(locationData, SetOptions.merge()) // 使用 merge 防止覆蓋其他資料
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(MapMainActivity.this, "位置已儲存到 Firestore", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(MapMainActivity.this, "無法儲存到 Firestore", Toast.LENGTH_SHORT).show();
//                    }
//                });
        // 建立 Firestore 實例
//        String userId = "elderly_user_id";  // 假設這是老人的 ID
//        String recordKey = firestore.collection("Users").document(userId).collection("LocationHistory").document().getId(); // 生成唯一的紀錄 ID
//
//        // 建立位置資料物件
//        HashMap<String, Object> locationData = new HashMap<>();
//        locationData.put("latitude", latitude);
//        locationData.put("longitude", longitude);
//        locationData.put("timestamp", System.currentTimeMillis());
//
//        // 儲存到 Firestore 的 Users 集合下
//        firestore.collection("Users").document(userId).collection("LocationHistory").document(recordKey)
//                .set(locationData)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(MapMainActivity.this, "位置已儲存到 Firestore", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(MapMainActivity.this, "無法儲存到 Firestore", Toast.LENGTH_SHORT).show();
//                    }
//                });
        if (userId == null) {
            Toast.makeText(this, "無法取得使用者 UID", Toast.LENGTH_SHORT).show();
            return;
        }

        String recordKey = firestore.collection("Users").document(userId).collection("LocationHistory").document().getId();
        HashMap<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("timestamp", System.currentTimeMillis());

        firestore.collection("Users").document(userId).collection("LocationHistory").document(recordKey)
                .set(locationData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MapMainActivity.this, "位置已儲存到 Firestore", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MapMainActivity.this, "無法儲存到 Firestore", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 取得當前位置
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();

                // 儲存到 Firebase
                saveLocationToFirebase(currentLatitude, currentLongitude);

                Toast.makeText(MapMainActivity.this, "當前位置：" + currentLatitude + ", " + currentLongitude, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MapMainActivity.this, "無法取得當前位置", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 請求位置權限
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 如果權限已經被授予，直接取得位置
            getCurrentLocation();
        }
    }

    // 處理權限請求結果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 如果權限授予，取得位置
                getCurrentLocation();
            } else {
                Toast.makeText(this, "需要位置權限才能繼續", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 從 Firebase 獲取使用者地址
    private void fetchUserAddressFromFirebase() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").orderByKey().limitToLast(1)  // 假設要取得最新填寫的地址
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Personal user = snapshot.getValue(Personal.class);
                            if (user != null) {
                                destinationAddress = user.getAddress();  // 獲取地址
                                Toast.makeText(MapMainActivity.this, "地址已載入: " + destinationAddress, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(MapMainActivity.this, "無法載入地址", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 在 Google 地圖上定位地點
    private void pinLocationMap(String latitude, String longititude) {
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longititude + "?q=" + latitude + "," + longititude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "無法開啟地圖", Toast.LENGTH_SHORT).show();
        }
    }

    // 從當前位置導航到另一個地點
    private void directionFromCurrentMap(String latitude, String longititude) {
        Uri mapUri = Uri.parse("https://maps.google.com/maps?saddr=" + currentLatitude + "," + currentLongitude + "&daddr=" + destinationAddress);
        Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
        startActivity(intent);
    }
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        if (!dataSnapshot.exists()) {
            Toast.makeText(MapMainActivity.this, "無位置紀錄", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> locationHistory = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            double latitude = snapshot.child("latitude").getValue(Double.class);
            double longitude = snapshot.child("longitude").getValue(Double.class);
            long timestamp = snapshot.child("timestamp").getValue(Long.class);
            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));

            String locationRecord = "緯度: " + latitude + ", 經度: " + longitude + " (時間: " + dateTime + ")";
            locationHistory.add(locationRecord);
        }

        ListView locationHistoryListView = findViewById(R.id.locationHistoryListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MapMainActivity.this, android.R.layout.simple_list_item_1, locationHistory);
        locationHistoryListView.setAdapter(adapter);
    }

}
