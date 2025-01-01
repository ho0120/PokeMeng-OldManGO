package com.PokeMeng.OldManGO.Medicine;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.SavedStateHandle;

import android.content.Context;

import com.PokeMeng.OldManGO.Medicine.ui.SharedViewModel;


public class SharedViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;
    private final SavedStateHandle savedStateHandle;

    public SharedViewModelFactory(Context context, SavedStateHandle savedStateHandle) {
        this.context = context;
        this.savedStateHandle = savedStateHandle;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SharedViewModel.class)) {
            return (T) new SharedViewModel(savedStateHandle, context); // 正确传递参数
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
