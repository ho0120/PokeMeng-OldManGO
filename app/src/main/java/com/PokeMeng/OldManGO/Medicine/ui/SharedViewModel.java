package com.PokeMeng.OldManGO.Medicine.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.SavedStateHandle;

import com.PokeMeng.OldManGO.Medicine.Medicine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;
import java.util.Set;

public class SharedViewModel extends ViewModel {

    private final SavedStateHandle stateHandle;
    private final MutableLiveData<List<Medicine>> takenMedicines = new MutableLiveData<>(new ArrayList<>());
    private final Map<String, List<Medicine>> medicinesByDate = new HashMap<>();
    private final MutableLiveData<List<Medicine>> historyMedicines = new MutableLiveData<>(new ArrayList<>());
    private final List<Integer> clickedMedicineIds = new ArrayList<>();
    private final MutableLiveData<List<Medicine>> clickedMedicinesFromDashboard = new MutableLiveData<>(new ArrayList<>());

    private final DatabaseReference databaseReference; // Firebase Database reference

    private String userId;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final Context context;

    private final FirebaseFirestore firestore;

    public SharedViewModel(SavedStateHandle stateHandle, Context context) {
        this.stateHandle = stateHandle;
        this.context = context;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            this.userId = user.getUid(); // 获取当前用户 UID
            this.databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        } else {
            throw new IllegalStateException("User not authenticated");
        }

        this.firestore = FirebaseFirestore.getInstance(); // 初始化 Firestore

        sharedPreferences = context.getSharedPreferences("MedicinePrefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        List<Medicine> savedMedicines = loadMedicineData();
        if (savedMedicines.isEmpty()) {
            loadMedicinesFromFirestore(); // 如果没有本地数据，从 Firestore 加载
        } else {
            stateHandle.set("medicines", savedMedicines); // 设置 LiveData
        }

        List<Medicine> savedHistoryMedicines = loadHistoryMedicines();
        if (!savedHistoryMedicines.isEmpty()) {
            historyMedicines.setValue(savedHistoryMedicines);
        }
    }


    public LiveData<List<Medicine>> getMedicines() {
        Log.d("SharedViewModel", "getMedicines() called");
        return stateHandle.getLiveData("medicines", new ArrayList<>());
    }


    // 添加药物
    public void addMedicine(Medicine medicine) {
        String recordKey = String.valueOf(medicine.getId());

        // 保存到 Firestore
        firestore.collection("Users")
                .document(userId)
                .collection("medicines")
                .document(recordKey)
                .set(medicine)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Medicine added successfully");

                    // 更新 LiveData 和 SharedPreferences
                    List<Medicine> updatedList = getMedicines().getValue();
                    if (updatedList != null) {
                        updatedList.add(medicine);
                        stateHandle.set("medicines", updatedList);
                        saveMedicineData(updatedList);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding medicine", e));
    }


    // 更新药物信息
    public void updateMedicine(Medicine medicine) {
        String recordKey = String.valueOf(medicine.getId());

        // 更新 Firestore 数据
        firestore.collection("Users")
                .document(userId)
                .collection("medicines")
                .document(recordKey)
                .set(medicine)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Medicine updated successfully");

                    // 更新 LiveData 和 SharedPreferences
                    List<Medicine> currentMedicines = getMedicines().getValue();
                    if (currentMedicines != null) {
                        for (int i = 0; i < currentMedicines.size(); i++) {
                            if (currentMedicines.get(i).getId() == medicine.getId()) {
                                currentMedicines.set(i, medicine);
                                stateHandle.set("medicines", currentMedicines);
                                saveMedicineData(currentMedicines);
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating medicine", e));
    }



    // 删除药物
    public void removeMedicine(int id) {
        String recordKey = String.valueOf(id);

        // 从 Firestore 删除
        firestore.collection("Users")
                .document(userId)
                .collection("medicines")
                .document(recordKey)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Medicine deleted successfully");

                    // 更新 LiveData 和 SharedPreferences
                    List<Medicine> currentMedicines = getMedicines().getValue();
                    if (currentMedicines != null) {
                        currentMedicines.removeIf(medicine -> medicine.getId() == id);
                        stateHandle.set("medicines", currentMedicines);
                        saveMedicineData(currentMedicines);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting medicine", e));
    }


    // 添加药品到历史记录
    public void addToHistory(Medicine medicine) {
        List<Medicine> currentList = historyMedicines.getValue();
        if (currentList != null) {
            currentList.add(medicine);
            historyMedicines.setValue(currentList);
            saveHistoryMedicines(currentList); // 保存历史药物数据到 SharedPreferences

            // 添加日志记录添加的药物
            Log.d("SharedViewModel", "Added to history: " + medicine.getName());
        }
    }



    public void setMedicines(List<Medicine> medicines) {
        Set<Medicine> uniqueMedicines = new HashSet<>(medicines);
        stateHandle.set("medicines", new ArrayList<>(uniqueMedicines));

    }



    // 获取历史药品
    public LiveData<List<Medicine>> getHistoryMedicines() {
        Log.d("SharedViewModel", "Fetching history medicines");
        return historyMedicines;
    }

    // 从 Firebase 加载数据后保存到 SharedPreferences
    public void loadMedicinesFromFirestore() {
        firestore.collection("Users")
                .document(userId)
                .collection("medicines")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Medicine> medicines = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Medicine medicine = document.toObject(Medicine.class);
                            if (medicine != null) {
                                medicines.add(medicine);
                            }
                        }
                        // 更新 LiveData 和 SharedPreferences
                        stateHandle.set("medicines", medicines);
                        saveMedicineData(medicines);
                    } else {
                        Log.e("Firestore", "Error getting medicines", task.getException());
                    }
                });
    }


    // 保存药品信息到 SharedPreferences
    // 保存药品信息到 SharedPreferences
    public void saveMedicineData(List<Medicine> medicineList) {
        Gson gson = new Gson();
        String json = gson.toJson(medicineList);
        editor.putString("medicine_data", json);

        // 保存 medicinesByDate
        String medicinesByDateJson = gson.toJson(medicinesByDate);
        editor.putString("medicines_by_date_data", medicinesByDateJson);

        editor.apply();
    }


    // 从 SharedPreferences 中加载药品信息
    // 从 SharedPreferences 中加载药品信息
    public List<Medicine> loadMedicineData() {
        String json = sharedPreferences.getString("medicine_data", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Medicine>>() {}.getType();
            List<Medicine> medicines = gson.fromJson(json, type);

            // 加载 medicinesByDate
            String medicinesByDateJson = sharedPreferences.getString("medicines_by_date_data", null);
            if (medicinesByDateJson != null) {
                Type mapType = new TypeToken<Map<String, List<Medicine>>>() {}.getType();
                Map<String, List<Medicine>> loadedMedicinesByDate = gson.fromJson(medicinesByDateJson, mapType);
                medicinesByDate.putAll(loadedMedicinesByDate);

            }

            return medicines;
        }
        return new ArrayList<>();
    }


    // 设置点击的日期
    public void setClickedDate(Date date) {
        stateHandle.set("clicked_date", date);
    }

    // 获取点击的日期
    public LiveData<Date> getClickedDate() {
        return stateHandle.getLiveData("clicked_date");
    }

    // 添加点击的药物 ID
    public void addClickedMedicineId(int id) {
        if (!clickedMedicineIds.contains(id)) {
            clickedMedicineIds.add(id);
        }
    }

    // 获取点击的药物 ID 列表
    public List<Integer> getClickedMedicineIds() {
        return clickedMedicineIds;
    }

    // 添加点击的药物到从 Dashboard 点击的药物列表
    public void addClickedMedicineFromDashboard(Medicine medicine) {
        List<Medicine> currentClickedMedicines = clickedMedicinesFromDashboard.getValue();
        if (currentClickedMedicines != null) {
            currentClickedMedicines.add(medicine);
            clickedMedicinesFromDashboard.setValue(currentClickedMedicines);
        }
    }

    // 获取从 Dashboard 点击的药物列表
    public LiveData<List<Medicine>> getClickedMedicinesFromDashboard() {
        return clickedMedicinesFromDashboard;
    }

    // 记录已服用药物
    public void addTakenMedicine(Medicine medicine) {
        List<Medicine> currentTaken = takenMedicines.getValue();
        if (currentTaken == null) {
            currentTaken = new ArrayList<>();
        }

        // 记录服用日期
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        medicine.setTakenDate(currentDate); // 假设 Medicine 类有 setTakenDate() 方法
        currentTaken.add(medicine);
        takenMedicines.setValue(currentTaken); // 更新已服用药品列表

        // 将药物添加到历史记录
        List<Medicine> currentHistory = historyMedicines.getValue();
        if (currentHistory == null) {
            currentHistory = new ArrayList<>();
        }
        currentHistory.add(medicine);
        historyMedicines.setValue(currentHistory); // 更新 LiveData
        saveHistoryMedicines(currentHistory); // 保存到 SharedPreferences

        Log.d("SharedViewModel", "Added to history: " + medicine.getName());

        // 更新药物按日期记录
        medicinesByDate.putIfAbsent(currentDate, new ArrayList<>());
        medicinesByDate.get(currentDate).add(medicine);

        // 记录点击的药物 ID
        addClickedMedicineId(medicine.getId());
        // 保存所有数据
        saveMedicineData(stateHandle.get("medicines"));

    }


    // 获取已服用药物列表
    public LiveData<List<Medicine>> getTakenMedicines() {
        return takenMedicines;
    }


    // 根据日期获取药物列表
    public LiveData<List<Medicine>> getMedicinesByDate(String date) {
        MutableLiveData<List<Medicine>> liveData = new MutableLiveData<>();
        liveData.setValue(medicinesByDate.getOrDefault(date, new ArrayList<>()));
        return liveData;
    }

    public void saveHistoryMedicines(List<Medicine> historyList) {
        Log.d("SharedViewModel", "Saving history medicines: " + historyList.size());
        Gson gson = new Gson();
        String json = gson.toJson(historyList);
        editor.putString("history_medicine_data", json);
        editor.apply();
        Log.d("SharedViewModel", "History medicines saved successfully.");
    }


    public List<Medicine> loadHistoryMedicines() {
        String json = sharedPreferences.getString("history_medicine_data", null);
        Log.d("SharedViewModel", "Loading history medicines: " + (json != null ? "Found" : "Not found"));
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Medicine>>() {}.getType();
            List<Medicine> historyMedicines = gson.fromJson(json, type);
            Log.d("SharedViewModel", "Loaded history medicines count: " + historyMedicines.size());
            return historyMedicines;
        }
        return new ArrayList<>();
    }


}