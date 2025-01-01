package com.PokeMeng.OldManGO.Medicine;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;

import com.PokeMeng.OldManGO.Medicine.ui.SharedViewModel;
import com.PokeMeng.OldManGO.R;
import com.google.firebase.FirebaseApp;
import com.PokeMeng.OldManGO.Medicine.ui.home.BlankFragment;

public class MainActivity2 extends AppCompatActivity {

    private SharedViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m_main2);
        FirebaseApp.initializeApp(this);

        // 创建 SavedStateHandle 实例
        SavedStateHandle savedStateHandle = new SavedStateHandle();

        // 使用 SharedViewModelFactory 创建 SharedViewModel 实例
        SharedViewModelFactory factory = new SharedViewModelFactory(this, savedStateHandle); // 传递 Context 和 SavedStateHandle

        // 使用 ViewModelProvider 初始化 SharedViewModel
        viewModel = new ViewModelProvider(this, factory).get(SharedViewModel.class);

        // 这里可以监听 LiveData 数据的变化
        viewModel.getMedicines().observe(this, medicines -> {
            // 更新 UI
        });

        if (savedInstanceState == null) {
            // 加载 BlankFragment
            Fragment fragment = new BlankFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }
}


