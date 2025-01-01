package com.PokeMeng.OldManGO.Medicine;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.PokeMeng.OldManGO.R;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "medication_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 从 Intent 中获取药物名称和时间
        String medicineName = intent.getStringExtra("medicine_name");
        String medicineTime = intent.getStringExtra("medicine_time");

        Log.d("AlarmReceiver", "Alarm received for: " + medicineName + " at " + medicineTime);

        // 显示 Toast 提示
        Toast.makeText(context, "時間到了~ 該吃藥了喔！", Toast.LENGTH_LONG).show();

        // 创建通知
        createNotification(context, medicineName, medicineTime);
    }

    private void createNotification(Context context, String medicineName, String medicineTime) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知渠道（Android 8.0 及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID, "Medication Reminders", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
                Log.d("AlarmReceiver", "Notification channel created: " + CHANNEL_ID);
            }
        }

        // 使用药物名称和时间组合成唯一 ID
        String uniqueId = medicineName + medicineTime;

        // 创建 Intent，用于打开 DashboardFragment
        Intent intent = new Intent(context, MainActivity5.class); // MainActivity5 是你的主活动
        intent.putExtra("fragment", "DashboardFragment"); // 添加一个标记，指示要打开的 Fragment
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // 使用 SINGLE_TOP，避免新建任务
        // 只保留 NEW_TASK 标志

        // 创建 PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 构建通知
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.medicine) // 使用你自己的通知图标
                .setContentTitle("用藥提醒")
                .setContentText("時間到了~ 該吃藥了喔！")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // 设置点击通知时的 PendingIntent
                .setAutoCancel(true)
                .build();

        // 显示通知，使用组合后的唯一 ID
        notificationManager.notify(uniqueId.hashCode(), notification);
    }

}
