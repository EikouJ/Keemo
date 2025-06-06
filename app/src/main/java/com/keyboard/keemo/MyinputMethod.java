package com.keyboard.keemo;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;

public class MyinputMethod extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard mainKeyboard;
    private Keyboard specialKeyboard;
    private Keyboard currentKeyboard;

    private boolean isCaps = false;
    private boolean isCapsLock = false;
    private long lastShiftTime = 0;
    private boolean isSpecialMode = false;

    private Handler previewHandler = new Handler();

    // Pour détecter la dernière touche pressée
    private int lastKeyCodePressed = 0;

    // Pour les popups d'accents
    private PopupWindow accentPopup;
    private boolean isShowingAccents = false;
    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private float initialTouchX, initialTouchY;
    private int selectedAccentIndex = 0;
    private LinearLayout accentLayout;
    private String[] accentChars = {"a", "à", "á", "â", "æ", "ä", "ā"};
    private String[] accentCharsUpper = {"A", "À", "Á", "Â", "Æ", "Ä", "Ā"};

    // Codes personnalisés
    private static final int KEYCODE_SWITCH_TO_SPECIAL = -11;
    private static final int KEYCODE_SWITCH_TO_MAIN = -10;
    private static final long LONG_PRESS_DELAY = 500; // 500ms pour le long press

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);

        mainKeyboard = new Keyboard(this, R.xml.keys);
        specialKeyboard = new Keyboard(this, R.xml.special_chars);

        currentKeyboard = mainKeyboard;
        keyboardView.setKeyboard(currentKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(true);

        // Intercepter les événements tactiles pour gérer les popups
        keyboardView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchEvent(event);
            }
        });

        return keyboardView;
    }

    private boolean handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getX();
                initialTouchY = event.getY();

                // Vérifier si c'est la touche 'a'
                int keyIndex = getKeyIndex(event.getX(), event.getY());
                if (keyIndex != -1 && isKeyA(keyIndex)) {
                    startLongPressTimer(keyIndex);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isShowingAccents) {
                    updateAccentSelection(event.getX(), event.getY());
                    return true; // Consommer l'événement
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isShowingAccents) {
                    selectAccent();
                    hideAccentPopup();
                    return true; // Consommer l'événement
                }
                cancelLongPressTimer();
                break;
        }
        return false; // Laisser le KeyboardView gérer les autres événements
    }

    private int getKeyIndex(float x, float y) {
        if (currentKeyboard == null) return -1;

        int[] location = new int[2];
        keyboardView.getLocationOnScreen(location);

        for (int i = 0; i < currentKeyboard.getKeys().size(); i++) {
            Keyboard.Key key = currentKeyboard.getKeys().get(i);
            if (x >= key.x && x <= key.x + key.width &&
                    y >= key.y && y <= key.y + key.height) {
                return i;
            }
        }
        return -1;
    }

    private boolean isKeyA(int keyIndex) {
        if (keyIndex < 0 || keyIndex >= currentKeyboard.getKeys().size()) return false;
        Keyboard.Key key = currentKeyboard.getKeys().get(keyIndex);
        return key.codes != null && key.codes.length > 0 &&
                (key.codes[0] == 97 || key.codes[0] == 65); // 'a' ou 'A'
    }

    private void startLongPressTimer(int keyIndex) {
        longPressRunnable = new Runnable() {
            @Override
            public void run() {
                showAccentPopup(keyIndex);
            }
        };
        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
    }

    private void cancelLongPressTimer() {
        if (longPressRunnable != null) {
            longPressHandler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }

    private void showAccentPopup(int keyIndex) {
        isShowingAccents = true;
        selectedAccentIndex = 0;

        // Créer le layout du popup
        accentLayout = new LinearLayout(this);
        accentLayout.setOrientation(LinearLayout.HORIZONTAL);
        accentLayout.setPadding(16, 16, 16, 16);

        // Background avec le même style que key_preview_background
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);

        // Gradient similaire au preview
        int[] colors = {Color.parseColor("#1565C0"), Color.parseColor("#0D47A1")};
        background.setColors(colors);
        background.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        background.setOrientation(GradientDrawable.Orientation.TL_BR); // 135°

        background.setCornerRadius(12); // Même rayon que le preview
        background.setStroke(2, Color.parseColor("#0D47A1"));
        accentLayout.setBackground(background);

        // Déterminer quels caractères utiliser selon le mode majuscule
        String[] charsToUse = (isCaps || isCapsLock) ? accentCharsUpper : accentChars;

        // Ajouter les caractères avec accents
        for (int i = 0; i < charsToUse.length; i++) {
            TextView textView = createAccentTextView(charsToUse[i], i == selectedAccentIndex);
            accentLayout.addView(textView);
        }

        // Créer et afficher le popup
        accentPopup = new PopupWindow(accentLayout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        accentPopup.setFocusable(false);
        accentPopup.setTouchable(false);

        // Calculer la position du popup pour qu'il soit centré au-dessus de la touche
        Keyboard.Key key = currentKeyboard.getKeys().get(keyIndex);
        int[] location = new int[2];
        keyboardView.getLocationOnScreen(location);

        // Mesurer la largeur du popup
        accentLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = accentLayout.getMeasuredWidth();

        // Centrer horizontalement sur la touche
        int popupX = location[0] + key.x + (key.width / 2) - (popupWidth / 2);

        // Positionner au-dessus de la touche avec un petit espacement
        int popupY = location[1] + key.y - accentLayout.getMeasuredHeight() - 20;

        // S'assurer que le popup reste dans les limites de l'écran
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        if (popupX < 0) popupX = 10;
        if (popupX + popupWidth > screenWidth) popupX = screenWidth - popupWidth - 10;
        if (popupY < 0) popupY = location[1] + key.y + key.height + 10; // En dessous si pas de place au-dessus

        accentPopup.showAtLocation(keyboardView, Gravity.NO_GRAVITY, popupX, popupY);
    }

    private TextView createAccentTextView(String character, boolean isSelected) {
        TextView textView = new TextView(this);
        textView.setText(character);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        textView.setTextColor(Color.WHITE); // Texte blanc pour contraster avec le fond bleu
        textView.setPadding(20, 15, 20, 15);
        textView.setGravity(Gravity.CENTER);
        textView.setMinWidth(45); // Largeur minimale pour uniformité

        if (isSelected) {
            // Cercle de mise en évidence avec un style cohérent
            GradientDrawable selectedBackground = new GradientDrawable();
            selectedBackground.setShape(GradientDrawable.OVAL);

            // Gradient plus clair pour la sélection
            int[] selectedColors = {Color.parseColor("#42A5F5"), Color.parseColor("#1E88E5")};
            selectedBackground.setColors(selectedColors);
            selectedBackground.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            selectedBackground.setOrientation(GradientDrawable.Orientation.TL_BR);

            selectedBackground.setStroke(2, Color.parseColor("#0D47A1"));
            textView.setBackground(selectedBackground);
            textView.setTextColor(Color.WHITE);
            textView.setElevation(4); // Légère élévation pour l'effet de sélection
        } else {
            // Fond transparent pour les non-sélectionnés
            textView.setBackground(null);
            textView.setTextColor(Color.WHITE);
        }

        return textView;
    }

    private void updateAccentSelection(float x, float y) {
        if (accentLayout == null) return;

        // Calculer l'index basé sur la position X relative au popup
        int[] popupLocation = new int[2];
        accentLayout.getLocationOnScreen(popupLocation);

        float relativeX = x - popupLocation[0];
        int newIndex = (int) (relativeX / (accentLayout.getWidth() / accentChars.length));
        newIndex = Math.max(0, Math.min(accentChars.length - 1, newIndex));

        if (newIndex != selectedAccentIndex) {
            selectedAccentIndex = newIndex;
            updateAccentHighlight();
        }
    }

    private void updateAccentHighlight() {
        if (accentLayout == null) return;

        String[] charsToUse = (isCaps || isCapsLock) ? accentCharsUpper : accentChars;

        for (int i = 0; i < accentLayout.getChildCount(); i++) {
            TextView textView = (TextView) accentLayout.getChildAt(i);

            if (i == selectedAccentIndex) {
                // Cercle de mise en évidence avec style cohérent
                GradientDrawable selectedBackground = new GradientDrawable();
                selectedBackground.setShape(GradientDrawable.OVAL);

                // Gradient plus clair pour la sélection
                int[] selectedColors = {Color.parseColor("#42A5F5"), Color.parseColor("#1E88E5")};
                selectedBackground.setColors(selectedColors);
                selectedBackground.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                selectedBackground.setOrientation(GradientDrawable.Orientation.TL_BR);

                selectedBackground.setStroke(2, Color.parseColor("#0D47A1"));
                textView.setBackground(selectedBackground);
                textView.setTextColor(Color.WHITE);
                textView.setElevation(4);
            } else {
                textView.setBackground(null);
                textView.setTextColor(Color.WHITE);
                textView.setElevation(0);
            }
        }
    }

    private void selectAccent() {
        if (selectedAccentIndex >= 0 && selectedAccentIndex < accentChars.length) {
            String[] charsToUse = (isCaps || isCapsLock) ? accentCharsUpper : accentChars;
            String selectedChar = charsToUse[selectedAccentIndex];

            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.commitText(selectedChar, 1);
            }

            // Réinitialiser caps si nécessaire
            if (isCaps && !isCapsLock && !isSpecialMode) {
                isCaps = false;
                updateShiftKey();
            }
        }
    }

    private void hideAccentPopup() {
        if (accentPopup != null && accentPopup.isShowing()) {
            accentPopup.dismiss();
            accentPopup = null;
        }
        isShowingAccents = false;
        selectedAccentIndex = 0;
        accentLayout = null;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        // Si on affiche les accents, ignorer les touches normales
        if (isShowingAccents) return;

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
                    code = (isCaps || isCapsLock) ? Character.toUpperCase(code) : Character.toLowerCase(code);
                }
                inputConnection.commitText(String.valueOf(code), 1);

                if (isCaps && !isCapsLock && !isSpecialMode) {
                    isCaps = false;
                    updateShiftKey();
                }
                break;
        }
    }

    private void switchToSpecialKeyboard() {
        isSpecialMode = true;
        animateKeyboardTransition(() -> {
            currentKeyboard = specialKeyboard;
            keyboardView.setKeyboard(currentKeyboard);
            keyboardView.invalidateAllKeys();
        });
    }

    private void switchToMainKeyboard() {
        isSpecialMode = false;
        animateKeyboardTransition(() -> {
            currentKeyboard = mainKeyboard;
            keyboardView.setKeyboard(currentKeyboard);
            updateShiftKey();
        });
    }

    private void toggleKeyboard() {
        if (isSpecialMode) {
            switchToMainKeyboard();
        } else {
            switchToSpecialKeyboard();
        }
    }

    private void animateKeyboardTransition(Runnable switchAction) {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(10);
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                switchAction.run();

                AnimationSet fadeInSet = new AnimationSet(true);
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(1);

                ScaleAnimation scale = new ScaleAnimation(
                        0.95f, 1.0f, 0.95f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                scale.setDuration(10);

                fadeInSet.addAnimation(fadeIn);
                fadeInSet.addAnimation(scale);
                fadeInSet.setInterpolator(new AccelerateDecelerateInterpolator());

                keyboardView.startAnimation(fadeInSet);
            }
            @Override public void onAnimationRepeat(Animation animation) {}
        });

        keyboardView.startAnimation(fadeOut);
    }

    private void handleShiftToggle() {
        if (isSpecialMode) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShiftTime < 500) {
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
                if (isCapsLock) {
                    key.label = "⇪";
                } else if (isCaps) {
                    key.label = "⇧";
                } else {
                    key.label = "SHIFT";
                }
            }
        }
        keyboardView.invalidateAllKeys();
    }

    @Override
    public void onPress(int primaryCode) {
        lastKeyCodePressed = primaryCode;
    }

    @Override
    public void onRelease(int primaryCode) {
        lastKeyCodePressed = 0;
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
        if (lastKeyCodePressed == 32) { // espace
            toggleKeyboard();
        }
    }

    @Override
    public void swipeRight() {
        if (lastKeyCodePressed == 32) { // espace
            toggleKeyboard();
        }
    }

    @Override
    public void swipeDown() {
        AlphaAnimation slideDown = new AlphaAnimation(1.0f, 0.0f);
        slideDown.setDuration(150);
        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                requestHideSelf(0);
            }
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        keyboardView.startAnimation(slideDown);
    }

    @Override
    public void swipeUp() {
        // Pas d'action
    }
}