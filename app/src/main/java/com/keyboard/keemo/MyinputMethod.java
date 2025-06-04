package com.keyboard.keemo;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputConnection;

public class MyinputMethod extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;

    private boolean isCaps = false;
    private boolean isCapsLock = false;
    private long lastShiftTime = 0;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this, R.xml.keys);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        return keyboardView;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) return;

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                CharSequence selectedText = inputConnection.getSelectedText(0);
                if (TextUtils.isEmpty(selectedText)) {
                    inputConnection.deleteSurroundingText(1, 0);
                } else {
                    inputConnection.commitText("", 1);
                }
                break;

            case Keyboard.KEYCODE_SHIFT:
                handleShiftToggle();
                break;

            case Keyboard.KEYCODE_DONE:
                inputConnection.sendKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)
                );
                break;

            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code)) {
                    if (isCaps || isCapsLock) {
                        code = Character.toUpperCase(code);
                    } else {
                        code = Character.toLowerCase(code);
                    }
                }
                inputConnection.commitText(String.valueOf(code), 1);

                // désactive majuscule temporaire après une touche si ce n’est pas un verrouillage
                if (isCaps && !isCapsLock) {
                    isCaps = false;
                    updateShiftKey();
                }
                break;
        }
    }

    private void handleShiftToggle() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShiftTime < 500) {
            // Double appui → activer caps lock
            isCapsLock = true;
            isCaps = true;
        } else {
            if (isCapsLock) {
                isCapsLock = false;
                isCaps = false;
            } else {
                isCaps = !isCaps;
            }
        }
        lastShiftTime = currentTime;
        updateShiftKey();
    }

    private void updateShiftKey() {
        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.label != null && key.label.length() == 1) {
                char c = key.label.charAt(0);
                if (Character.isLetter(c)) {
                    char updatedChar = (isCaps || isCapsLock) ?
                            Character.toUpperCase(c) : Character.toLowerCase(c);
                    key.label = String.valueOf(updatedChar);
                    key.codes[0] = (int) updatedChar;
                }
            } else if (key.codes[0] == Keyboard.KEYCODE_SHIFT) {
                // facultatif : changer l’apparence du label de SHIFT selon l'état
                key.label = isCapsLock ? "⇪" : "SHIFT";
            }
        }
        keyboardView.invalidateAllKeys();
    }

    @Override
    public void onPress(int primaryCode) {
        // Optionnel : retour visuel
    }

    @Override
    public void onRelease(int primaryCode) {
        // Optionnel : retour visuel
    }

    @Override
    public void onText(CharSequence text) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.commitText(text, 1);
        }
    }

    @Override
    public void swipeLeft() {
        // Optionnel
    }

    @Override
    public void swipeRight() {
        // Optionnel
    }

    @Override
    public void swipeDown() {
        // Optionnel
    }

    @Override
    public void swipeUp() {
        // Optionnel
    }
}
