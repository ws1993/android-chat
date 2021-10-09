/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.BaseUserListFragment;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.ptt.PTTSession;
import cn.wildfirechat.remote.ChatManager;

public class PttParticipantListFragment extends BaseUserListFragment {
    private String selfUid;
    private PTTSession session;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showQuickIndexBar(false);
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);

        session = PTTClient.getInstance().getCurrentSession();
        if (session == null) {
            getActivity().finish();
            return;
        }
        selfUid = ChatManager.Instance().getUserId();
        loadAndShowConferenceParticipants();
    }

    @Override
    public void onUserClick(UIUserInfo userInfo) {
        List<String> items = new ArrayList<>();
        items.add("查看用户信息");
        if (selfUid.equals(session.getHost()) && !selfUid.equals(userInfo.getUserInfo().uid)) {
            items.add("移除成员");
        } else if (selfUid.equals(userInfo.getUserInfo().uid)) {
            items.add("退出");
        }

        new MaterialDialog.Builder(getActivity())
            .cancelable(true)
            .items(items)
            .itemsCallback((dialog, itemView, position, text) -> {
                switch (position) {
                    case 0:
                        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                        intent.putExtra("userInfo", userInfo.getUserInfo());
                        startActivity(intent);
                        break;
                    case 1:
                        // TODO
                        break;
                    case 2:
//                        session.leaveChannel();
                        // TODO
                        break;
                    default:
                        break;
                }
            })
            .show();
    }

    private void loadAndShowConferenceParticipants() {
        session.getMembers(new PTTSession.GetMembersCallback() {
            @Override
            public void onSuccess(List<String> participantIds) {

                List<UIUserInfo> uiUserInfos = new ArrayList<>();
                if (participantIds != null && participantIds.size() > 0) {
                    List<UserInfo> participantUserInfos = ChatManager.Instance().getUserInfos(participantIds, null);
                    for (UserInfo userInfo : participantUserInfos) {
                        UIUserInfo uiUserInfo = new UIUserInfo(userInfo);

                        if (selfUid.equals(session.getHost())) {
                            uiUserInfo.setDesc("主持人");
                            uiUserInfos.add(0, uiUserInfo);
                        } else {
                            uiUserInfos.add(uiUserInfo);
                        }
                    }
                }
                UIUserInfo selfUiUserInfo = new UIUserInfo(ChatManager.Instance().getUserInfo(selfUid, false));
                if (selfUid.equals(session.getHost())) {
                    selfUiUserInfo.setDesc("我、主持人");
                } else {
                    selfUiUserInfo.setDesc("我");
                }

                for (UIUserInfo uiUserInfo : uiUserInfos) {
                    uiUserInfo.setShowCategory(false);
                }

                showContent();
                userListAdapter.setUsers(uiUserInfos);
            }

            @Override
            public void onFail(int errorCode) {
                Toast.makeText(getActivity(), "获取参与者列表失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
