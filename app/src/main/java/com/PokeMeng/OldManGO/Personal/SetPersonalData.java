package com.PokeMeng.OldManGO.Personal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.PokeMeng.OldManGO.Game.ColorGame;
import com.PokeMeng.OldManGO.MainActivity;
import com.PokeMeng.OldManGO.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;

public class SetPersonalData extends AppCompatActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener {

    Calendar c = Calendar.getInstance();
    TextView txDate;
    EditText editTextAge, editTextText, editTextText3, editTextNumber2, editTextText2;
    ImageView img01, img02, img03;
    RadioButton rbt01, rbt02;
    ImageButton save;

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    DatabaseReference mDatabase;
    FirebaseUser currentUser;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_personal_data);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();
        // Find views
        editTextText = findViewById(R.id.editTextText); // 姓名
        txDate = findViewById(R.id.year); // 出生日期
        editTextAge = findViewById(R.id.editTextNumber); // 年龄
        txDate.setOnClickListener(this);

        img01 = findViewById(R.id.father);
        img02 = findViewById(R.id.mother);
        img03 = findViewById(R.id.imageView9);

        // Initialize RadioButtons
        rbt01 = findViewById(R.id.men);
        rbt02 = findViewById(R.id.girl);

        editTextText3 = findViewById(R.id.editTextText3); // 紧急联系人
        editTextNumber2 = findViewById(R.id.editTextNumber2); // 紧急联系人电话
        editTextText2 = findViewById(R.id.editTextText2); // 地址
        save = findViewById(R.id.save);

        // Set listeners for RadioButtons
        rbt01.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                img01.setVisibility(View.VISIBLE);
                img02.setVisibility(View.INVISIBLE);
                img03.setVisibility(View.INVISIBLE);
            }
        });

        rbt02.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                img01.setVisibility(View.INVISIBLE);
                img02.setVisibility(View.VISIBLE);
                img03.setVisibility(View.INVISIBLE);
            }
        });

        // Set listener for save button
        save.setOnClickListener(v -> saveData());

        // Set listener for close button
        ImageView closeButton = findViewById(R.id.close);
        closeButton.setOnClickListener(v -> showClearConfirmationDialog());

        // Load existing user data from Firebase if it exists
        if (currentUser != null) {
            loadUserData();
        }
    }

        // Add Firebase ValueEventListener to monitor the 'users' node
        private void loadUserData() {
            mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Personal personal = dataSnapshot.getValue(Personal.class);
                    if (personal != null) {
                        // Fill fields with retrieved data
                        editTextText.setText(personal.getName());
                        txDate.setText(personal.getDate());
                        editTextAge.setText(personal.getAge());
                        editTextText3.setText(personal.getAdditionalInfo1());
                        editTextNumber2.setText(personal.getAdditionalInfo2());
                        editTextText2.setText(personal.getAddress());

                        // Set gender based on saved data
                        if (personal.getGender().equals("男")) {
                            rbt01.setChecked(true);
                        } else if (personal.getGender().equals("女")) {
                            rbt02.setChecked(true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
                Log.e("FirebaseData", "Error: " + databaseError.getMessage());
            }
        });
    }

    // Date picker click event
    public void onClick(View v) {
        if (v == txDate) {
            new DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar, this,
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH))
                    .show();
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        txDate.setText(year + "/" + (month + 1) + "/" + dayOfMonth);

        // Calculate age
        Calendar dob = Calendar.getInstance();
        dob.set(year, month, dayOfMonth);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        // Set age to EditText
        editTextAge.setText(String.valueOf(age));
    }

    private void saveData() {
        String date = txDate.getText().toString(); // 生日
        String age = editTextAge.getText().toString(); // 年龄
        String text1 = editTextText.getText().toString(); // 姓名
        String text2 = editTextText3.getText().toString(); // 紧急联系人
        String number2 = editTextNumber2.getText().toString(); // 紧急联系人电话
        String text3 = editTextText2.getText().toString(); // 地址

        // 检查所有必填字段是否填写
        if (text1.isEmpty() || date.isEmpty() || (!rbt01.isChecked() && !rbt02.isChecked())
                || text2.isEmpty() || number2.isEmpty() || text3.isEmpty()) {

            Toast.makeText(SetPersonalData.this, "請填寫所有必填欄位", Toast.LENGTH_SHORT).show();
            return; // 如果有任何必填字段为空，则停止保存操作
        }

        // 检查紧急联络人电话号码是否为10位并以"09"开头
        if (number2.length() != 10 || !number2.startsWith("09")) {
            Toast.makeText(SetPersonalData.this, "緊急聯絡人電話必須為10位數且以09開頭", Toast.LENGTH_SHORT).show();
            return; // 如果电话号码不是10位或不是以09开头，则停止保存操作
        }

        String selectedGender = rbt01.isChecked() ? "男" : "女";

        Personal personal = new Personal(text1, date, selectedGender, age, text2, number2, text3);

        // 保存数据到 Firebase
        mDatabase.child("users").child(currentUser.getUid()).setValue(personal)
                .addOnSuccessListener(aVoid -> Toast.makeText(SetPersonalData.this, "儲存成功", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SetPersonalData.this, "儲存失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Show confirmation dialog
    private void clearFields() {
        editTextText.setText(""); // 姓名
        txDate.setText("請點選輸入生日"); // 出生日期
        editTextAge.setText(""); // 年龄
        editTextText3.setText(""); // 紧急联系人
        editTextNumber2.setText(""); // 紧急联系人电话
        editTextText2.setText(""); // 地址
        rbt01.setChecked(false); // 性别
        rbt02.setChecked(false); // 性别
    }
    private void showClearConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("確認清空")
                .setMessage("您確定要清空所有資料嗎？")
                .setPositiveButton("確定", (dialog, which) -> clearFields()) // 點擊確定後清空資料
                .setNegativeButton("取消", null) // 點擊取消不執行任何操作
                .show();
    }
    public void gotomain(View v) {
        String date = txDate.getText().toString(); // 生日
        String age = editTextAge.getText().toString(); // 年龄
        String name = editTextText.getText().toString(); // 姓名
        String emergencyContact = editTextText3.getText().toString(); // 紧急联系人
        String emergencyNumber = editTextNumber2.getText().toString(); // 紧急联系人电话
        String address = editTextText2.getText().toString(); // 地址

        // 检查所有必填字段是否填写
        if (name.isEmpty() || date.isEmpty() || (!rbt01.isChecked() && !rbt02.isChecked())
                || emergencyContact.isEmpty() || emergencyNumber.isEmpty() || address.isEmpty()) {
            Toast.makeText(SetPersonalData.this, "請填寫所有必填字段", Toast.LENGTH_SHORT).show();
        } else {
            // 所有必填字段都已填写，允许返回主页面
            Intent it = new Intent(this, MainActivity.class);
            startActivity(it);
        }
    }
}
