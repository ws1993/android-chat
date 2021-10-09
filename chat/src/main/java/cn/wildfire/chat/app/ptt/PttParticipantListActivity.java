/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.view.MenuItem;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.voip.conference.ConferenceInviteActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.PttInviteMessageContent;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.ptt.PTTSession;

public class PttParticipantListActivity extends WfcBaseActivity {
    private PTTSession session;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        session = PTTClient.getInstance().getCurrentSession();
        if (session == null) {
            finish();
            return;
        }
        PttParticipantListFragment fragment = new PttParticipantListFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }

    @Override
    protected int menu() {
        return R.menu.ptt_participant_list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_ptt_participant_add) {
            addParticipant();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void addParticipant() {
        MessageContent invite;
        invite = new PttInviteMessageContent(session.getChannelId(), session.getHost(), session.getTitle(), null, null);

        Intent intent = new Intent(this, ConferenceInviteActivity.class);
        intent.putExtra("inviteMessage", invite);
        startActivity(intent);
    }
}
