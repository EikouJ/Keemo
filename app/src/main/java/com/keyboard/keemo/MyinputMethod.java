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
import android.util.DisplayMetrics;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

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
    private int lastKeyCodePressed = 0;

    private PopupWindow accentPopup;
    private boolean isShowingAccents = false;
    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private float initialTouchX, initialTouchY;
    private int selectedAccentIndex = 0;
    private LinearLayout accentLayout;

    private String[] accentCharsA = {"à", "á", "â", "æ", "ä", "ā"};
    private String[] accentCharsAUpper = {"À", "Á", "Â", "Æ", "Ä", "Ā"};

    private String[] accentCharsE = {"è", "é", "ê", "ě", "ë", "ē"};
    private String[] accentCharsEUpper = {"È", "É", "Ê", "Ě", "Ë", "Ē"};

    private String[] accentCharsI = {"ì", "í", "î", "ï", "ī", ""};
    private String[] accentCharsIUpper = {"Ì", "Í", "Î", "Ï", "Ī", "Ǐ"};

    private String[] accentCharsO = {"ó", "ò", "œ", "ô", "ō", "ö"};
    private String[] accentCharsOUpper = {"Ó", "Ò", "Œ", "Ô", "Ō", "Ö"};

    private char currentAccentLetter = 0;

    private static final int KEYCODE_SWITCH_TO_SPECIAL = -11;
    private static final int KEYCODE_SWITCH_TO_MAIN = -10;
    private static final long LONG_PRESS_DELAY = 500;

    private FrameLayout mainContainer;
    private View overlayView;

    @Override
    public View onCreateInputView() {
        mainContainer = new FrameLayout(this);

        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        mainContainer.addView(keyboardView);

        // Créer l'overlay semi-transparent
        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.argb(100, 0, 0, 0));
        overlayView.setVisibility(View.GONE);
        overlayView.setOnClickListener(v -> hideAccentPopup());
        mainContainer.addView(overlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        mainKeyboard = new Keyboard(this, R.xml.keys);
        specialKeyboard = new Keyboard(this, R.xml.special_chars);

        currentKeyboard = mainKeyboard;
        keyboardView.setKeyboard(currentKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(true);

        keyboardView.setOnTouchListener((v, event) -> handleTouchEvent(event));

        return mainContainer;
    }


    private boolean handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getX();
                initialTouchY = event.getY();

                int keyIndex = getKeyIndex(event.getX(), event.getY());
                if (keyIndex != -1 && isAccentKey(keyIndex)) {
                    startLongPressTimer(keyIndex);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isShowingAccents) {
                    if (Math.abs(event.getX() - initialTouchX) > dpToPx(5)
                            || Math.abs(event.getY() - initialTouchY) > dpToPx(5)) {
                        updateAccentSelection(event.getX(), event.getY());
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isShowingAccents) {
                    selectAccent();
                    hideAccentPopup();
                    return true;
                }
                cancelLongPressTimer();
                break;
        }
        return false;
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

    private boolean isAccentKey(int keyIndex) {
        if (keyIndex < 0 || keyIndex >= currentKeyboard.getKeys().size()) return false;
        Keyboard.Key key = currentKeyboard.getKeys().get(keyIndex);
        if (key.codes == null || key.codes.length == 0) return false;

        int code = key.codes[0];
        return (code == 97 || code == 65) ||
                (code == 101 || code == 69) ||
                (code == 105 || code == 73) ||
                (code == 111 || code == 79);
    }

    private char getAccentLetter(int keyIndex) {
        if (keyIndex < 0 || keyIndex >= currentKeyboard.getKeys().size()) return 0;
        Keyboard.Key key = currentKeyboard.getKeys().get(keyIndex);
        if (key.codes == null || key.codes.length == 0) return 0;

        int code = key.codes[0];
        switch (code) {
            case 97: case 65: return 'a';
            case 101: case 69: return 'e';
            case 105: case 73: return 'i';
            case 111: case 79: return 'o';
            default: return 0;
        }
    }

    private void startLongPressTimer(int keyIndex) {
        currentAccentLetter = getAccentLetter(keyIndex);
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
        currentAccentLetter = 0;
    }

    private String[] getCurrentAccentChars() {
        switch (currentAccentLetter) {
            case 'a':
                return (isCaps || isCapsLock) ? accentCharsAUpper : accentCharsA;
            case 'e':
                return (isCaps || isCapsLock) ? accentCharsEUpper : accentCharsE;
            case 'i':
                return (isCaps || isCapsLock) ? accentCharsIUpper : accentCharsI;
            case 'o':
                return (isCaps || isCapsLock) ? accentCharsOUpper : accentCharsO;
            default:
                return new String[0];
        }
    }

    private void showAccentPopup(int keyIndex) {
        isShowingAccents = true;
        selectedAccentIndex = 0;

        // Supprimer l'overlay qui cause des problèmes de layout
        // overlayView.setVisibility(View.VISIBLE);

        accentLayout = new LinearLayout(this);
        accentLayout.setOrientation(LinearLayout.HORIZONTAL);
        accentLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.parseColor("#424242"));
        background.setCornerRadius(dpToPx(8));
        background.setStroke(dpToPx(1), Color.parseColor("#616161"));
        accentLayout.setBackground(background);

        String[] charsToUse = getCurrentAccentChars();

        for (int i = 0; i < charsToUse.length; i++) {
            TextView textView = createAccentTextView(charsToUse[i], i == selectedAccentIndex);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            textView.setLayoutParams(lp);
            accentLayout.addView(textView);
        }

        accentPopup = new PopupWindow(
                accentLayout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        accentPopup.setFocusable(false);
        accentPopup.setTouchable(true);
        accentPopup.setOutsideTouchable(false); // Changé pour éviter les interactions externes

        // Positionnement direct sans attendre le layout
        positionAccentPopupDirect(keyIndex);
    }

    private void positionAccentPopupDirect(int keyIndex) {
        Keyboard.Key key = currentKeyboard.getKeys().get(keyIndex);

        // Mesurer le popup
        accentLayout.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int popupHeight = accentLayout.getMeasuredHeight();

        // Positionnement simple relatif à la touche
        int offsetX = key.x + (key.width / 2);
        int offsetY = key.y - popupHeight - dpToPx(8);

        // Afficher directement sans calculs complexes
        accentPopup.showAsDropDown(keyboardView, offsetX, offsetY);
    }

    private TextView createAccentTextView(String character, boolean isSelected) {
        TextView textView = new TextView(this);
        textView.setText(character);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        textView.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        textView.setGravity(Gravity.CENTER);
        textView.setMinWidth(dpToPx(42));
        textView.setMinHeight(dpToPx(42));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(6));

        if (isSelected) {
            bg.setColor(Color.parseColor("#5E5E5E"));
            textView.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.TRANSPARENT);
            textView.setTextColor(Color.parseColor("#F0F0F0"));
        }

        textView.setBackground(bg);
        return textView;
    }

    private void hideAccentPopup() {
        if (accentPopup != null && accentPopup.isShowing()) {
            accentPopup.dismiss();
        }
        // overlayView.setVisibility(View.GONE); // Supprimer cette ligne
        isShowingAccents = false;
        selectedAccentIndex = 0;
        accentLayout = null;
        currentAccentLetter = 0;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void updateAccentSelection(float x, float y) {
        if (accentLayout == null) return;

        int[] popupLocation = new int[2];
        accentLayout.getLocationOnScreen(popupLocation);

        float relativeX = x - popupLocation[0];
        String[] currentAccents = getCurrentAccentChars();
        int newIndex = (int) (relativeX / (accentLayout.getWidth() / currentAccents.length));
        newIndex = Math.max(0, Math.min(currentAccents.length - 1, newIndex));

        if (newIndex != selectedAccentIndex) {
            selectedAccentIndex = newIndex;
            updateAccentHighlight();
        }
    }

    private void updateAccentHighlight() {
        if (accentLayout == null) return;

        for (int i = 0; i < accentLayout.getChildCount(); i++) {
            TextView textView = (TextView) accentLayout.getChildAt(i);

            if (i == selectedAccentIndex) {
                GradientDrawable selectedBackground = new GradientDrawable();
                selectedBackground.setShape(GradientDrawable.RECTANGLE);
                selectedBackground.setColor(Color.parseColor("#616161"));
                selectedBackground.setCornerRadius(dpToPx(6));
                textView.setBackground(selectedBackground);
                textView.setTextColor(Color.WHITE);
                textView.setElevation(dpToPx(2));
            } else {
                textView.setBackground(null);
                textView.setTextColor(Color.parseColor("#E0E0E0"));
                textView.setElevation(0);
            }
        }
    }

    private void selectAccent() {
        String[] currentAccents = getCurrentAccentChars();
        if (selectedAccentIndex >= 0 && selectedAccentIndex < currentAccents.length) {
            String selectedChar = currentAccents[selectedAccentIndex];

            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.commitText(selectedChar, 1);
            }

            if (isCaps && !isCapsLock && !isSpecialMode) {
                isCaps = false;
                updateShiftKey();
            }
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
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
        if (lastKeyCodePressed == 32) {
            toggleKeyboard();
        }
    }

    @Override
    public void swipeRight() {
        if (lastKeyCodePressed == 32) {
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