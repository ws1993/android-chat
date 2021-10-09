/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;

import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;

public class CreatePttChannelActivity extends WfcBaseActivity {
    @BindView(R.id.pttTitleTextInputEditText)
    FixedTextInputEditText titleEditText;
    @BindView(R.id.pttDescTextInputEditText)
    FixedTextInputEditText descEditText;

    @BindView(R.id.createPttBtn)
    Button createButton;

    private String title;
    private String desc;

    @Override
    protected int contentLayout() {
        return R.layout.av_ptt_create_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(ChatManager.Instance().getUserId(), false);
        if (userInfo != null) {
            titleEditText.setText(userInfo.displayName + "的对讲频道");
        } else {
            titleEditText.setText("频道");
        }
        descEditText.setText("欢迎参加");
    }


    @OnTextChanged(value = R.id.pttTitleTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pttTitle(Editable editable) {
        this.title = editable.toString();
        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(desc)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.pttDescTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pttDesc(Editable editable) {
        this.desc = editable.toString();
        if (!TextUtils.isEmpty(desc) && !TextUtils.isEmpty(title)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnClick(R.id.createPttBtn)
    public void onClickCreateBtn() {
        List<String> members = new ArrayList<>();
        members.add(ChatManager.Instance().getUserId());
        //ChatManager.Instance().createGroup(null, title, null, GroupInfo.GroupType.Free, null, members, null, Collections.singletonList(1), new DummyPttNotificationMessageContent(), new GeneralCallback2() {
        PTTClient.getInstance().createPttChannel(title, 0, new GeneralCallback2() {
            @Override
            public void onSuccess(String result) {
                Log.e("ptt", "create ptt channel success" + result);
                Intent intent = new Intent(CreatePttChannelActivity.this, PttActivity.class);
                intent.putExtra("channelId", result);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFail(int errorCode) {
                Log.e("ptt", "create ptt channel error" + errorCode);
            }
        });
    }
}
