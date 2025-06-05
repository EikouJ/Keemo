package com.keyboard.keemo;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputConnection;

public class MyinputMethod extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard mainKeyboard;
    private Keyboard specialKeyboard;
    private Keyboard currentKeyboard;

    private boolean isCaps = false;
    private boolean isCapsLock = false;
    private long lastShiftTime = 0;
    private boolean isSpecialMode = false;

    // Codes personnalisés
    private static final int KEYCODE_SWITCH_TO_SPECIAL = -11;
    private static final int KEYCODE_SWITCH_TO_MAIN = -10;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);

        // Initialiser les deux claviers
        mainKeyboard = new Keyboard(this, R.xml.keys);
        specialKeyboard = new Keyboard(this, R.xml.special_chars);

        // Commencer avec le clavier principal
        currentKeyboard = mainKeyboard;
        keyboardView.setKeyboard(currentKeyboard);
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

            case KEYCODE_SWITCH_TO_SPECIAL:
                switchToSpecialKeyboard();
                break;

            case KEYCODE_SWITCH_TO_MAIN:
                switchToMainKeyboard();
                break;

            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code) && !isSpecialMode) {
                    if (isCaps || isCapsLock) {
                        code = Character.toUpperCase(code);
                    } else {
                        code = Character.toLowerCase(code);
                    }
                }
                inputConnection.commitText(String.valueOf(code), 1);

                // Désactiver majuscule temporaire après une touche si ce n'est pas un verrouillage
                if (isCaps && !isCapsLock && !isSpecialMode) {
                    isCaps = false;
                    updateShiftKey();
                }
                break;
        }
    }

    private void switchToSpecialKeyboard() {
        isSpecialMode = true;
        currentKeyboard = specialKeyboard;
        keyboardView.setKeyboard(currentKeyboard);
        keyboardView.invalidateAllKeys();
    }

    private void switchToMainKeyboard() {
        isSpecialMode = false;
        currentKeyboard = mainKeyboard;
        keyboardView.setKeyboard(currentKeyboard);
        updateShiftKey();
    }

    private void handleShiftToggle() {
        if (isSpecialMode) return; // Pas de shift en mode spécial

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShiftTime < 500) {
            // Double appui → activer caps lock
            isCapsLock = !isCapsLock;
            isCaps = isCapsLock;
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
        if (isSpecialMode) return;

        for (Keyboard.Key key : currentKeyboard.getKeys()) {
            if (key.label != null && key.label.length() == 1) {
                char c = key.label.charAt(0);
                if (Character.isLetter(c)) {
                    char updatedChar = (isCaps || isCapsLock) ?
                            Character.toUpperCase(c) : Character.toLowerCase(c);
                    key.label = String.valueOf(updatedChar);
                    key.codes[0] = (int) updatedChar;
                }
            } else if (key.codes[0] == Keyboard.KEYCODE_SHIFT) {
                // Changer l'apparence du SHIFT selon l'état
                if (isCapsLock) {
                    key.label = "⇪"; // Caps lock activé
                } else if (isCaps) {
                    key.label = "⇧"; // Shift temporaire activé
                } else {
                    key.label = "SHIFT"; // Normal
                }
            }
        }
        keyboardView.invalidateAllKeys();
    }

    @Override
    public void onPress(int primaryCode) {
        // Feedback visuel optional
    }

    @Override
    public void onRelease(int primaryCode) {
        // Feedback visuel optional
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
        // Optionnel: fonctionnalité de swipe vers la gauche
    }

    @Override
    public void swipeRight() {
        // Optionnel: fonctionnalité de swipe vers la droite
    }

    @Override
    public void swipeDown() {
        // Swipe vers le bas: fermer le clavier
        requestHideSelf(0);
    }

    @Override
    public void swipeUp() {
        // Swipe vers le haut: changer de clavier
        if (isSpecialMode) {
            switchToMainKeyboard();
        } else {
            switchToSpecialKeyboard();
        }
    }
}