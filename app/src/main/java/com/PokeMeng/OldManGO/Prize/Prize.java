package com.PokeMeng.OldManGO.Prize;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.PokeMeng.OldManGO.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Prize extends AppCompatActivity {

    private int currentPoints; //用戶當前積分
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private ActivityResultLauncher<Intent> rewardDetailsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prize);
        getPointsFromFireStore();
        // 設置積分到期日
        String pointsExpiryDate = "2024年12月31日";
        ((TextView)findViewById(R.id.expiryDateTextView)).setText(getString(R.string.Prize_points_expiry_date, pointsExpiryDate));
        // 設置第一個獎品的點擊事件 // 獎品1所需積分為21000
        findViewById(R.id.rewardImageView1).setOnClickListener(v -> openRewardDetails(1, 5));
        // 設置第二個獎品的點擊事件 // 獎品2所需積分為15000
        findViewById(R.id.rewardImageView2).setOnClickListener(v -> openRewardDetails(2, 20));
        // 設置查看兌換記錄按鈕的點擊事件
        findViewById(R.id.viewHistoryButton).setOnClickListener(v -> startActivity(new Intent(Prize.this, ExchangeHistory.class)));
        // 假设button10对应21000积分的兑换
        findViewById(R.id.button10).setOnClickListener(v -> handleRedeem(5));
        // 假设button9对应15000积分的兑换
        findViewById(R.id.button9).setOnClickListener(v -> handleRedeem(20));
        rewardDetailsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        int updatedPoints = result.getData().getIntExtra("updatedPoints", currentPoints);
                        currentPoints = updatedPoints;  // 更新當前積分
                        showCurrentUserPoints();        // 刷新積分顯示
                    }
                }
        );
    }

    private void getPointsFromFireStore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) {
            Log.w("TaskRead", "No current user found.");
            return;
        }
        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("Users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Long points = document.getLong("points");
                    if (points != null) {
                        Toast.makeText(Prize.this, "Points found: " + points, Toast.LENGTH_SHORT).show();
                        currentPoints = points.intValue();
                    }
                    else {
                        Toast.makeText(Prize.this, "No points found", Toast.LENGTH_SHORT).show();
                        currentPoints = 0;
                    }
                    showCurrentUserPoints(); // Update the points display after fetching data
                } else
                    Log.d("TAG", "No such document");
            } else
                Log.d("TAG", "get failed with ", task.getException());
        });
    }
    private void showCurrentUserPoints() {
        // 設置顯示積分的 TextView
        ((TextView)findViewById(R.id.pointsTextView)).setText(getString(R.string.Prize_points_text, currentPoints));
    }

    private void handleRedeem(int requiredPoints) {
        if (currentPoints >= requiredPoints) {
            openRewardDetails(1, requiredPoints); // 调用打开奖励详情的方法
        }
        // else {
            // 显示提示用户积分不足
            // 您可以使用Toast或AlertDialog来提示
        //}
    }

    // 跳轉到詳細頁面的方法
    private void openRewardDetails(int rewardId, int requiredPoints) {
        Intent intent = new Intent(this, RewardDetails.class);
        intent.putExtra("userPoints", currentPoints); // 傳遞當前積分到 reward_details 活動
        intent.putExtra("rewardId", rewardId); // 傳遞獎品 ID
        intent.putExtra("requiredPoints", requiredPoints); // 傳遞所需積分
        rewardDetailsLauncher.launch(intent);
    }
}
