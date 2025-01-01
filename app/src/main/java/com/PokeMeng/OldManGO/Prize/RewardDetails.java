package com.PokeMeng.OldManGO.Prize;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.PokeMeng.OldManGO.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RewardDetails extends AppCompatActivity {

    private int requiredPoints; // 所需積分
    private int userPoints; // 使用者當前積分
    private int rewardId; // 獎品 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prize_reward_details);

        // 獲取從上級頁面傳遞的資料
        userPoints = getIntent().getIntExtra("userPoints", 0);
        requiredPoints = getIntent().getIntExtra("requiredPoints", 0); // 獲取所需積分
        rewardId = getIntent().getIntExtra("rewardId", 0); // 獲取獎品 ID

        // 顯示積分有效期
        TextView rewardExpiryDateTextView = findViewById(R.id.rewardExpiryDateTextView);
        rewardExpiryDateTextView.setText("兌換有效期: 2024年10月1日 - 2024年11月30日");

        // 顯示產品資訊
        TextView rewardProductInfoTextView = findViewById(R.id.rewardProductInfoTextView);
        ImageView rewardImageView = findViewById(R.id.rewardImageView);

        switch (rewardId) {
            case 1:
                rewardImageView.setImageResource(R.drawable.reward_image);
                rewardProductInfoTextView.setText("兌換日期期限：\n2024年10月01日 - 2024年11月30日\n\n" +
                        "這款高品質不沾鍋是每位廚房愛好者的必備良品！先進的不沾塗層技術讓食材輕鬆滑出，無論是煎、炒、煮或燉煮，均可輕鬆掌控火候，烹調出色香味俱全的佳餚。\n\n" +
                        "產品特點：\n\n" +
                        "耐高溫設計：確保長時間使用不變形。\n" +
                        "易於清潔：省去繁瑣的清理時間。\n" +
                        "多種烹飪方式適用：無論日常家常菜或節日大餐，均能應對自如。\n\n" +
                        "使用建議：建議使用木製或矽膠鍋具，以保護不沾塗層，延長產品壽命。讓這款鍋成為您烹飪的得力助手，為家人和朋友準備美味餐點！");
                break;
            case 2:
                rewardImageView.setImageResource(R.drawable.reward_image02);
                rewardProductInfoTextView.setText("兌換日期期限：\n2024年10月01日 - 2024年11月30日\n\n" +
                        "這款智能掃地機器人將為您的家居清潔帶來革命性便利！其先進的導航技術可自動檢測房間結構，高效清潔每一個角落，讓您無需煩惱。\n\n" +
                        "產品特點：\n\n" +
                        "智能導航：規劃最佳清掃路徑，避開障礙物，徹底清掃每寸地面。\n" +
                        "多種清潔模式：支持自動、邊緣及局部清掃，滿足各種需求。\n" +
                        "長效電池：單次充電可持續清掃120分鐘，無需頻繁充電。\n" +
                        "智能APP控制：隨時遠端控制，查看清掃進度，輕鬆管理家庭清潔。\n\n" +
                        "使用建議：首次使用前整理地面，定期清理塵盒和刷子，確保最佳性能。讓這款掃地機器人成為您舒適居住環境的得力助手！");
                break;
            // 你可以添加更多的獎品資訊
        }

        // 顯示所需積分
        rewardExpiryDateTextView.setText("所需積分: " + requiredPoints + "分");

        // 設置兌換按鈕
        Button redeemButton = findViewById(R.id.redeemButton);
        if (userPoints >= requiredPoints) {
            redeemButton.setEnabled(true); // 使用者積分足夠時按鈕可用
            redeemButton.setOnClickListener(v -> {
                updatePointsAfterRedemption();
                saveExchangeHistory(); // 保存兌換記錄
                showSuccessDialog();
            });
        } else {
            redeemButton.setEnabled(false); // 積分不足時按鈕不可用
            redeemButton.setOnClickListener(v ->
                    Toast.makeText(this, "積分不足，無法兌換", Toast.LENGTH_SHORT).show()
            );
        }


    }

    // 更新積分並返回到前一個頁面
    private void updatePointsAfterRedemption() {
//        Intent resultIntent = new Intent();
//        resultIntent.putExtra("updatedPoints", userPoints - requiredPoints); // 返回更新後的積分
//        setResult(RESULT_OK, resultIntent);
//        finish(); // 關閉當前活動
//        Intent resultIntent = new Intent();
//        resultIntent.putExtra("updatedPoints", userPoints - requiredPoints); // 返回更新後的積分
//        setResult(RESULT_OK, resultIntent);
//        finish(); // 關閉當前活動
        // 計算新的積分
        int updatedPoints = userPoints - requiredPoints;

        // 獲取當前用戶
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // 獲取Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 更新用戶的積分
            DocumentReference userRef = db.collection("Users").document(userId);
            userRef.update("points", updatedPoints)
                    .addOnSuccessListener(aVoid -> {
                        // Firestore更新成功
                        Toast.makeText(RewardDetails.this, "積分更新成功！", Toast.LENGTH_SHORT).show();

                        // 返回更新後的積分到上一頁面!!!(重要)
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("updatedPoints", updatedPoints); // 返回更新后的积分
                        setResult(RESULT_OK, resultIntent);
                        finish(); // 关闭当前页面
                    })
                    .addOnFailureListener(e -> {
                        // Firestore更新失败
                        Toast.makeText(RewardDetails.this, "積分更新失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // 保存兌換記錄到 SharedPreferences
    private void saveExchangeHistory() {
        // 獲取當前用戶
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 創建紀錄
            String record = getCurrentDateTime() + " - " + getRewardDescription() + " - " + requiredPoints + "分";

            // 設firestore細項
            Map<String, Object> exchangeRecord = new HashMap<>();
            exchangeRecord.put("record", record);
            exchangeRecord.put("timestamp", FieldValue.serverTimestamp());

            // 保存紀錄到 Firestore
            db.collection("Users")
                    .document(userId)
                    .collection("ExchangeHistory")
                    .add(exchangeRecord)
                    .addOnSuccessListener(documentReference -> {
                        // 記錄保存成功
                        Toast.makeText(RewardDetails.this, "兑换记录保存成功！", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // 記錄保存失敗
                        Toast.makeText(RewardDetails.this, "兑换记录保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date()); // 獲取當前日期和時間
    }

    private String getRewardDescription() {
        switch (rewardId) {
            case 1:
                return "高品質不沾鍋";
            case 2:
                return "高級清潔器";
            default:
                return "未知獎品";
        }
    }

    // 顯示兌換成功的對話框
    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("兌換成功")
                .setMessage("恭喜！您已成功兌換獎勳。請確認您已閱讀此消息。")
                .setPositiveButton("我知道了", (dialog, which) -> {
                    // 用户点击“我知道了”按钮后，关闭对话框
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}
