/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import cn.wildfire.chat.kit.BuildConfig;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.ptt.PTTSession;

public class PttService extends Service {
    private static final int NOTIFICATION_ID = 200;

    private WindowManager wm;
    private View view;
    private WindowManager.LayoutParams params;
    private Intent resumeActivityIntent;
    private boolean initialized = false;
    private boolean showFloatingWindow = false;

    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 可以多次调用
    public static void start(Context context, boolean showFloatingView) {
        Intent intent = new Intent(context, PttService.class);
        intent.putExtra("showFloatingView", showFloatingView);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, PttService.class);
        context.stopService(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PTTSession session = PTTClient.getInstance().getCurrentSession();
        if (session == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!initialized) {
            initialized = true;
            startForeground(NOTIFICATION_ID, buildNotification(session));
            checkCallState();
        }
        showFloatingWindow = intent.getBooleanExtra("showFloatingView", false);
        if (showFloatingWindow) {
            try {
                showFloatingWindow(session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            hideFloatBox();
        }
        return START_NOT_STICKY;
    }

    private void checkCallState() {
        PTTSession session = PTTClient.getInstance().getCurrentSession();
        if (session == null) {
            stopSelf();
        } else {
            updateNotification(session);
            if (showFloatingWindow) {
                showPttView(session);
            }
            handler.postDelayed(this::checkCallState, 1000);
        }
    }

    private void updateNotification(PTTSession session) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, buildNotification(session));
    }

    private Notification buildNotification(PTTSession session) {
        resumeActivityIntent = new Intent(this, PttActivity.class);
        resumeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resumeActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = BuildConfig.LIBRARY_PACKAGE_NAME + ".ptt";
            String channelName = "ptt";
            NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        String title = "对讲中";
        // TODO 谁在说话
        return builder.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wm != null && view != null) {
            wm.removeView(view);
        }
    }

    private void showFloatingWindow(PTTSession session) {
        if (wm != null) {
            return;
        }

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.type = type;
        params.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        params.format = PixelFormat.TRANSLUCENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        params.x = getResources().getDisplayMetrics().widthPixels;
        params.y = 0;

        view = LayoutInflater.from(this).inflate(R.layout.ptt_float_view, null);
        view.setOnTouchListener(onTouchListener);
        wm.addView(view, params);
        showPttView(session);
    }

    public void hideFloatBox() {
        if (wm != null && view != null) {
            wm.removeView(view);
            wm = null;
            view = null;
        }
    }

    private void showPttView(PTTSession session) {

        view.findViewById(R.id.pttLinearLayout).setVisibility(View.VISIBLE);
        TextView timeView = view.findViewById(R.id.durationTextView);

        long duration = (System.currentTimeMillis() - session.getStartMillis()) / 1000;
        if (duration >= 3600) {
            timeView.setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
        } else {
            timeView.setText(String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60)));
        }
    }

    private void clickToResume() {
        PTTSession session = PTTClient.getInstance().getCurrentSession();
        if (session != null) {
            startActivity(resumeActivityIntent);
        }
        showFloatingWindow = false;
    }

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        float lastX, lastY;
        int oldOffsetX, oldOffsetY;
        int tag = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            if (tag == 0) {
                oldOffsetX = params.x;
                oldOffsetY = params.y;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                lastX = x;
                lastY = y;
            } else if (action == MotionEvent.ACTION_MOVE) {
                // 减小偏移量,防止过度抖动
                params.x += (int) (x - lastX) / 3;
                params.y += (int) (y - lastY) / 3;
                tag = 1;
                wm.updateViewLayout(v, params);
            } else if (action == MotionEvent.ACTION_UP) {
                int newOffsetX = params.x;
                int newOffsetY = params.y;
                if (Math.abs(oldOffsetX - newOffsetX) <= 20 && Math.abs(oldOffsetY - newOffsetY) <= 20) {
                    clickToResume();
                } else {
                    tag = 0;
                }
            }
            return true;
        }
    };
}
