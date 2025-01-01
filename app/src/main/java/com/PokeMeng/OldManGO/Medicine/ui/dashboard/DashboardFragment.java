package com.PokeMeng.OldManGO.Medicine.ui.dashboard;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import com.PokeMeng.OldManGO.Medicine.SharedViewModelFactory;
import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.Medicine.AlarmReceiver;
import com.PokeMeng.OldManGO.Medicine.Medicine;
import com.PokeMeng.OldManGO.Medicine.MedicineAdapter;
import com.PokeMeng.OldManGO.Medicine.ui.SharedViewModel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class DashboardFragment extends Fragment {
    private SharedViewModel viewModel;
    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private TextView textView11;
    private AlarmManager alarmManager;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.m_fragment_dashboard, container, false);

        // 创建 SavedStateHandle 实例
        SavedStateHandle savedStateHandle = new SavedStateHandle();
        // 创建 SharedViewModel 并传递 Context 和 SavedStateHandle
        viewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(requireActivity(), savedStateHandle)).get(SharedViewModel.class);

        recyclerView = root.findViewById(R.id.eat);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        textView11 = root.findViewById(R.id.textView11);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        textView11.setText(currentDate);

        // 初始化适配器
        adapter = new MedicineAdapter(getContext(), new ArrayList<>(), medicine -> {
            viewModel.addClickedMedicineFromDashboard(medicine);
            viewModel.addClickedMedicineId(medicine.getId());
            markMedicineAsTaken(medicine);
        }, true, false,false, viewModel);
        recyclerView.setAdapter(adapter);

        // 观察药品数据
        viewModel.getMedicines().observe(getViewLifecycleOwner(), medicines -> {
            Log.d("DashboardFragment", "Received medicines: " + medicines.size());
            List<Medicine> todayMedicines = getTodayMedicines(medicines);
            List<Medicine> flattenedMedicines = flattenMedicines(todayMedicines);
            adapter.updateMedicines(flattenedMedicines);
            Log.d("DashboardFragment", "Adapter item count: " + adapter.getItemCount());

            for (Medicine medicine : flattenedMedicines) {
                setAlarmForMedicine(medicine);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!requireContext().getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        return root;
    }


    private List<Medicine> getTodayMedicines(List<Medicine> medicines) {
        List<Medicine> todayMedicines = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK);

        for (Medicine medicine : medicines) {
            String frequency = medicine.getFrequency();
            String startDate = medicine.getStartDate();

            if (shouldTakeToday(frequency, today, startDate) && startDate != null) {
                int interval = extractInterval(frequency);
                if (isTodayInInterval(interval, startDate)) {
                    todayMedicines.add(medicine);
                }
            }
        }
        return todayMedicines;
    }

    private boolean shouldTakeToday(String frequency, int today, String startDate) {
        if (frequency.contains("根據需要用藥")) {
            return true;
        }
        // 判断今天是否在频率字符串中
        if (frequency.contains(String.valueOf(getDayOfWeekChar(today)))) {
            return true;
        }
        if (frequency.startsWith("每")) {
            int interval = extractInterval(frequency);
            return isTodayInInterval(interval, startDate);
        }
        return false;
    }

    private char getDayOfWeekChar(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY: return '日';
            case Calendar.MONDAY: return '一';
            case Calendar.TUESDAY: return '二';
            case Calendar.WEDNESDAY: return '三';
            case Calendar.THURSDAY: return '四';
            case Calendar.FRIDAY: return '五';
            case Calendar.SATURDAY: return '六';
            default: return ' ';
        }
    }

    private int extractInterval(String frequency) {
        String[] parts = frequency.split(" ");
        if (parts.length < 2) {
            return 1;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private boolean isTodayInInterval(int interval, String startDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date start = sdf.parse(startDate);
            long diff = new Date().getTime() - start.getTime();
            long daysSinceStart = diff / (1000 * 60 * 60 * 24);
            return (daysSinceStart >= 0) && (daysSinceStart % interval) == 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<Medicine> flattenMedicines(List<Medicine> medicines) {
        List<Medicine> flattenedList = new ArrayList<>();

        for (Medicine medicine : medicines) {
            for (String time : medicine.getTimes()) {
                flattenedList.add(new Medicine(
                        medicine.getName(),
                        medicine.getFrequency(),
                        new ArrayList<>(Arrays.asList(time)),
                        medicine.getDosage(),
                        medicine.getStock(),
                        medicine.getStock2(),
                        medicine.getImageUrl(),
                        medicine.getSpinner2Value(),
                        medicine.getId(),
                        medicine.getStartDate()
                ));
            }
        }

        // 对 flattenedList 按时间排序
        SimpleDateFormat sdf = new SimpleDateFormat("aa hh:mm", Locale.CHINESE);
        flattenedList.sort((medicine1, medicine2) -> {
            try {
                String time1 = medicine1.getTimes().get(0);
                String time2 = medicine2.getTimes().get(0);
                Date date1 = sdf.parse(time1);
                Date date2 = sdf.parse(time2);
                return date1.compareTo(date2); // 按时间升序排序
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });
        Log.d("DashboardFragment", "Flattened medicines: " + flattenedList.toString());
        return flattenedList;
    }

    private void setAlarmForMedicine(Medicine medicine) {
        Set<String> addedTimes = new HashSet<>();

        for (String time : medicine.getTimes()) {
            if (!addedTimes.contains(time)) {
                SimpleDateFormat sdf = new SimpleDateFormat("aa hh:mm", Locale.CHINESE);
                Calendar calendar = Calendar.getInstance();
                try {
                    Date date = sdf.parse(time);
                    calendar.setTime(date);
                    calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

                    // 如果设置的时间在当前时间之前，则推迟到第二天
                    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    Intent intent = new Intent(getContext(), AlarmReceiver.class);
                    intent.putExtra("medicine_name", medicine.getName());
                    intent.putExtra("medicine_time", time);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            getContext(),
                            (medicine.getName() + time).hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    addedTimes.add(time);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void markMedicineAsTaken(Medicine medicine) {
        viewModel.addTakenMedicine(medicine);
        // 其他逻辑可以根据需求添加
    }



}