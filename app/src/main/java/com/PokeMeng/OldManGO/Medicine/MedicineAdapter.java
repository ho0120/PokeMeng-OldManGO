package com.PokeMeng.OldManGO.Medicine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.PokeMeng.OldManGO.R;

import java.util.List;
import com.PokeMeng.OldManGO.Medicine.ui.SharedViewModel;
import com.PokeMeng.OldManGO.TaskManager;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {
    private List<Medicine> medicines;
    private OnItemClickListener onItemClickListener;
    private boolean isDashboard;
    private Context context;
    private boolean isNotificationsFragment;
    private boolean isHomeFragment;
    private SharedViewModel sharedViewModel;

    // 统一 SharedPreferences 名称
    private static final String SHARED_PREFS_NAME = "MedicinePrefs";

    public MedicineAdapter(Context context, List<Medicine> medicines, OnItemClickListener onItemClickListener, boolean isDashboard, boolean isNotificationsFragment, boolean isHomeFragment, SharedViewModel sharedViewModel) {
        this.context = context;
        this.medicines = medicines;
        this.onItemClickListener = onItemClickListener;
        this.isDashboard = isDashboard;
        this.isNotificationsFragment = isNotificationsFragment;
        this.isHomeFragment = isHomeFragment;
        this.sharedViewModel = sharedViewModel;
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        Medicine medicine = medicines.get(position);
        // 获取最新的库存值
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        int updatedStock2 = sharedPreferences.getInt("stock2_" + medicine.getId(), medicine.getStock2());

        holder.bind(medicine, onItemClickListener);
        handleButtonState(holder, medicine);

        // 显示当前库存
        holder.stockTextView.setText("庫存: " + medicine.getStock2());

        // 点击事件来更新库存
        holder.stockTextView.setOnClickListener(v -> {
            int newStock2 = medicine.getStock2() - medicine.getStock();
            if (newStock2 < 0) {
                newStock2 = 0;  // 确保库存不为负数
            }

            // 更新 Medicine 对象中的库存值
            medicine.setStock2(newStock2);
            holder.stockTextView.setText("庫存: " + newStock2); // 更新显示

            // 通知 Fragment 进行 Firebase 更新
            if (onStockUpdateListener != null) {
                onStockUpdateListener.onStockUpdated(medicine);
            }
        });


        // 仅在 HomeFragment 中显示库存
        if (isHomeFragment) {
            holder.stockTextView.setText("庫存剩餘: " + updatedStock2);
            holder.stockTextView.setVisibility(View.VISIBLE);
        } else {
            holder.stockTextView.setVisibility(View.GONE);
        }

        Log.d("MedicineAdapter", "Binding medicine: " + medicine.getName() + ", " + medicine.getFrequency());
        if (isNotificationsFragment) {
            holder.checkmark.setVisibility(View.VISIBLE); // 在 NotificationsFragment 中显示打勾符号
        } else {
            holder.checkmark.setVisibility(View.GONE); // 在其他地方隐藏打勾符号
        }
    }

    private void handleButtonState(MedicineViewHolder holder, Medicine medicine) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String firstTime = medicine.getTimes().isEmpty() ? "" : medicine.getTimes().get(0);
        boolean isClicked = getButtonClickedState(sharedPreferences, medicine.getId(), firstTime);

        if (isDashboard) {
            holder.takenButton.setVisibility(View.VISIBLE);
            holder.takenButton.setEnabled(!isClicked);
            holder.takenButton.setBackgroundColor(isClicked ? Color.GRAY : Color.BLACK);


            if (!isClicked) {
                // 修改这里，传递 position
                holder.takenButton.setOnClickListener(v -> showConfirmationDialog(holder, medicine, firstTime, holder.getAdapterPosition()));

            }
        } else {
            holder.takenButton.setVisibility(View.GONE); // 在 HomeFragment 中隐藏按钮
        }
    }


    private void showConfirmationDialog(MedicineViewHolder holder, Medicine medicine, String firstTime, int position) {
        new AlertDialog.Builder(context)
                .setTitle("確認")
                .setMessage("是否已服用藥物？")
                .setPositiveButton("是", (dialog, which) -> {
                    SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();

                    setButtonClickedState(editor, medicine.getId(), firstTime);
                    medicine.setTaken(true);
                    holder.takenButton.setEnabled(false);
                    holder.takenButton.setBackgroundColor(Color.GRAY);

                    // 每次点击更新库存量
                    int newStock2 = medicine.getStock2() - medicine.getStock();
                    if (newStock2 < 0) {
                        newStock2 = 0;  // 库存量不能为负数
                    }
                    medicine.setStock2(newStock2);

                    // 保存更新后的库存量到 SharedPreferences
                    editor.putInt("stock2_" + medicine.getId(), newStock2);
                    editor.apply();

                    // 更新 TextView 显示新库存量
                    holder.stockTextView.setText("庫存: " + newStock2);

                    // 检查是否需要提示警告
                    if (newStock2 < 5) {
                        new AlertDialog.Builder(context)
                                .setTitle("警告")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage("庫存量已低於5，請盡快回診補充！")
                                .setPositiveButton("確定", null)
                                .show();
                    }

                    // 更新 Firebase 中的库存量
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    String userId = currentUser.getUid();
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference medicineRef = database.getReference("medicines").child(userId).child(String.valueOf(medicine.getId()));

                    medicineRef.child("stock2").setValue(newStock2).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("HomeFragment", "Stock updated successfully in Firebase.");
                        } else {
                            Log.e("HomeFragment", "Failed to update stock in Firebase: " + task.getException().getMessage());
                        }
                    });

                    // 更新 SharedViewModel
                    sharedViewModel.addTakenMedicine(medicine);

                    // 通知 RecyclerView 更新当前项
                    notifyItemChanged(position);

                    // 检查 Firebase 任务是否已完成
                    TaskManager taskManager = new TaskManager(FirebaseFirestore.getInstance(), userId);
                    taskManager.checkAndCompleteTask("CompletedMedicine", result -> {
                        if (!result) {
                            Log.d("FireStore", "ChallengeCompleted not completed yet.");
                            taskManager.updateTaskStatusForSteps(3); // 更新步骤
                            taskManager.markTaskAsCompleted("CompletedMedicine"); // 标记为完成
                        } else {
                            Log.d("FireStore", "ChallengeCompleted already completed for today.");
                        }
                    });
                })
                .setNegativeButton("否", null)
                .show();
    }



    // 读取按钮点击状态
    private boolean getButtonClickedState(SharedPreferences sharedPreferences, int medicineId, String time) {
        return sharedPreferences.getBoolean("button_clicked_" + medicineId + "_" + time, false);
    }

    // 设置按钮点击状态
    private void setButtonClickedState(SharedPreferences.Editor editor, int medicineId, String time) {
        editor.putBoolean("button_clicked_" + medicineId + "_" + time, true);
        editor.apply();
    }


    // 定义接口用于库存更新的回调
    public interface OnStockUpdateListener {
        void onStockUpdated(Medicine medicine);
    }

    private OnStockUpdateListener onStockUpdateListener;

    public void setOnStockUpdateListener(OnStockUpdateListener listener) {
        this.onStockUpdateListener = listener;
    }



    @Override
    public int getItemCount() {
        return medicines.size();
    }

    public void updateMedicines(List<Medicine> newMedicines) {
        this.medicines = newMedicines;
        notifyDataSetChanged(); // 通知 RecyclerView 更新数据
    }

    class MedicineViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView frequencyTextView;
        private TextView timeTextView;
        private ImageView imageView;
        private Button takenButton;
        private ImageView checkmark;

        private TextView stockTextView;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.medicineName);
            frequencyTextView = itemView.findViewById(R.id.medicineFrequency);
            timeTextView = itemView.findViewById(R.id.medicineTime);
            imageView = itemView.findViewById(R.id.medicineImageView);
            takenButton = itemView.findViewById(R.id.btn_taken);
            checkmark = itemView.findViewById(R.id.checkmark);
            stockTextView = itemView.findViewById(R.id.stockTextView);
        }

        public void bind(Medicine medicine, OnItemClickListener listener) {
            Log.d("MedicineAdapter", "Binding medicine: " + medicine.getName() + ", " + medicine.getFrequency());

            nameTextView.setText(medicine.getName());
            frequencyTextView.setText(medicine.getFrequency());

            List<String> times = medicine.getTimes();
            if (times != null && !times.isEmpty()) {
                String timeString = String.join(", ", times);
                timeTextView.setText(timeString);
            } else {
                timeTextView.setText("No times available");
            }

            // 确保 imageUrl 不为空
            if (medicine.getImageUrl() != null && !medicine.getImageUrl().isEmpty()) {
                Glide.with(imageView.getContext())
                        .load(medicine.getImageUrl())
                        .placeholder(R.drawable.mc_pt)
                        .error(R.drawable.mc_pt)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.mc_pt);  // 使用默认图片
            }

            itemView.setOnClickListener(v -> listener.onItemClick(medicine));
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Medicine medicine);
    }
}