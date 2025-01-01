package com.PokeMeng.OldManGO.FirstLogin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.PokeMeng.OldManGO.Personal.SetPersonalData;
import com.PokeMeng.OldManGO.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;


public class FacebookGoogle extends AppCompatActivity {
    ImageView grandmaa;
    ImageView grandpaa;
    Bitmap[] maa = new Bitmap[3];
    Bitmap[] paa = new Bitmap[3];
    int currentmaaIndex = 0;
    int currentpaaIndex = 0;
    Handler handler = new Handler();


    Runnable maaRunnable = new Runnable() {
        @Override
        public void run() {
            // 切換心形圖片索引
            currentmaaIndex = (currentmaaIndex + 1) % maa.length;

            // 設置下一個圖片到 grandmaa
            grandmaa.setImageBitmap(maa[currentmaaIndex]);

            // 確保畫面更新
            grandmaa.invalidate();

            // 設置下一次的切換時間為 400 毫秒
            handler.postDelayed(this, 400);
        }
    };

    Runnable paaRunnable = new Runnable() {
        @Override
        public void run() {
            // 切換心形圖片索引
            currentpaaIndex = (currentpaaIndex + 1) % paa.length;

            // 設置下一個圖片到 grandpaa
            grandpaa.setImageBitmap(paa[currentpaaIndex]);

            // 確保畫面更新
            grandpaa.invalidate();

            // 設置下一次的切換時間為 400 毫秒
            handler.postDelayed(this, 400);
        }
    };
    ImageButton google;
    //ImageButton fb;
    FirebaseAuth auth;
    FirebaseDatabase database;
    GoogleSignInClient googleSignInClient;

    //CallbackManager callbackManager;

    int RC_SIGN_IN=30;

    FirebaseFirestore firestore;

    // Facebook 登入用的 ActivityResultLauncher
    /*private final ActivityResultLauncher<Intent> facebookLoginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        callbackManager.onActivityResult(result.getResultCode(), result.getResultCode(), data);
                    }
                }
            }
    );*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_facebook_google);
        grandmaa = findViewById(R.id.grandmaa);
        grandpaa = findViewById(R.id.grandpaa);

        maa[0] = BitmapFactory.decodeResource(getResources(), R.drawable.grandma);
        maa[1] = BitmapFactory.decodeResource(getResources(), R.drawable.grandma02);
        maa[2] = BitmapFactory.decodeResource(getResources(), R.drawable.grandma03);
        paa[0] = BitmapFactory.decodeResource(getResources(), R.drawable.grandpa);
        paa[1] = BitmapFactory.decodeResource(getResources(), R.drawable.grandpa02);
        paa[2] = BitmapFactory.decodeResource(getResources(), R.drawable.grandpa03);

        handler.post(maaRunnable);
        handler.post(paaRunnable);

        //FacebookSdk.sdkInitialize(getApplicationContext());
        //AppEventsLogger.activateApp(this.getApplication());

        //callbackManager = CallbackManager.Factory.create();

        /*LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        startActivity(new Intent(MainActivity.this,next.class));
                        finish();
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });*/

        google=findViewById(R.id.google);
        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        firestore = FirebaseFirestore.getInstance();


        //fb=findViewById(R.id.fb);
        /*fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList("public_profile","email","user_status"));

            }
        });*/

        GoogleSignInOptions gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_id))
                .requestEmail()
                .build();

        googleSignInClient=GoogleSignIn.getClient(this,gso);
        // 確保每次進入這個頁面時登出 Google 帳戶
        googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                googleSignInClient.revokeAccess().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // 確保完全登出
                    }
                });
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signin();
            }
        });
    }


    private void signin() {
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent,RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Task<GoogleSignInAccount> task= GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuth(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this,e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

    }
    public void onGet(View v) {
        Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(it, 100);
    }
    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(FacebookGoogle.this, "歡迎你! 登入成功", Toast.LENGTH_SHORT).show();
                            FirebaseUser user = auth.getCurrentUser();

                            // 創建一個 HashMap 來儲存使用者資料
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("name", user.getDisplayName());
                            map.put("Gmail", user.getEmail());

                            // 將資料儲存到 Firestore 的 Users 集合中
                            firestore.collection("Users")
                                    .document(user.getUid()) // 使用者的 UID 作為文件 ID
                                    .set(map, SetOptions.merge())// 使用 map 來儲存資料使用set會覆蓋掉目前的文所有的欄位，要使用merge option，現在還有一個問題是清除資料後並沒有跳出重新登入要求，要改成跳出登入要求，會讀不到目前的使用者，因為讀不到無法把uid放在text上，會直接閃退 現在已經清除了，所以會閃退(現在主畫面的文字是uid，讀不到就會閃退)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() { //剛才的狀況是在重新執行後因為使用set，會覆蓋調積分的欄位會被清空沒有積分的欄位，現在，ok
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // 資料儲存成功，跳轉到主頁面
                                                Intent intent = new Intent(FacebookGoogle.this, SetPersonalData.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                // 資料儲存失敗，顯示錯誤訊息
                                                Toast.makeText(FacebookGoogle.this, "資料儲存失敗: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            // 登入失敗，顯示錯誤訊息
                            Toast.makeText(FacebookGoogle.this, "登入失敗: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}