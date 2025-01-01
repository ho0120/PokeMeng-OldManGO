package com.PokeMeng.OldManGO.Medicine.ui.notifications;


import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.PokeMeng.OldManGO.Medicine.MainActivity5;
import com.PokeMeng.OldManGO.Medicine.SharedViewModelFactory;
import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.databinding.MFragmentNotificationsBinding;
import com.PokeMeng.OldManGO.Medicine.Medicine;
import com.PokeMeng.OldManGO.Medicine.MedicineAdapter;

import com.PokeMeng.OldManGO.Medicine.ui.SharedViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    private MFragmentNotificationsBinding binding;
    private RecyclerView recyclerView;
    private MedicineAdapter medicineAdapter;
    private List<Medicine> medicineList = new ArrayList<>();
    private SharedViewModel viewModel;
    private Calendar selectedCalendar;
    private TextView textView12;
    private SharedPreferences sharedPreferences; // 用于保存偏好设置
    private List<Medicine> takenMedicines = new ArrayList<>();

    private MainActivity5 activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = MFragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 获取 SharedPreferences 实例
        sharedPreferences = requireContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);

        // 创建 SavedStateHandle 实例
        SavedStateHandle savedStateHandle = new SavedStateHandle();
        activity = (MainActivity5) getActivity();
        // 创建 SharedViewModel 并传递 Context 和 SavedStateHandle
        viewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(requireActivity(), savedStateHandle)).get(SharedViewModel.class);
        // 使用 getViewLifecycleOwner() 作为 LifecycleOwner
        viewModel.getMedicines().observe(getViewLifecycleOwner(), medicines -> {
            // 更新 UI
        });

        // 初始化 RecyclerView 和 Adapter
        recyclerView = root.findViewById(R.id.recyclerhistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        medicineAdapter = new MedicineAdapter(getContext(), takenMedicines, medicine -> {
            // 点击事件处理（如果需要）
        }, false, true,false, viewModel);

        recyclerView.setAdapter(medicineAdapter);

        // 观察已服用药物的 LiveData
        viewModel.getTakenMedicines().observe(getViewLifecycleOwner(), medicines -> {
            if (medicines != null) {
                takenMedicines.clear();
                takenMedicines.addAll(medicines);
                medicineAdapter.notifyDataSetChanged(); // 通知 Adapter 更新数据
            }
        });


        initializeRecyclerView();
        initializeDatePicker();
        // 加载历史药物列表
        loadHistoryMedicines();

        return root; // 确保返回的是 root，而不是新的视图
    }


    private void initializeRecyclerView() {
        recyclerView = binding.recyclerhistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        medicineAdapter = new MedicineAdapter(requireContext(), medicineList, medicine -> {
            // Handle click event (optional)
        }, false, true,false, viewModel);
        recyclerView.setAdapter(medicineAdapter);
    }

    private void initializeDatePicker() {
        textView12 = binding.textView12;
        ImageButton pickButton = binding.pickButton;
        selectedCalendar = Calendar.getInstance();

        // 默认设置 textView12 的文本为 "點選圖示可查看紀錄"
        textView12.setText("點選圖示可查看紀錄");

        // 加载保存的日期
        // loadSavedDate();

        pickButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, monthOfYear, dayOfMonth) -> {
                        selectedCalendar.set(year, monthOfYear, dayOfMonth);
                        updateDateTextView();
                        filterMedicinesBySelectedDate();
                        saveSelectedDate();
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void saveSelectedDate() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(selectedCalendar.getTime());
        Log.d("NotificationsFragment", "Saving selected date: " + formattedDate); // Debug 日志
        editor.putString("selected_date", formattedDate); // 保存选择的日期
        editor.apply();
    }

    private void loadSavedDate() {
        if (sharedPreferences == null) {
            Log.e("NotificationsFragment", "SharedPreferences is null");
            return;
        }
        String savedDate = sharedPreferences.getString("selected_date", null); // 读取保存的日期
        if (savedDate != null) {
            Log.d("NotificationsFragment", "Loaded saved date: " + savedDate); // Debug 日志
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedCalendar.setTime(sdf.parse(savedDate));
                updateDateTextView();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }


    private void updateDateTextView() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        textView12.setText(sdf.format(selectedCalendar.getTime()));
    }

    private void filterMedicinesBySelectedDate() {
        String selectedDate = textView12.getText().toString();
        Log.d("NotificationsFragment", "Filtering medicines by selected date: " + selectedDate); // 添加调试日志
        viewModel.getMedicinesByDate(selectedDate).observe(getViewLifecycleOwner(), medicines -> {
            if (medicines != null) {
                medicineList.clear();
                medicineList.addAll(medicines);
                Log.d("NotificationsFragment", "Filtered medicines count: " + medicines.size()); // 添加调试日志
                medicineAdapter.notifyDataSetChanged();
            } else {
                Log.d("NotificationsFragment", "No medicines found for the selected date");
            }
        });
    }



    private void loadHistoryMedicines() {
        viewModel.getHistoryMedicines().observe(getViewLifecycleOwner(), medicines -> {
            medicineList.clear();
            medicineList.addAll(medicines); // 确保数据被正确加载
            medicineAdapter.notifyDataSetChanged();
            Log.d("NotificationsFragment", "Loaded history medicines: " + medicines.size());
        });
    }





    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

