package com.keyboard.keemo;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.TextUtils;
import android.util.Log;
import android.media.AudioManager;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.core.content.ContextCompat;
import android.view.HapticFeedbackConstants;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import java.lang.reflect.Field;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.Gravity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.keyboard.keemo.network.models.PredictionResponse;
import com.keyboard.keemo.network.models.PredictionRequest;
import com.keyboard.keemo.network.ApiClient;
import android.widget.FrameLayout;
import android.view.KeyEvent;
import android.view.WindowManager;

public class MyinputMethod extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    // Constantes pour les prédictions
    private static final int KEYCODE_PREDICTION_1 = -20;
    private static final int KEYCODE_PREDICTION_2 = -21;
    private static final int KEYCODE_PREDICTION_3 = -22;

    private TextView prediction1;
    private TextView prediction2;
    private TextView prediction3;

    // Variables pour les prédictions
    private String[] currentPredictions = new String[]{"", "", ""};
    private StringBuilder currentWord = new StringBuilder();
    private Handler predictionHandler = new Handler();
    private Runnable predictionRunnable;
    private String currentText = "";
    private static final long PREDICTION_DELAY = 3; // ms

    private KeyboardView keyboardView;
    private Keyboard mainKeyboard;
    private Keyboard specialKeyboard;
    private Keyboard specialKeyboard1;
    private Keyboard specialKeyboard2;
    private int currentSpecialPage = 1;
    private Keyboard aglcKeyboard;
    private Keyboard currentKeyboard;

    private boolean isCaps = false;
    private boolean isCapsLock = false;
    private long lastShiftTime = 0;
    private boolean isSpecialMode = false;
    private boolean isAGLCMode = false;

    private PopupWindow accentPopup;
    private boolean isShowingAccents = false;
    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private float initialTouchX, initialTouchY;
    private int selectedAccentIndex = 0;
    private LinearLayout accentLayout;

    // Lettres accentuées pour 'a'
    private String[] accentCharsA = {"à", "á", "â", "æ", "ä", "ā", "a\u030C"};
    private String[] accentCharsAUpper = {"À", "Á", "Â", "Æ", "Ä", "Ā", "A\u030C"};

    // Lettres accentuées pour 'e'
    private String[] accentCharsE = {"è", "é", "ê", "ě", "ë", "ē"};
    private String[] accentCharsEUpper = {"È", "É", "Ê", "Ě", "Ë", "Ē"};

    // Lettres accentuées pour 'i'
    private String[] accentCharsI = {"ì", "í", "î", "ï", "ī", "ǐ"};
    private String[] accentCharsIUpper = {"Ì", "Í", "Î", "Ï", "Ī", "Ǐ"};

    // Lettres accentuées pour 'o'
    private String[] accentCharsO = {"ó", "ò", "œ", "ô", "ō", "ö", "o\u030C"};
    private String[] accentCharsOUpper = {"Ó", "Ò", "Œ", "Ô", "Ō", "Ö", "O\u030C"};

    // Lettres accentuées pour 'u'
    private String[] accentCharsU = {"ù", "ú", "û", "ü", "ū", "ǔ"};
    private String[] accentCharsUUpper = {"Ù", "Ú", "Û", "Ü", "Ū", "Ǔ"};

    // ɛ
    private final String[] accentCharsƐ = {"ɛ̀", "ɛ́", "ɛ̂", "ɛ̈", "ɛ̄", "ɛ\u030C"};
    private final String[] accentCharsƐUpper = {"Ɛ̀", "Ɛ́", "Ɛ̂", "Ɛ̈", "Ɛ̄", "Ɛ\u030C"};

    // ə
    private final String[] accentCharsƏ = {"ə̀", "ə́", "ə̂", "ə̈", "ə̄", "ə\u030C"};
    private final String[] accentCharsƏUpper = {"Ə̀", "Ə́", "Ə̂", "Ə̈", "Ə̄", "Ə\u030C"};

    // ɔ
    private final String[] accentCharsƆ = {"ɔ̀", "ɔ́", "ɔ̂", "ɔ̈", "ɔ̄", "ɔ\u030C"};
    private final String[] accentCharsƆUpper = {"Ɔ̀", "Ɔ́", "Ɔ̂", "Ɔ̈", "Ɔ̄", "Ɔ\u030C"};

    // w
    private final String[] accentCharsW = {"ẁ", "ẃ", "ŵ", "ẅ", "w̄", "w\u030C"};
    private final String[] accentCharsWUpper = {"Ẁ", "Ẃ", "Ŵ", "Ẅ", "W̄", "W\u030C"};

    private char currentAccentLetter = 0;

    private static final int KEYCODE_SWITCH_TO_SPECIAL = -11;
    private static final int KEYCODE_SWITCH_TO_MAIN = -10;
    private static final int KEYCODE_SWITCH_TO_AGLC = -12;
    private static final long LONG_PRESS_DELAY = 500;

    private FrameLayout mainContainer;
    private android.util.Log Log;

    @Override
    public View onCreateInputView() {
        mainContainer = new FrameLayout(this);

        // 1. Initialisation des claviers (MODIFIER cette partie)
        mainKeyboard = new Keyboard(this, R.xml.keys);
        specialKeyboard1 = new Keyboard(this, R.xml.special_chars);     // Page 1
        specialKeyboard2 = new Keyboard(this, R.xml.special_chars_2);   // Page 2
        aglcKeyboard = new Keyboard(this, R.xml.aglc_keys);

        // Par défaut, specialKeyboard pointe vers la page 1
        specialKeyboard = specialKeyboard1;
        currentKeyboard = mainKeyboard;
        currentSpecialPage = 1;


        // 2. Création de la barre de prédictions
        LinearLayout predictionBar = new LinearLayout(this);
        predictionBar.setOrientation(LinearLayout.HORIZONTAL);
        predictionBar.setBackgroundColor(Color.parseColor("#E0E0E0"));
        predictionBar.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        predictionBar.setGravity(Gravity.CENTER_VERTICAL);

        // Paramètres de layout pour la barre
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40)
        );
        predictionBar.setLayoutParams(barParams);

        // 3. Création des TextViews de prédiction
        LinearLayout.LayoutParams predictionParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f
        );
        predictionParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);

        View.OnClickListener predictionClickListener = view -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                String word = ((TextView) view).getText().toString();
                if (!TextUtils.isEmpty(word)) {
                    ic.commitText( word + " ", 1);
                    updateCurrentText(ic);
                    requestNextWordPrediction();
                }
            }
        };

        // Prédiction 1
        prediction1 = new TextView(this);
        prediction1.setLayoutParams(predictionParams);
        prediction1.setGravity(Gravity.CENTER);
        prediction1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        prediction1.setTextColor(Color.parseColor("#424242"));
        prediction1.setBackground(createPredictionBackground());
        prediction1.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        prediction1.setOnClickListener(predictionClickListener);
        prediction1.setClickable(true);
        prediction1.setFocusable(true);

        // Prédiction 2
        prediction2 = new TextView(this);
        prediction2.setLayoutParams(predictionParams);
        prediction2.setGravity(Gravity.CENTER);
        prediction2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        prediction2.setTextColor(Color.parseColor("#424242"));
        prediction2.setBackground(createPredictionBackground());
        prediction2.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        prediction2.setOnClickListener(predictionClickListener);
        prediction2.setClickable(true);
        prediction2.setFocusable(true);

        // Prédiction 3
        prediction3 = new TextView(this);
        prediction3.setLayoutParams(predictionParams);
        prediction3.setGravity(Gravity.CENTER);
        prediction3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        prediction3.setTextColor(Color.parseColor("#424242"));
        prediction3.setBackground(createPredictionBackground());
        prediction3.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        prediction3.setOnClickListener(predictionClickListener);
        prediction3.setClickable(true);
        prediction3.setFocusable(true);

        // Ajout des prédictions à la barre
        predictionBar.addView(prediction1);
        predictionBar.addView(prediction2);
        predictionBar.addView(prediction3);

        // 4. Création du KeyboardView
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        keyboardView.setKeyboard(currentKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(true);
        keyboardView.setOnTouchListener((v, event) -> handleTouchEvent(event));

        // 5. Assemblage final : barre de prédictions + clavier
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(predictionBar);
        mainLayout.addView(keyboardView);

        mainContainer.addView(mainLayout);
        return mainContainer;
    }

    // Méthode helper pour créer le background des prédictions
    private GradientDrawable createPredictionBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.parseColor("#F5F5F5"));
        background.setCornerRadius(dpToPx(4));
        background.setStroke(dpToPx(1), Color.parseColor("#CCCCCC"));
        return background;
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        if (accentPopup != null && isShowingAccents) {
            accentPopup.dismiss();
            isShowingAccents = false;
        }
    }

    private void checkAndApplyAutoCapitalization() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            CharSequence textBeforeCursor = inputConnection.getTextBeforeCursor(100, 0);
            if (shouldAutoCapitalize(textBeforeCursor)) {
                isCaps = true;
                updateShiftKey();
            }
        }
    }

    private boolean shouldAutoCapitalize(CharSequence textBefore) {
        if (TextUtils.isEmpty(textBefore)) {
            return true; // Début de texte
        }

        String text = textBefore.toString().trim();
        if (text.isEmpty()) {
            return true; // Début de ligne ou après espaces
        }

        // Vérifier si le texte se termine par un point suivi d'espaces
        if (text.matches(".*\\.[\\s]*$")) {
            return true;
        }

        // Vérifier d'autres signes de ponctuation de fin de phrase
        if (text.matches(".*[.!?][\\s]*$")) {
            return true;
        }

        return false;
    }

    private boolean handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getX();
                initialTouchY = event.getY();

                int keyIndex = getKeyIndex(event.getX(), event.getY());
                if (keyIndex != -1) {
                    Keyboard.Key key = currentKeyboard.getKeys().get(keyIndex);
                    // Ne pas gérer les long press pour les prédictions
                    if (isAccentKey(keyIndex) &&
                            key.codes[0] != KEYCODE_PREDICTION_1 &&
                            key.codes[0] != KEYCODE_PREDICTION_2 &&
                            key.codes[0] != KEYCODE_PREDICTION_3) {
                        startLongPressTimer(keyIndex);
                    }
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

    private void updatePredictionKeys() {
        if (currentKeyboard == null || keyboardView == null) return;

        if (prediction1 != null && prediction2 != null && prediction3 != null) {
            prediction1.setText(currentPredictions[0]);
            prediction2.setText(currentPredictions[1]);
            prediction3.setText(currentPredictions[2]);

            // Rendre visible ou invisible selon le contenu
            prediction1.setVisibility(currentPredictions[0].isEmpty() ? View.INVISIBLE : View.VISIBLE);
            prediction2.setVisibility(currentPredictions[1].isEmpty() ? View.INVISIBLE : View.VISIBLE);
            prediction3.setVisibility(currentPredictions[2].isEmpty() ? View.INVISIBLE : View.VISIBLE);
        }

        for (Keyboard.Key key : currentKeyboard.getKeys()) {
            if (key.codes == null || key.codes.length == 0) continue;

            int code = key.codes[0];
            if (code == KEYCODE_PREDICTION_1) {
                key.label = currentPredictions[0];
                key.text = currentPredictions[0];
            } else if (code == KEYCODE_PREDICTION_2) {
                key.label = currentPredictions[1];
                key.text = currentPredictions[1];
            } else if (code == KEYCODE_PREDICTION_3) {
                key.label = currentPredictions[2];
                key.text = currentPredictions[2];
            }
        }
        keyboardView.invalidateAllKeys();
    }

    private void resetPredictions() {
        currentPredictions[0] = "";
        currentPredictions[1] = "";
        currentPredictions[2] = "";
        updatePredictionKeys();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        resetPredictions();

        // Récupérer le texte existant pour les prédictions initiales
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence currentText = ic.getTextBeforeCursor(100, 0);
            if (TextUtils.isEmpty(currentText) || currentText.toString().trim().isEmpty()) {
                resetPredictions(); // ← vide tout
            } else {
                requestNextWordPrediction();
            }

        }
    }

    private void switchToSpecialPage1() {
        if (specialKeyboard == specialKeyboard1) return;

        specialKeyboard = specialKeyboard1;
        currentSpecialPage = 1;

        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        animateKeyboardTransition(() -> {
            currentKeyboard = specialKeyboard1;
            keyboardView.setKeyboard(currentKeyboard);
            keyboardView.invalidateAllKeys();
        });
    }

    private void switchToSpecialPage2() {
        if (specialKeyboard == specialKeyboard2) return;

        specialKeyboard = specialKeyboard2;
        currentSpecialPage = 2;

        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        animateKeyboardTransition(() -> {
            currentKeyboard = specialKeyboard2;
            keyboardView.setKeyboard(currentKeyboard);
            keyboardView.invalidateAllKeys();
        });
    }

    private void requestNextWordPrediction() {
        predictionHandler.removeCallbacksAndMessages(null);

        PredictionRequest request = new PredictionRequest(currentText.trim(), 3);

        ApiClient.INSTANCE.getPredictionApi().getPredictions(request).enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PredictionResponse.Prediction> predictions = response.body().predictions;

                    if (predictions != null && !predictions.isEmpty()) {
                        for (int i = 0; i < Math.min(3, predictions.size()); i++) {
                            currentPredictions[i] = predictions.get(i).word;
                        }
                        updatePredictionKeys();
                    } else {
                        resetPredictions();
                    }
                } else {
                    resetPredictions();
                }
                if (TextUtils.isEmpty(currentText.trim())) {
                    resetPredictions();
                    return;
                }

            }

            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                resetPredictions();
            }
        });
    }

    private int getKeyIndex(float x, float y) {
        if (currentKeyboard == null) return -1;

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
        return (code == 97 || code == 65) ||    // a/A
                (code == 101 || code == 69) ||   // e/E
                (code == 105 || code == 73) ||   // i/I
                (code == 111 || code == 79) ||   // o/O
                (code == 117 || code == 85) ||   // u/U
                (code == 603 || code == 399) ||  // ɛ/Ɛ
                (code == 601 || code == 398) ||  // ə/Ə
                (code == 596 || code == 390) ||  // ɔ/Ɔ
                (code == 7813 || code == 7812);  // ẅ/Ẅ
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
            case 117: case 85: return 'u';
            case 603: case 399: return 'ɛ';
            case 601: case 398: return 'ə';
            case 596: case 390: return 'ɔ';
            case 7813: case 7812: return 'ẅ';
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
            case 'u':
                return (isCaps || isCapsLock) ? accentCharsUUpper : accentCharsU;
            case 'ɛ':
                return (isCaps || isCapsLock) ? accentCharsƐUpper : accentCharsƐ;
            case 'ə':
                return (isCaps || isCapsLock) ? accentCharsƏUpper : accentCharsƏ;
            case 'ɔ':
                return (isCaps || isCapsLock) ? accentCharsƆUpper : accentCharsƆ;
            case 'ẅ':
                return (isCaps || isCapsLock) ? accentCharsWUpper : accentCharsW;
            default:
                return new String[0];
        }
    }

    private void showAccentPopup(int keyIndex) {
        isShowingAccents = true;
        selectedAccentIndex = 0;

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
        accentPopup.setOutsideTouchable(false);

        positionAccentPopupDirect(keyIndex);
    }

    private void positionAccentPopupDirect(int keyIndex) {
        Keyboard.Key key = currentKeyboard.getKeys().get(keyIndex);

        accentLayout.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int popupWidth = accentLayout.getMeasuredWidth();
        int popupHeight = accentLayout.getMeasuredHeight();

        // Calculer la position relative au KeyboardView - centré horizontalement sur la touche
        int popupX = key.x + (key.width / 2) - (popupWidth / 2);
        // Positionner au-dessus de la touche avec un petit espace
        int popupY = key.y - popupHeight - dpToPx(12);

        // Ajuster si le popup dépasse à gauche
        if (popupX < dpToPx(4)) {
            popupX = dpToPx(4);
        }
        // Ajuster si le popup dépasse à droite
        else if (popupX + popupWidth > keyboardView.getWidth() - dpToPx(4)) {
            popupX = keyboardView.getWidth() - popupWidth - dpToPx(4);
        }

        // Si le popup dépasse en haut, le garder quand même au-dessus mais ajuster
        if (popupY < dpToPx(4)) {
            popupY = dpToPx(4);
        }

        // Afficher le popup en position relative au KeyboardView
        accentPopup.showAtLocation(
                keyboardView,
                Gravity.TOP | Gravity.LEFT,
                popupX,
                popupY
        );
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

            if (isCaps && !isCapsLock && !isSpecialMode && !isAGLCMode) {
                isCaps = false;
                updateShiftKey();
            }
        }
    }

    private void handleBackspace(InputConnection ic) {
        CharSequence selectedText = ic.getSelectedText(0);

        if (selectedText != null && selectedText.length() > 0) {
            ic.commitText("", 1);
        } else {
            ic.deleteSurroundingText(1, 0);
        }

        // Met à jour la phrase actuelle
        updateCurrentText(ic);

        // Lancer nouvelle prédiction
        requestNextWordPrediction();
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) {
            am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }

        if (isShowingAccents) return;

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) return;
        Log.d("KEY", "Pressed key code: " + primaryCode);
        // Ignorer les touches désactivées
        if (primaryCode == -999) return;

        // Gestion des touches de prédiction
        if (primaryCode == KEYCODE_PREDICTION_1 ||
                primaryCode == KEYCODE_PREDICTION_2 ||
                primaryCode == KEYCODE_PREDICTION_3) {

            int index = -1;
            if (primaryCode == KEYCODE_PREDICTION_1) index = 0;
            else if (primaryCode == KEYCODE_PREDICTION_2) index = 1;
            else if (primaryCode == KEYCODE_PREDICTION_3) index = 2;

            if (index >= 0 && index < currentPredictions.length &&
                    !TextUtils.isEmpty(currentPredictions[index])) {

                String word = currentPredictions[index];

                // ✅ Ajoute un espace avant le mot prédit
                inputConnection.commitText(" " + word + " ", 1);

                updateCurrentText(inputConnection);
                requestNextWordPrediction();
            }

            return;
        }

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                handleBackspace(inputConnection);
                break;

            case Keyboard.KEYCODE_SHIFT:
                handleShiftToggle();
                break;

            case Keyboard.KEYCODE_DONE:
                inputConnection.sendKeyEvent(
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                );
                // Activer les majuscules après un retour à la ligne
                isCaps = true;
                updateShiftKey();
                break;

            case KEYCODE_SWITCH_TO_SPECIAL:
                switchToSpecialKeyboard();
                break;

            case KEYCODE_SWITCH_TO_MAIN:
                switchToMainKeyboard();
                break;

            case KEYCODE_SWITCH_TO_AGLC:
                // Logique de bascule améliorée
                if (currentKeyboard == aglcKeyboard) {
                    switchToMainKeyboard();
                } else {
                    switchToAGLCKeyboard();
                }
                break;

            case -101: // Code pour "1/2" - aller à la page 2
                if (isSpecialMode && currentSpecialPage == 1) {
                    switchToSpecialPage2();
                }
                break;

            case -102: // Code pour "2/2" - retourner à la page 1
                if (isSpecialMode && currentSpecialPage == 2) {
                    switchToSpecialPage1();
                }
                break;

            case 32: // Espace
                handleSpace(inputConnection);
                break;

            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code) && !isSpecialMode) {

                }
                inputConnection.commitText(String.valueOf(code), 1);
                if (isCaps && !isCapsLock && !isSpecialMode && !isAGLCMode) {
                    isCaps = false;
                    updateShiftKey();
                }
                currentWord.append(code);
                updateCurrentText(inputConnection); // ← met à jour currentText
                requestNextWordPrediction();        // ← prédit le mot suivant
                break;
        }
    }

    private void handleSpace(InputConnection ic) {
        ic.commitText(" ", 1);

        // Reconstruire la phrase avant le curseur
        CharSequence beforeCursor = ic.getTextBeforeCursor(200, 0);
        if (beforeCursor != null) {
            currentText = beforeCursor.toString();
        } else {
            currentText = "";
        }

        currentWord.setLength(0); // plus besoin de currentWord pour cette logique

        requestNextWordPrediction();  // nouvelle fonction pour prédire le mot suivant
    }

    private void updateCurrentText(InputConnection ic) {
        CharSequence textBeforeCursor = ic.getTextBeforeCursor(200, 0);
        this.currentText = textBeforeCursor != null ? textBeforeCursor.toString() : "";
    }

    private void switchToAGLCKeyboard() {
        if (currentKeyboard == aglcKeyboard) return;

        // Mettre à jour l'état avant de changer de clavier
        isAGLCMode = true;
        isSpecialMode = false;

        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        animateKeyboardTransition(() -> {
            currentKeyboard = aglcKeyboard;
            keyboardView.setKeyboard(currentKeyboard);
            keyboardView.invalidateAllKeys();
        });
    }

    // Optionnel : Méthode helper pour obtenir le clavier spécial actuel
    private Keyboard getCurrentSpecialKeyboard() {
        return currentSpecialPage == 1 ? specialKeyboard1 : specialKeyboard2;
    }

    // Optionnel : Méthode pour reset la page des symboles quand on quitte le mode spécial
    private void resetSpecialKeyboardPage() {
        currentSpecialPage = 1;
        specialKeyboard = specialKeyboard1;
    }
    private void switchToMainKeyboard() {
        if (currentKeyboard == mainKeyboard) return;

        // Réinitialiser les états
        isAGLCMode = false;
        isSpecialMode = false;
        resetSpecialKeyboardPage(); // AJOUTER cette ligne

        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        animateKeyboardTransition(() -> {
            currentKeyboard = mainKeyboard;
            keyboardView.setKeyboard(currentKeyboard);
            updateShiftKey();
        });
    }

    private void switchToSpecialKeyboard() {
        // Si on n'est pas en mode spécial, aller à la page 1
        if (!isSpecialMode) {
            specialKeyboard = specialKeyboard1;
            currentSpecialPage = 1;

            isAGLCMode = false;
            isSpecialMode = true;

            keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            animateKeyboardTransition(() -> {
                currentKeyboard = specialKeyboard1;
                keyboardView.setKeyboard(currentKeyboard);
                keyboardView.invalidateAllKeys();
            });
        }
    }

    private void animateKeyboardTransition(Runnable switchAction) {
        // Create animation that will be applied through KeyboardView's drawing mechanism
        AlphaAnimation fadeLetters = new AlphaAnimation(1.0f, 0.85f);
        fadeLetters.setDuration(100);

        fadeLetters.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                switchAction.run();

                AlphaAnimation fadeIn = new AlphaAnimation(0.85f, 1.0f);
                fadeIn.setDuration(120);
                fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

                // Apply fade-in to key content only
                animateKeyContent(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Apply fade-out to key content only
        animateKeyContent(fadeLetters);
    }

    private void animateKeyContent(Animation animation) {
        // Force KeyboardView to redraw with animation effect on keys only
        keyboardView.invalidate();
        keyboardView.startAnimation(animation);
    }

    private void handleShiftToggle() {
        if (isSpecialMode || isAGLCMode) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShiftTime < 500) {
            // Double clic → CapsLock
            isCapsLock = !isCapsLock;
            isCaps = isCapsLock;
        } else {
            // Clic simple → Maj temporaire
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

    private void updateCurrentWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence beforeCursor = ic.getTextBeforeCursor(30, 0);
            if (beforeCursor != null) {
                String[] words = beforeCursor.toString().split("\\s+");
                if (words.length > 0) {
                    currentWord.setLength(0);
                    currentWord.append(words[words.length - 1]);
                } else {
                    currentWord.setLength(0);
                }
            }
        }
    }

    private void updateShiftKey() {
        if (currentKeyboard == null) return;

        for (Keyboard.Key key : currentKeyboard.getKeys()) {
            if (key.codes[0] == Keyboard.KEYCODE_SHIFT) {
                if (isCapsLock) {
                    key.icon = ContextCompat.getDrawable(this, R.drawable.ic_shift_filled);
                } else {
                    key.icon = ContextCompat.getDrawable(this, R.drawable.ic_shift_outline);
                }
            } else if (key.label != null && key.label.length() == 1) {
                char c = key.label.charAt(0);
                if (Character.isLetter(c)) {
                    char updatedChar = (isCaps || isCapsLock)
                            ? Character.toUpperCase(c)
                            : Character.toLowerCase(c);
                    key.label = String.valueOf(updatedChar);
                    key.codes[0] = (int) updatedChar;
                }
            }
        }

        keyboardView.invalidateAllKeys();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        predictionHandler.removeCallbacksAndMessages(null);
    }
    @Override
    public void onPress(int primaryCode) {
        // lastKeyCodePressed = primaryCode;
    }

    @Override
    public void onRelease(int primaryCode) {
        // lastKeyCodePressed = 0;
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
        // Pas d'action
    }

    @Override
    public void swipeRight() {
        // Pas d'action
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