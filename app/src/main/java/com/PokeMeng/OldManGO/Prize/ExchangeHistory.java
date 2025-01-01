package com.PokeMeng.OldManGO.Prize;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import com.PokeMeng.OldManGO.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeHistory extends AppCompatActivity {

    private ListView exchangeHistoryListView;
    private List<String> exchangeHistoryList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prize_exchange_history);

        exchangeHistoryListView = findViewById(R.id.exchangeHistoryListView);
        exchangeHistoryList = new ArrayList<>();

        // 获取当前用户
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            // 读取 Firestore 中的兑换记录
            loadExchangeHistory();

        }

    }

    private void loadExchangeHistory() {
        String userId = currentUser.getUid();
        CollectionReference exchangeHistoryRef = db.collection("Users")
                .document(userId)
                .collection("ExchangeHistory");

        // 获取用户的所有兑换记录
        exchangeHistoryRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String record = document.getString("record");
                        exchangeHistoryList.add(record); // 添加到兑换记录列表
                    }
                    if (exchangeHistoryList.isEmpty()) {
                        exchangeHistoryList.add("暂无兑换记录");
                    }
                    // 更新 ListView
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, exchangeHistoryList);
                    exchangeHistoryListView.setAdapter(adapter);
                }
            }
        });
    }


    public void gotoprize(View v) {
        Intent it = new Intent(this, Prize.class);
        startActivity(it);
    }
}
