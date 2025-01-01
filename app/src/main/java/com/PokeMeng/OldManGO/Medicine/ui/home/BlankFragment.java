package com.PokeMeng.OldManGO.Medicine.ui.home;

import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.app.DatePickerDialog;

import com.PokeMeng.OldManGO.Medicine.Medicine;
import com.PokeMeng.OldManGO.R;
import com.PokeMeng.OldManGO.Medicine.BottomSheet;
import com.PokeMeng.OldManGO.Medicine.BottomWeekdaySheet;
import com.PokeMeng.OldManGO.Medicine.DayInterval;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.OutputStream;
import java.util.Calendar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlankFragment extends Fragment implements BottomSheet.OnFrequencySelectedListener, DayInterval.OnIntervalSelectedListener {

    private static final int PICK_IMAGE = 1;
    private ImageView photo;
    private ImageButton addTimeButton, imageButton, imageButton3;
    private GridLayout timeContainer;
    private List<TextView> timeTextViews = new ArrayList<>();
    private Spinner spinner2;
    private TextView textView9, textView3, editTextNumber, editTextNumber2, editTextText,startDateTextView;
    private Button medicineButton;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Calendar selectedStartDate;

    private int medicineId = -1; // 使用 -1 作为默认无效值

    public BlankFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.m_fragment_blank, container, false);

        // 初始化 UI 元件
        spinner2 = view.findViewById(R.id.spinner2);
        textView9 = view.findViewById(R.id.textView9);
        textView3 = view.findViewById(R.id.textView3);
        addTimeButton = view.findViewById(R.id.imageButton2);
        imageButton = view.findViewById(R.id.imageButton);
        imageButton3 = view.findViewById(R.id.imageButton3);
        timeContainer = view.findViewById(R.id.timeContainer);
        photo = view.findViewById(R.id.imageView4);
        medicineButton = view.findViewById(R.id.frequencyButton);
        editTextNumber = view.findViewById(R.id.editTextNumber);
        editTextNumber2 = view.findViewById(R.id.editTextNumber2);
        editTextText = view.findViewById(R.id.editTextText);


        startDateTextView = view.findViewById(R.id.startDateTextView); // 新增 startDateTextView
        selectedStartDate = Calendar.getInstance();
        startDateTextView.setOnClickListener(v -> showDatePickerDialog());


        // 初始化 pickImageLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                            photo.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "加載圖片失敗", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 設置 photo 的 OnClickListener
        photo.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            pickImageLauncher.launch(Intent.createChooser(intent, "選擇圖片"));
        });

        // 設置按鈕點擊事件
        medicineButton.setOnClickListener(v -> showFrequencyBottomSheet());
        addTimeButton.setOnClickListener(v -> showTimePickerDialog());

        imageButton.setOnClickListener(v -> {
            Bundle args = getArguments();
            if (args == null) {
                requireActivity().onBackPressed(); // 直接返回到 HomeFragment
                return; // 直接返回，不继续执行
            }

            new AlertDialog.Builder(getContext())
                    .setTitle("確定刪除此藥物?")
                    .setMessage("您確定要刪除此藥物嗎?")
                    .setPositiveButton("是", (dialog, which) -> {
                        int medicineId = args.getInt("medicineId"); // 获取要删除的药物 ID
                        Bundle result = new Bundle();
                        result.putInt("deletedMedicineId", medicineId); // 把药物 ID 放入 Bundle
                        getParentFragmentManager().setFragmentResult("deleteMedicineResult", result);
                        requireActivity().onBackPressed(); // 返回到 HomeFragment
                    })
                    .setNegativeButton("否", null) // 点击“否”时，什么都不做
                    .show();
        });

        imageButton3.setOnClickListener(v -> {
            Bundle bundle = getArguments(); // 确保在这里获取 arguments
            if (bundle != null && bundle.getBoolean("isEdit", false)) {
                medicineId = bundle.getInt("medicineId", -1);
                if (medicineId == -1) {
                    Log.e("BlankFragment", "Medicine ID is invalid, cannot update.");
                    Toast.makeText(getActivity(), "無法保存，藥物ID不存在", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.action_blankFragment_to_homeFragment);
                    return;
                }
                updateExistingMedicine();
            } else {
                addNewMedicine();
            }
        });

        setupSpinner();

        // 在 onCreateView 中填充資料
        Bundle bundle = getArguments();
        if (bundle != null) {
            medicineId = bundle.getInt("medicineId", -1); // 获取 int 类型的 medicineId
            if (medicineId == -1) {
                Log.e("BlankFragment", "Medicine ID is invalid.");
            } else {
                Log.d("BlankFragment", "Received medicine ID: " + medicineId);
            }

            if (bundle.getBoolean("isEdit", false)) {
                editTextText.setText(bundle.getString("editTextText", ""));
                textView3.setText(bundle.getString("textView3", ""));
                spinner2.setSelection(getSpinnerPosition(bundle.getString("spinner2Value", "選擇單位")));
                textView9.setText(bundle.getString("textView9", ""));
                editTextNumber.setText(String.valueOf(bundle.getInt("editTextNumber", 0)));
                editTextNumber2.setText(String.valueOf(bundle.getInt("editTextNumber2", 0)));

                String startDate = bundle.getString("startDate", "");
                startDateTextView.setText(startDate);

                ArrayList<String> timeList = bundle.getStringArrayList("timeContainer");
                if (timeList != null) {
                    for (String time : timeList) {
                        addTimeTextView(time);
                    }
                }
            }
        } else {
            Log.e("BlankFragment", "No arguments found.");
        }

        return view;
    }

    // 獲取 spinner2 的位置
    private int getSpinnerPosition(String value) {
        for (int i = 0; i < spinner2.getCount(); i++) {
            if (spinner2.getItemAtPosition(i).toString().equals(value)) {
                return i;
            }
        }
        return 0; // 如果未找到，返回默認值
    }

    private void setupSpinner() {
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                textView9.setText(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.單位, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter);

    }


    private void showDatePickerDialog() {
        // 获取当前日期
        int year = selectedStartDate.get(Calendar.YEAR);
        int month = selectedStartDate.get(Calendar.MONTH);
        int day = selectedStartDate.get(Calendar.DAY_OF_MONTH);

        // 显示日期选择器
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year1, month1, dayOfMonth) -> {
                    selectedStartDate.set(year1, month1, dayOfMonth);
                    startDateTextView.setText(String.format("%d-%02d-%02d", year1, month1 + 1, dayOfMonth));
                },
                year, month, day
        );
        datePickerDialog.show();
    }


    private void addNewMedicine() {
        String medicineName = editTextText.getText().toString().trim();
        String medicineFrequency = textView3.getText().toString().trim();
        ArrayList<String> times = getTimeContainerTexts();
        String dosage = textView9.getText().toString().trim();

        // 检查输入的有效性
        if (TextUtils.isEmpty(medicineName) || TextUtils.isEmpty(medicineFrequency) ||
                times.isEmpty() || TextUtils.isEmpty(dosage) ||
                TextUtils.isEmpty(editTextNumber.getText()) ||
                TextUtils.isEmpty(editTextNumber2.getText()) ||
                TextUtils.isEmpty(startDateTextView.getText())) {
            Toast.makeText(getActivity(), "請填寫正確藥物資料", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查库存是否有效
        int stock;
        int stock2;
        try {
            stock = Integer.parseInt(editTextNumber.getText().toString());
            stock2 = Integer.parseInt(editTextNumber2.getText().toString());

            if (stock <= 0 || stock2 <= 0) {
                Toast.makeText(getActivity(), "資料不可為0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (stock > stock2) {
                Toast.makeText(getActivity(), "劑量不能超過庫存量", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "請填寫正確的數量", Toast.LENGTH_SHORT).show();
            return;
        }

        String spinner2Value = spinner2.getSelectedItem().toString();
        // 获取图片的 URI
        Uri imageUri;
        photo.buildDrawingCache();
        Bitmap bitmap = photo.getDrawingCache();

        // 使用临时变量保存 imageUri
        Uri tempImageUri = getImageUri(getActivity(), bitmap);

        // 如果 tempImageUri 为 null，使用默认图片
        if (tempImageUri == null) {
            tempImageUri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + R.drawable.mc_pt);
        }

        // 将临时变量赋值给 imageUri，确保其不被后续代码修改
        imageUri = tempImageUri;

        String startDate = startDateTextView.getText().toString().trim();
        if (startDate.equals("選擇開始日期") || TextUtils.isEmpty(startDate)) {
            Toast.makeText(getActivity(), "請選擇開始日期", Toast.LENGTH_SHORT).show();
            return;
        }

        // 先查找是否已经存在相同名称的药品
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // 获取当前用户的 UID
        DatabaseReference medicinesRef = FirebaseDatabase.getInstance().getReference("medicines").child(userUid);

        medicinesRef.orderByChild("name").equalTo(medicineName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(getActivity(), "此藥品已存在，請勿重複添加", Toast.LENGTH_SHORT).show();
                } else {
                    // 创建药品对象
                    Medicine newMedicine = new Medicine(medicineName, medicineFrequency, times, dosage, stock, stock2, spinner2Value, imageUri.toString(), -1, startDate);

                    // 获取药品的唯一 ID，使用当前时间戳作为 ID
                    int medicineId = (int) System.currentTimeMillis(); // 使用时间戳作为 ID

                    // 将 ID 设置到药品对象中
                    newMedicine.setId(medicineId); // 设置药物 ID

                    // 创建数据库引用并使用字符串 ID
                    DatabaseReference medicineRef = medicinesRef.child(String.valueOf(medicineId)); // 使用字符串 ID

                    // 存储药品数据到 Firebase
                    medicineRef.setValue(newMedicine).addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            // 将结果传递给 HomeFragment
                            Bundle result = new Bundle();
                            result.putInt("medicineId", medicineId); // 保存整数类型的 ID
                            result.putString("medicineName", medicineName);
                            result.putString("medicineFrequency", medicineFrequency);
                            result.putStringArrayList("medicineTimes", times);
                            result.putString("dosage", dosage);
                            result.putInt("stock", stock);
                            result.putInt("stock2", stock2);
                            result.putString("spinner2Value", spinner2Value);
                            result.putString("medicineImageUrl", imageUri.toString());
                            result.putString("startDate", startDate);

                            getParentFragmentManager().setFragmentResult("addMedicineResult", result);
                            NavController navController = Navigation.findNavController(requireView());
                            navController.popBackStack();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getActivity(), "添加藥品失敗", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "查找藥品時出錯", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 提取获取图片 URI 的方法
    private Uri getImageUri(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        // 使用当前时间戳或 UUID 生成唯一文件名
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg"; // 或者使用 UUID
        // String fileName = "IMG_" + UUID.randomUUID().toString() + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);  // 使用唯一的文件名
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        ContentResolver resolver = context.getContentResolver();
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                }
            } catch (IOException e) {
                Log.e("BlankFragment", "Failed to save image", e);
                // 删除不完整的文件
                resolver.delete(imageUri, null, null);
                return null;
            }
        } else {
            Log.e("BlankFragment", "Failed to create MediaStore entry");
            return null;
        }

        return imageUri;
    }
    private void updateExistingMedicine() {
        int existingMedicineId = getArguments().getInt("medicineId", -1); // 使用 int 类型
        if (existingMedicineId == -1) {
            Log.e("BlankFragment", "Existing medicine ID is invalid, cannot update.");
            Toast.makeText(getActivity(), "ID無效", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedName = editTextText.getText().toString().trim();
        String updatedFrequency = textView3.getText().toString().trim();
        ArrayList<String> updatedTimes = getTimeContainerTexts();
        String updatedDosage = textView9.getText().toString().trim();
        String updatedStartDate = startDateTextView.getText().toString().trim(); // 获取开始日期

        // 检查 updatedName, updatedFrequency, updatedTimes, updatedDosage 是否为空
        if (TextUtils.isEmpty(updatedName) || TextUtils.isEmpty(updatedFrequency) ||
                updatedTimes.isEmpty() || TextUtils.isEmpty(updatedDosage) ||
                TextUtils.isEmpty(updatedStartDate)) {
            Toast.makeText(getActivity(), "請填寫正確藥物資料", Toast.LENGTH_SHORT).show();
            return;
        }

        String stockString = editTextNumber.getText().toString().trim();
        String stock2String = editTextNumber2.getText().toString().trim();

        // 检查 stock 和 stock2 是否为空，并解析值
        int updatedStock;
        int updatedStock2;

        try {
            updatedStock = Integer.parseInt(stockString);
            updatedStock2 = Integer.parseInt(stock2String);

            if (updatedStock <= 0 || updatedStock2 <= 0) {
                Toast.makeText(getActivity(), "資料不可為0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (updatedStock > updatedStock2) {
                Toast.makeText(getActivity(), "劑量不能超過庫存量", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "請填寫正確的數量", Toast.LENGTH_SHORT).show();
            return; // 捕获异常并返回
        }

        String updatedSpinner2Value = spinner2.getSelectedItem().toString();

        // 获取图片
        photo.buildDrawingCache();
        Bitmap bitmap = photo.getDrawingCache();
        Uri updatedImageUri = getImageUri(getActivity(), bitmap);

        // 如果没有更新图片，保留之前的图片
        if (updatedImageUri == null) {
            updatedImageUri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + R.drawable.mc_pt); // 默认图片
        }

        // 创建药品对象并更新到 Firebase
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // 获取当前用户的 UID
        Medicine updatedMedicine = new Medicine(updatedName, updatedFrequency, updatedTimes, updatedDosage, updatedStock, updatedStock2, updatedSpinner2Value, updatedImageUri.toString(), existingMedicineId, updatedStartDate);
        DatabaseReference medicineRef = FirebaseDatabase.getInstance().getReference("medicines").child(userUid).child(String.valueOf(existingMedicineId));


        medicineRef.setValue(updatedMedicine).addOnSuccessListener(aVoid -> {
            // 发送更新结果回 HomeFragment
            Bundle result = new Bundle();
            result.putString("updatedName", updatedName);
            result.putString("updatedFrequency", updatedFrequency);
            result.putStringArrayList("updatedTimes", updatedTimes);
            result.putString("updatedDosage", updatedDosage);
            result.putInt("updatedStock", updatedStock);
            result.putInt("updatedStock2", updatedStock2);
            result.putString("updatedSpinner2Value", updatedSpinner2Value);
            result.putInt("medicineId", existingMedicineId); // 确保 ID 被传递
            result.putString("updatedStartDate", updatedStartDate);

            getParentFragmentManager().setFragmentResult("editResult", result);
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack();
        }).addOnFailureListener(e -> {
            // 处理错误
        });
    }

    private void navigateToHomeFragment(boolean isDeleting) {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main5);

        Bundle bundle = new Bundle();
        if (!isDeleting) {
            bundle.putString("editTextText", editTextText.getText().toString());
            bundle.putString("textView3", textView3.getText().toString());
            bundle.putStringArrayList("timeContainer", getTimeContainerTexts());
            bundle.putString("spinner2Value", spinner2.getSelectedItem().toString());

            // 如果有照片，获取其 URI
            if (photo.getDrawable() != null) {
                photo.buildDrawingCache();
                Bitmap bitmap = photo.getDrawingCache();
                Uri imageUri = getImageUri(getActivity(), bitmap);
                bundle.putString("medicineImageUrl", imageUri != null ? imageUri.toString() : null);
            }
        }

        // 使用 popBackStack 返回到 HomeFragment
        navController.popBackStack(R.id.navigation_home, false);
        navController.navigate(R.id.navigation_home, bundle);
    }



    private ArrayList<String> getTimeContainerTexts() {
        ArrayList<String> times = new ArrayList<>();
        for (TextView timeTextView : timeTextViews) {
            times.add(timeTextView.getText().toString());
        }
        return times;
    }

    private void deleteMedicine() {
        Toast.makeText(getActivity(), "藥物已刪除", Toast.LENGTH_SHORT).show();
    }

    private void showFrequencyBottomSheet() {
        BottomSheet bottomSheetFragment = new BottomSheet();
        bottomSheetFragment.setOnFrequencySelectedListener(this);
        bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
    }

    @Override
    public void onFrequencySelected(String frequency) {
        if ("weekday".equals(frequency)) {
            BottomWeekdaySheet bottomWeekdaySheet = new BottomWeekdaySheet();
            bottomWeekdaySheet.setOnWeekdaySelectedListener(selectedDays -> {
                textView3.setText(selectedDays);
                closeAllBottomSheets();
            });
            bottomWeekdaySheet.show(getParentFragmentManager(), bottomWeekdaySheet.getTag());
        } else if ("interval".equals(frequency)) {
            DayInterval dayIntervalSheet = new DayInterval();
            dayIntervalSheet.setOnIntervalSelectedListener(this);
            dayIntervalSheet.show(getParentFragmentManager(), dayIntervalSheet.getTag());
        } else {
            textView3.setText(frequency);
            closeAllBottomSheets();
        }
    }

    @Override
    public void onIntervalSelected(String interval) {
        textView3.setText(interval);
        closeAllBottomSheets();
    }

    private void closeAllBottomSheets() {
        FragmentManager fragmentManager = getParentFragmentManager();
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof BottomSheetDialogFragment) {
                ((BottomSheetDialogFragment) fragment).dismiss();
            }
        }
    }





    private void showTimePickerDialog() {
        int hour = 8;
        int minute = 0;

        TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                (view, hourOfDay, minute1) -> {
                    String period = hourOfDay < 12 ? "上午" : "下午";
                    int hourIn12Format = hourOfDay % 12 == 0 ? 12 : hourOfDay % 12;
                    String time = String.format("%s %02d:%02d", period, hourIn12Format, minute1);
                    addTimeTextView(time);
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void addTimeTextView(String time) {
        // 檢查時間是否已存在
        for (TextView timeTextView : timeTextViews) {
            if (timeTextView.getText().toString().equals(time)) {
                Toast.makeText(getActivity(), "此時間已選擇", Toast.LENGTH_SHORT).show();
                return; // 如果時間已存在，則返回
            }
        }

        final TextView timeTextView = new TextView(getActivity());
        timeTextView.setTextSize(35);
        timeTextView.setText(time);
        timeTextView.setPadding(16, 16, 16, 16);
        timeTextView.setBackgroundResource(R.drawable.timetextview);

        // 設置刪除按鈕
        timeTextView.setOnClickListener(v -> {
            timeContainer.removeView(timeTextView);
            timeTextViews.remove(timeTextView);
        });

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.setMargins(16, 16, 16, 16);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        timeTextView.setLayoutParams(params);

        timeTextViews.add(timeTextView);
        timeContainer.addView(timeTextView);
    }


    // 保存药物信息到 SharedPreferences
    private void saveToPreferences(String medicineName, String medicineFrequency) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("medicineName", medicineName);
        editor.putString("medicineFrequency", medicineFrequency);
        editor.apply();
    }

    // 从 SharedPreferences 中读取药物信息
    private void loadFromPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String medicineName = sharedPreferences.getString("medicineName", "");
        String medicineFrequency = sharedPreferences.getString("medicineFrequency", "");
        editTextText.setText(medicineName);
        textView3.setText(medicineFrequency);
    }


    @Override
    public void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.nav_view);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE); // 隱藏 BottomNavigationView
        }
    }
}