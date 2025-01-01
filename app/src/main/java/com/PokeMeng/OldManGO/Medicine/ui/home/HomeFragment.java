package com.PokeMeng.OldManGO.Medicine.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.PokeMeng.OldManGO.Medicine.MainActivity5;
import com.PokeMeng.OldManGO.Medicine.SharedViewModelFactory;
import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.Medicine.MedicineAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.PokeMeng.OldManGO.Medicine.Medicine;
import com.PokeMeng.OldManGO.Medicine.ui.SharedViewModel;
import com.PokeMeng.OldManGO.TaskManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private MedicineAdapter medicineAdapter;
    private List<Medicine> medicines = new ArrayList<>();
    private TextView textView;
    private SharedViewModel viewModel;
    private MedicineAdapter adapter;
    private MainActivity5 activity;




    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.m_fragment_home, container, false);



        // 创建 SavedStateHandle 实例
        SavedStateHandle savedStateHandle = new SavedStateHandle();
        activity = (MainActivity5) getActivity();
        // 创建 SharedViewModel 并传递 Context 和 SavedStateHandle
        viewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(requireActivity(), savedStateHandle)).get(SharedViewModel.class);
        textView = view.findViewById(R.id.textView); // 确保获取 TextView
        updateEmptyView(textView);

        // 初始化 RecyclerView 和 Adapter
        recyclerView = view.findViewById(R.id.recyclerViewMedicines);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 加载数据
        loadMedicinesFromSharedPreferences();
        loadMedicinesFromFirebase();

        // 初始化适配器
        medicineAdapter = new MedicineAdapter(getContext(), medicines, new MedicineAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Medicine medicine) {
                navigateToBlankFragment(medicine);
            }
        }, false, false,true, viewModel);
        recyclerView.setAdapter(medicineAdapter);

        // 在观察药物列表的变化时调用
        viewModel.getMedicines().observe(getViewLifecycleOwner(), medicines -> {
            if (medicines != null) {
                this.medicines = medicines; // 更新本地列表
                medicineAdapter.updateMedicines(medicines); // 更新 Adapter
                updateEmptyView(textView); // 更新空视图的显示状态
            } else {
                Log.e("HomeFragment", "Medicines list is null");
            }

        });


        // 检查 Firebase 任务是否已完成
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser.getUid();

        // 添加 TaskManager 的任务检查逻辑
        TaskManager taskManager = new TaskManager(FirebaseFirestore.getInstance(), userId);
        taskManager.checkAndCompleteTask("CheckedMedicine", result -> {
            if (!result) {
                Log.d("FireStore", "ChallengeCompleted not completed yet.");
                taskManager.updateTaskStatusForSteps(2); // 更新步骤
                taskManager.markTaskAsCompleted("CheckedMedicine"); // 标记为完成
            } else {
                Log.d("FireStore", "ChallengeCompleted already completed for today.");
            }
        });



        medicineAdapter.setOnStockUpdateListener(medicine -> {
            // 更新 Firebase 中的库存值
            updateStockInFirebase(medicine);
        });



        // 点击 header_section 来新增药物
        view.findViewById(R.id.header_section).setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main5);
            navController.navigate(R.id.action_homeFragment_to_blankFragment);
        });

        // 设置 FragmentResultListener 来接收编辑结果
        getParentFragmentManager().setFragmentResultListener("editResult", this, (requestKey, bundle) -> {
            String updatedName = bundle.getString("updatedName");
            String updatedFrequency = bundle.getString("updatedFrequency");
            ArrayList<String> updatedTimes = bundle.getStringArrayList("updatedTimes");
            String updatedDosage = bundle.getString("updatedDosage");
            int updatedStock = bundle.getInt("updatedStock");
            int updatedStock2 = bundle.getInt("updatedStock2");
            String updatedSpinner2Value = bundle.getString("updatedSpinner2Value");
            int medicineId = bundle.getInt("medicineId", -1);
            String updatedStartDate = bundle.getString("updatedStartDate");

            if (isInputValid(updatedName, updatedFrequency, updatedTimes, updatedDosage, updatedStock, updatedStock2)) {
                if (medicineId != -1) {
                    updateRecyclerView(updatedName, updatedFrequency, updatedTimes, updatedDosage, updatedStock, updatedStock2, updatedSpinner2Value, medicineId, updatedStartDate);
                } else {
                    Log.e("HomeFragment", "Medicine ID is invalid");
                }
            } else {
                Toast.makeText(getContext(), "請填寫所有字段", Toast.LENGTH_SHORT).show();
            }


        });

        getParentFragmentManager().setFragmentResultListener("deleteMedicineResult", this, (requestKey, bundle) -> {
            int deletedMedicineId = bundle.getInt("deletedMedicineId", -1);
            // 从 RecyclerView 中删除相应的药物项
            if (deletedMedicineId != -1) {
                deleteMedicine(deletedMedicineId); // 直接调用删除方法
            }
        });



        updateEmptyView(textView);


        return view; // 返回已初始化的视图
    }


    // 新增一个方法检查药物是否已存在
    private boolean isMedicineExist(Medicine newMedicine) {
        for (Medicine medicine : medicines) {
            if (medicine.getId() == newMedicine.getId()) {
                return true; // 药物已存在
            }
        }
        return false; // 药物不存在
    }

    private boolean isInputValid(String name, String frequency, ArrayList<String> times, String dosage, int stock, int stock2) {
        return !(name == null || name.isEmpty() ||
                frequency == null || frequency.isEmpty() ||
                times == null || times.isEmpty() ||
                dosage == null || dosage.isEmpty() ||
                stock < 0 || stock2 < 0);
    }

    public void deleteMedicine(int medicineId) {
        medicines.removeIf(medicine -> medicine.getId() == medicineId);
        medicineAdapter.notifyDataSetChanged();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("medicines").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        databaseReference.child(String.valueOf(medicineId)).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("HomeFragment", "Medicine deleted from Firebase successfully.");
            } else {
                Log.e("HomeFragment", "Failed to delete medicine from Firebase: " + task.getException().getMessage());
            }
        });
    }





    private void updateRecyclerView(String name, String frequency, ArrayList<String> times, String dosage, int stock, int stock2, String spinner2Value, int medicineId, String startDate) {
        int position = getMedicinePositionById(medicineId);
        if (position != -1) {
            Medicine existingMedicine = medicines.get(position);
            existingMedicine.setName(name);
            existingMedicine.setFrequency(frequency);
            existingMedicine.setTimes(times);
            existingMedicine.setDosage(dosage);
            existingMedicine.setStock(stock);
            existingMedicine.setStock2(stock2);
            existingMedicine.setSpinner2Value(spinner2Value);
            existingMedicine.setStartDate(startDate);

            resetButtonStates(medicineId, times);
            medicineAdapter.notifyItemChanged(position);
        }
    }



    private int getMedicinePositionById(int medicineId) {
        for (int i = 0; i < medicines.size(); i++) {
            if (medicines.get(i).getId() == medicineId) {
                return i;
            }
        }
        return -1;
    }

    // 在添加或更新药物时，确保列表中只存在唯一的药物
    private void addOrUpdateMedicine(Medicine newMedicine) {
        // 获取 Firebase 实例
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("medicines");

        // 使用 Firebase 自动生成的 ID 来检查和更新药物
        databaseReference.child(String.valueOf(newMedicine.getId())).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // 删除旧的条目
                    dataSnapshot.getRef().removeValue();
                }

                // 添加新药物到 Firebase
                databaseReference.child(String.valueOf(newMedicine.getId())).setValue(newMedicine)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // 更新本地列表
                                int position = getMedicinePositionById(newMedicine.getId());
                                if (position != -1) {
                                    medicines.set(position, newMedicine);
                                    medicineAdapter.notifyItemChanged(position);
                                } else {
                                    medicines.add(newMedicine);
                                    medicineAdapter.notifyItemInserted(medicines.size() - 1);
                                }
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("HomeFragment", "Failed to update medicine: " + databaseError.getMessage());
            }
        });
    }



    private void resetButtonStates(int medicineId, ArrayList<String> times) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("button_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String time : times) {
            editor.putBoolean("button_clicked_" + medicineId + "_" + time, false);
        }
        editor.apply();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("medicines", new ArrayList<>(medicines));
    }


    @Override
    public void onPause() {
        super.onPause();
        saveMedicinesToSharedPreferences(); // 在应用进入后台时保存数据
    }

    @Override
    public void onStop() {
        super.onStop();
        saveMedicinesToSharedPreferences(); // 在应用被停止时保存数据
    }


    private void updateEmptyView(TextView emptyView) {
        if (emptyView != null) {
            // 根据 medicines 列表的大小来决定 TextView 的可见性
            if (medicines.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE); // 显示空视图
            } else {
                emptyView.setVisibility(View.GONE); // 隐藏空视图
            }
        } else {
            Log.e("HomeFragment", "TextView not found");
        }
    }


    private void navigateToBlankFragment(Medicine medicine) {
        Log.d("HomeFragment", "Navigating with medicine ID: " + medicine.getId());
        Bundle bundle = new Bundle();
        bundle.putString("editTextText", medicine.getName());
        bundle.putString("textView3", medicine.getFrequency());
        bundle.putStringArrayList("timeContainer", medicine.getTimes());
        bundle.putString("textView9", medicine.getDosage());
        bundle.putInt("editTextNumber", medicine.getStock());
        bundle.putInt("editTextNumber2", medicine.getStock2());
        bundle.putString("imageView4", medicine.getImageUrl());
        bundle.putString("spinner2Value", medicine.getSpinner2Value());
        bundle.putString("startDate", medicine.getStartDate());
        bundle.putInt("medicineId", medicine.getId());
        bundle.putBoolean("isEdit", true);

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main5);
        navController.navigate(R.id.action_homeFragment_to_blankFragment, bundle);
    }

    private int generateUniqueId() {
        // Firebase 自动生成的 ID 作为唯一标识，不再手动生成 ID
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("medicines");
        return databaseReference.push().getKey().hashCode(); // 使用 Firebase 自动生成的 key 作为 ID 的一部分
    }


    private void saveMedicinesToSharedPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("medicine_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(medicines); // 将药品列表转换为 JSON 字符串
        editor.putString("medicines_list", json); // 保存 JSON 到 SharedPreferences
        editor.apply(); // 提交编辑
    }

    private void loadMedicinesFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("medicine_prefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("medicines_list", null);
        if (json != null) {
            Type type = new TypeToken<List<Medicine>>() {}.getType();
            try {
                medicines = gson.fromJson(json, type);
            } catch (Exception e) {
                Log.e("HomeFragment", "Failed to parse medicines", e);
                medicines = new ArrayList<>();
            }
        } else {
            medicines = new ArrayList<>();
        }

        // 更新适配器和空视图
        if (medicineAdapter != null) {
            medicineAdapter.updateMedicines(medicines);
        }
        updateEmptyView(textView); // 确保传入 TextView
    }

    // 添加 Firebase 数据加载逻辑
    private void loadMedicinesFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference medicinesRef = database.getReference("medicines").child(userId);

        medicinesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                medicines.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Medicine medicine = snapshot.getValue(Medicine.class);
                    if (medicine != null) {
                        medicines.add(medicine);
                    }
                }
                medicineAdapter.notifyDataSetChanged();
                updateEmptyView(textView);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("HomeFragment", "Failed to load medicines: " + databaseError.getMessage());
            }
        });
    }


    // 更新 Firebase 中的库存值
    private void updateStockInFirebase(Medicine medicine) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference medicineRef = database.getReference("medicines").child(userId).child(String.valueOf(medicine.getId()));

        medicineRef.child("stock2").setValue(medicine.getStock2()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("HomeFragment", "Stock updated successfully in Firebase.");
            } else {
                Log.e("HomeFragment", "Failed to update stock in Firebase: " + task.getException().getMessage());
            }
        });
    }



    @Override
    public void onResume() {
        super.onResume();
        loadMedicinesFromFirebase(); // 每次返回到此 Fragment 时重新加载数据

        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.nav_view);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }}
