package com.keyboard.keemo;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import android.widget.FrameLayout;

public class MyinputMethod extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    // Constantes pour les prédictions
    private static final int KEYCODE_PREDICTION_1 = -20;
    private static final int KEYCODE_PREDICTION_2 = -21;
    private static final int KEYCODE_PREDICTION_3 = -22;

    // Variables pour les prédictions
    private List<String> currentPredictions = new ArrayList<>();
    private Handler predictionHandler = new Handler();
    private Runnable predictionRunnable;
    private static final long PREDICTION_DELAY = 300; // ms

    // Interface pour l'API de prédiction
    interface PredictionApi {
        @POST("/predict")
        Call<PredictionResponse> getPredictions(@Body PredictionRequest request);
    }

    // Modèles de données
    static class PredictionRequest {
        String phrase;
        int top_k = 3;

        PredictionRequest(String phrase) {
            this.phrase = phrase;
        }
    }

    static class PredictionResponse {
        List<Prediction> predictions;
        String message;

        static class Prediction {
            String word;
            double probability;
        }
    }

    private KeyboardView keyboardView;
    private Keyboard mainKeyboard;
    private Keyboard specialKeyboard;
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
    private String[] accentCharsA = {"à", "á", "â", "æ", "ä", "ā", "a\u011A"};
    private String[] accentCharsAUpper = {"À", "Á", "Â", "Æ", "Ä", "Ā", "A\u011A"};

    // Lettres accentuées pour 'e'
    private String[] accentCharsE = {"è", "é", "ê", "ě", "ë", "ē"};
    private String[] accentCharsEUpper = {"È", "É", "Ê", "Ě", "Ë", "Ē"};

    // Lettres accentuées pour 'i'
    private String[] accentCharsI = {"ì", "í", "î", "ï", "ī", "ǐ"};
    private String[] accentCharsIUpper = {"Ì", "Í", "Î", "Ï", "Ī", "Ǐ"};

    // Lettres accentuées pour 'o'
    private String[] accentCharsO = {"ó", "ò", "œ", "ô", "ō", "ö", "o\u011A"};
    private String[] accentCharsOUpper = {"Ó", "Ò", "Œ", "Ô", "Ō", "Ö", "O\u011A"};

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
    private View overlayView;

    @Override
    public View onCreateInputView() {
        mainContainer = new FrameLayout(this);

        // Initialiser tous les claviers
        mainKeyboard = new Keyboard(this, R.xml.keys);
        specialKeyboard = new Keyboard(this, R.xml.special_chars);
        aglcKeyboard = new Keyboard(this, R.xml.aglc_keys);

        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        mainContainer.addView(keyboardView);

        // Initialiser les prédictions vides
        currentPredictions.add("");
        currentPredictions.add("");
        currentPredictions.add("");
        updatePredictionKeys();

        // Application de la police JustSans
        try {
            Typeface justSansFont = Typeface.createFromAsset(getAssets(), "fonts/justsans.ttf");

            // Appliquer la police via réflexion pour KeyboardView
            Field typefaceField = KeyboardView.class.getDeclaredField("mKeyTextStyle");
            typefaceField.setAccessible(true);
            Paint paint = new Paint();
            paint.setTypeface(justSansFont);
            typefaceField.set(keyboardView, paint);
        } catch (Exception e) {
            // En cas d'échec, utiliser la police par défaut
            e.printStackTrace();
        }

        // Créer l'overlay semi-transparent
        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.argb(100, 0, 0, 0));
        overlayView.setVisibility(View.GONE);
        overlayView.setOnClickListener(v -> hideAccentPopup());
        mainContainer.addView(overlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Démarrer avec le clavier principal et vérifier si majuscules nécessaires
        currentKeyboard = mainKeyboard;
        isAGLCMode = false;
        isSpecialMode = false;

        // Vérifier si on doit commencer en majuscules
        checkAndApplyAutoCapitalization();

        keyboardView.setKeyboard(currentKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(true);

        // Désactiver les touches communes dans le clavier AGLC
        disableCommonKeysInAGLC();

        keyboardView.setOnTouchListener((v, event) -> handleTouchEvent(event));

        return mainContainer;
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

    private void disableCommonKeysInAGLC() {
        if (aglcKeyboard == null) return;

        // Codes des touches communes à désactiver dans AGLC
        int[] commonKeyCodes = {
                97, 114, 116, 121, 117, 105, 111, // a, r, t, y, u, i, o
                115, 102, 104, 107, 108, 109, // s, f, h, k, l, m
                118, 98 // v, b
        };

        for (Keyboard.Key key : aglcKeyboard.getKeys()) {
            if (key.codes != null && key.codes.length > 0) {
                for (int commonCode : commonKeyCodes) {
                    if (key.codes[0] == commonCode) {
                        // Désactiver la touche en modifiant son apparence
                        key.codes[0] = -999; // Code inactif
                        break;
                    }
                }
            }
        }
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

        for (Keyboard.Key key : currentKeyboard.getKeys()) {
            if (key.codes == null || key.codes.length == 0) continue;

            int code = key.codes[0];
            if (code == KEYCODE_PREDICTION_1) {
                key.label = currentPredictions.get(0);
                key.text = currentPredictions.get(0);
            } else if (code == KEYCODE_PREDICTION_2) {
                key.label = currentPredictions.get(1);
                key.text = currentPredictions.get(1);
            } else if (code == KEYCODE_PREDICTION_3) {
                key.label = currentPredictions.get(2);
                key.text = currentPredictions.get(2);
            }
        }
        keyboardView.invalidateAllKeys();
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

    private void fetchPredictions(String input) {
        // Annuler la requête précédente
        predictionHandler.removeCallbacks(predictionRunnable);

        predictionRunnable = new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(input)) {
                    resetPredictions();
                    return;
                }

                // Créer le client Retrofit
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://10.0.2.2:8000") // URL de votre API
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                PredictionApi api = retrofit.create(PredictionApi.class);
                PredictionRequest request = new PredictionRequest(input);

                api.getPredictions(request).enqueue(new Callback<PredictionResponse>() {
                    @Override
                    public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<PredictionResponse.Prediction> predictions = response.body().predictions;
                            if (predictions != null && !predictions.isEmpty()) {
                                currentPredictions.clear();
                                for (int i = 0; i < 3; i++) {
                                    if (i < predictions.size()) {
                                        currentPredictions.add(predictions.get(i).word);
                                    } else {
                                        currentPredictions.add("");
                                    }
                                }
                                updatePredictionKeys();
                                return;
                            }
                        }
                        resetPredictions();
                    }

                    @Override
                    public void onFailure(Call<PredictionResponse> call, Throwable t) {
                        Log.e("Prediction", "API error: " + t.getMessage());
                        resetPredictions();
                    }
                });
            }
        };

        // Délai pour éviter les requêtes trop fréquentes
        predictionHandler.postDelayed(predictionRunnable, PREDICTION_DELAY);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        resetPredictions();

        // Récupérer le texte existant pour les prédictions initiales
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence currentText = ic.getTextBeforeCursor(100, 0);
            if (currentText != null) {
                fetchPredictions(currentText.toString());
            }
        }
    }

    private void resetPredictions() {
        currentPredictions.clear();
        currentPredictions.add("");
        currentPredictions.add("");
        currentPredictions.add("");
        updatePredictionKeys();
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

        int popupHeight = accentLayout.getMeasuredHeight();
        int offsetX = key.x + (key.width / 2);
        int offsetY = key.y - popupHeight - dpToPx(8);

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

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (isShowingAccents) return;

        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) return;

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

            if (index >= 0 && index < currentPredictions.size() &&
                    !TextUtils.isEmpty(currentPredictions.get(index))) {

                String word = currentPredictions.get(index);
                inputConnection.commitText(word, 1);

                // Réinitialiser les prédictions après sélection
                resetPredictions();
            }
            return;
        }

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                CharSequence selectedText = inputConnection.getSelectedText(0);
                if (TextUtils.isEmpty(selectedText)) {
                    inputConnection.deleteSurroundingText(1, 0);
                } else {
                    inputConnection.commitText("", 1);
                }

                // Vérifier si on doit activer les majuscules après suppression
                checkAndApplyAutoCapitalization();
                break;

            case Keyboard.KEYCODE_SHIFT:
                handleShiftToggle();
                break;

            case Keyboard.KEYCODE_DONE:
                inputConnection.sendKeyEvent(
                        new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)
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
                // Alternance entre clavier principal et AGLC
                if (currentKeyboard == aglcKeyboard) {
                    switchToMainKeyboard();
                } else {
                    switchToAGLCKeyboard();
                }
                break;

            case 32: // Espace
                inputConnection.commitText(" ", 1);
                break;

            case 46: // Point
                inputConnection.commitText(".", 1);
                // Activer les majuscules après un point
                isCaps = true;
                updateShiftKey();
                break;

            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code) && !isSpecialMode) {
                    // Ne pas modifier la casse en mode AGLC
                    if (!isAGLCMode) {
                        code = (isCaps || isCapsLock) ?
                                Character.toUpperCase(code) :
                                Character.toLowerCase(code);
                    }
                }
                inputConnection.commitText(String.valueOf(code), 1);

                // Ne pas désactiver Caps Lock en mode AGLC
                if (!isAGLCMode && isCaps && !isCapsLock && !isSpecialMode) {
                    isCaps = false;
                    updateShiftKey();
                }
                break;
        }

        // Après avoir traité la touche, récupérer le texte pour les prédictions
        CharSequence currentText = inputConnection.getTextBeforeCursor(100, 0);
        if (currentText != null) {
            fetchPredictions(currentText.toString());
        }
    }

    private void switchToAGLCKeyboard() {
        if (currentKeyboard == aglcKeyboard) return;
        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        isAGLCMode = true;
        isSpecialMode = false;
        animateKeyboardTransition(() -> {
            currentKeyboard = aglcKeyboard;
            keyboardView.setKeyboard(currentKeyboard);
            keyboardView.invalidateAllKeys();
        });
    }

    private void switchToMainKeyboard() {
        if (currentKeyboard == mainKeyboard) return;
        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        isAGLCMode = false;
        isSpecialMode = false;
        animateKeyboardTransition(() -> {
            currentKeyboard = mainKeyboard;
            keyboardView.setKeyboard(currentKeyboard);
            updateShiftKey();
        });
    }

    private void switchToSpecialKeyboard() {
        if (currentKeyboard == specialKeyboard) return;
        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        isAGLCMode = false;
        isSpecialMode = true;
        animateKeyboardTransition(() -> {
            currentKeyboard = specialKeyboard;
            keyboardView.setKeyboard(currentKeyboard);
            keyboardView.invalidateAllKeys();
        });
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
        if (isSpecialMode || isAGLCMode) return;

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