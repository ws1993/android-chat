/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.ptt.PTTSession;
import cn.wildfirechat.ptt.PTTSessionCallback;
import cn.wildfirechat.ptt.RequestToSpeakCallback;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class PttActivity extends FragmentActivity implements PTTSessionCallback {
    private static final int REQUEST_CODE_ADD_PARTICIPANT = 103;

    @BindView(R.id.membersTextView)
    TextView membersTextView;
    @BindView(R.id.talkingMemberLayout)
    LinearLayout talkingMemberLayout;
    @BindView(R.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R.id.nameTextView)
    TextView nameTextView;
    @BindView(R.id.talkButton)
    Button talkButton;

    private String channelId;
    private PTTSession session;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.av_ptt_activity);
        ButterKnife.bind(this);
        channelId = getIntent().getStringExtra("channelId");
        if (TextUtils.isEmpty(channelId)) {
            finish();
        } else {
            PTTClient.getInstance().joinPttChannel(channelId, this);
        }
    }

    @OnTouch(R.id.talkButton)
    public boolean talk(View button, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            updateTalkButton(true);
            this.session.requestToSpeak(new RequestToSpeakCallback() {
                @Override
                public void onReadyToSpeak(long maxDurationMillis) {
                    String selfUid = ChatManager.Instance().getUserId();
                    onPttTalking(selfUid);
                }

                @Override
                public void onFail(String msg) {
                    nameTextView.setText("对讲失败 " + msg);
                }

                @Override
                public void onSpeakTimeOut() {

                }
            });

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            updateTalkButton(false);
            this.session.stopSpeak();
            onPttIdle();
        }
        return true;
    }

    @OnClick(R.id.minimizeImageView)
    public void minimize() {
        String[] items;
        if (!ChatManager.Instance().getUserId().equals(session.getHost())) {
            items = new String[]{"最小化对讲机", "退出对讲机"};
        } else {
            items = new String[]{"最小化对讲机", "退出对讲机", "结束对讲"};
        }

        new MaterialDialog.Builder(this)
            .items(items)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    switch (position) {
                        case 0:
                            finish();
                            break;
                        case 1:
                            session.leaveChannel();
                            finish();
                            break;
                        case 2:
                            PTTClient.getInstance().destroyPttChannel(session.getChannelId(), new GeneralCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(PttActivity.this, "销毁对讲频道成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onFail(int errorCode) {
                                    Toast.makeText(PttActivity.this, "销毁对讲频道失败 " + errorCode, Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                            break;
                        default:
                            break;
                    }

                }
            })
            .build()
            .show();
    }

    @OnClick(R.id.membersTextView)
    public void members() {
//        Intent intent = new Intent(this, ConferenceParticipantListActivity.class);
//        startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
        }
    }

    private void updateTalkButton(boolean talking) {
        talkButton.setPressed(talking);
        talkButton.setText(talking ? "松手释放" : "按下说话");
        float scale = talking ? 1.5f : 1.0f;
        talkButton.animate().scaleX(scale).scaleY(scale).setDuration(100).start();
    }

    private void updateParticipantCount() {
        // TODO
        //int count = session.getParticipantIds().size() + 1;
//        membersTextView.setText("(" + count + ")");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onPttTalking(String userId) {
        talkingMemberLayout.setVisibility(View.VISIBLE);
        UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, false);
        GlideApp.with(this).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        nameTextView.setText(userInfo.displayName);
    }

    @Override
    public void onPttIdle() {
        talkingMemberLayout.setVisibility(View.GONE);
    }

    @Override
    public void onJoinSuccess(PTTSession session) {
        this.session = session;
    }

    @Override
    public void onJoinFail(int errorCode) {
        Toast.makeText(this, "join ptt channel fail " + errorCode, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PTTClient.getInstance().setSessionCallback(null);
    }
}
