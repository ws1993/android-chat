/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.lqr.emoji.EmotionLayout;
import com.lqr.emoji.IEmotionSelectedListener;
import com.lqr.emoji.LQREmotionKit;
import com.lqr.emoji.MoonUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardHeightFrameLayout;

public class InputPanel extends FrameLayout implements IEmotionSelectedListener {

    @BindView(R2.id.editText)
    EditText editText;
    @BindView(R2.id.emotionImageView)
    ImageView emotionImageView;
    @BindView(R2.id.sendButton)
    Button sendButton;

    @BindView(R2.id.emotionContainerFrameLayout)
    KeyboardHeightFrameLayout emotionContainerFrameLayout;
    @BindView(R2.id.emotionLayout)
    EmotionLayout emotionLayout;
    @BindView(R2.id.inputContainerLinearLayout)
    LinearLayout inputContainerLinearLayout;

    private InputAwareLayout rootLinearLayout;
    private KeyboardDialogFragment keyboardDialogFragment;

    private OnConversationInputPanelStateChangeListener onConversationInputPanelStateChangeListener;

    private int commentEmojiCount = 0;
    private final static int MAX_EMOJI_PER_COMMENT = 50;

    public InputPanel(@NonNull Context context) {
        super(context);
    }

    public InputPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

    }

    public InputPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public InputPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }

    public void setOnConversationInputPanelStateChangeListener(OnConversationInputPanelStateChangeListener onConversationInputPanelStateChangeListener) {
        this.onConversationInputPanelStateChangeListener = onConversationInputPanelStateChangeListener;
    }

    public void bind(FragmentActivity activity, InputAwareLayout rootInputAwareLayout) {

    }

    public void onDestroy() {
    }

    public void init(KeyboardDialogFragment commentFragment, InputAwareLayout rootInputAwareLayout) {
        LayoutInflater.from(getContext()).inflate(R.layout.input_panel, this, true);
        ButterKnife.bind(this, this);

        this.keyboardDialogFragment = commentFragment;
        this.rootLinearLayout = rootInputAwareLayout;

        // emotion
        emotionLayout.setEmotionAddVisiable(false);
        emotionLayout.setEmotionSettingVisiable(false);
        emotionLayout.setStickerVisible(false);
        emotionLayout.setEmotionSelectedListener(this);
    }

    @OnClick(R2.id.emotionImageView)
    void onEmotionImageViewClick() {
        if (rootLinearLayout.getCurrentInput() == emotionContainerFrameLayout) {
            rootLinearLayout.showSoftkey(editText);
            hideEmotionLayout();
        } else {
            rootLinearLayout.show(editText, emotionContainerFrameLayout);
            showEmotionLayout();
        }
    }

    @OnTextChanged(value = R2.id.editText, callback = OnTextChanged.Callback.TEXT_CHANGED)
    void onInputTextChanged(CharSequence s, int start, int before, int count) {
        // do nothing
    }

    @OnTextChanged(value = R2.id.editText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void afterInputTextChanged(Editable editable) {
        if (editText.getText().toString().trim().length() > 0) {
            sendButton.setEnabled(true);
        } else {
            sendButton.setEnabled(false);
        }
    }

    @OnClick(R2.id.sendButton)
    void sendMessage() {
        commentEmojiCount = 0;
        Editable content = editText.getText();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        keyboardDialogFragment.sendMessage(content.toString());

        editText.setText("");
    }

    public void onKeyboardShown() {
        hideEmotionLayout();
    }

    public void onKeyboardHidden() {
        // do nothing
    }

    private void showEmotionLayout() {
        emotionImageView.setImageResource(R.mipmap.ic_chat_keyboard);
        if (onConversationInputPanelStateChangeListener != null) {
            onConversationInputPanelStateChangeListener.onInputPanelExpanded();
        }
    }

    private void hideEmotionLayout() {
        emotionImageView.setImageResource(R.mipmap.ic_chat_emo);
        if (onConversationInputPanelStateChangeListener != null) {
            onConversationInputPanelStateChangeListener.onInputPanelCollapsed();
        }
    }

    void collapse() {
        emotionImageView.setImageResource(R.mipmap.ic_chat_emo);
        rootLinearLayout.hideAttachedInput(true);
        rootLinearLayout.hideCurrentInput(editText);
    }

    @Override
    public void onEmojiSelected(String key) {
        Editable editable = editText.getText();
        if (key.equals("/DEL")) {
            commentEmojiCount--;
            commentEmojiCount = commentEmojiCount < 0 ? 0 : commentEmojiCount;
            editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        } else {
            if (commentEmojiCount >= MAX_EMOJI_PER_COMMENT) {
                Toast.makeText(keyboardDialogFragment.getContext(), "最多允许输入" + MAX_EMOJI_PER_COMMENT + "个表情符号", Toast.LENGTH_SHORT).show();
                return;
            }
            commentEmojiCount++;
            int code = Integer.decode(key);
            char[] chars = Character.toChars(code);
            String value = Character.toString(chars[0]);
            for (int i = 1; i < chars.length; i++) {
                value += Character.toString(chars[i]);
            }

            int start = editText.getSelectionStart();
            int end = editText.getSelectionEnd();
            start = (start < 0 ? 0 : start);
            end = (start < 0 ? 0 : end);
            editable.replace(start, end, value);

            int editEnd = editText.getSelectionEnd();
            MoonUtils.replaceEmoticons(LQREmotionKit.getContext(), editable, 0, editable.toString().length());
            editText.setSelection(editEnd);
        }
    }

    @Override
    public void onStickerSelected(String categoryName, String stickerName, String stickerBitmapPath) {
    }

    public interface OnConversationInputPanelStateChangeListener {
        /**
         * 输入面板展开
         */
        void onInputPanelExpanded();

        /**
         * 输入面板关闭
         */
        void onInputPanelCollapsed();
    }
}
