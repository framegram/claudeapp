package com.example.circuittimerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String KEY_WEIGHT_KG = "user_weight_kg";
    private static final String KEY_HEIGHT_CM = "user_height_cm";
    private static final String KEY_BGM_PLAYLIST = "bgm_playlist";

    // 種目ごとの推奨ダンベル重量(体重に対する割合)[初心者, 中級者, 上級者]
    private static final Map<String, float[]> WEIGHT_GUIDE_PERCENT = new LinkedHashMap<>();
    private static final float[] DEFAULT_WEIGHT_GUIDE_PERCENT = {0.08f, 0.15f, 0.22f};

    static {
        WEIGHT_GUIDE_PERCENT.put("ダンベルベンチプレス", new float[]{0.15f, 0.25f, 0.35f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルキックバック", new float[]{0.04f, 0.07f, 0.10f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルデッドリフト", new float[]{0.20f, 0.35f, 0.50f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルカール", new float[]{0.06f, 0.10f, 0.15f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルショルダープレス", new float[]{0.10f, 0.18f, 0.28f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルランジ", new float[]{0.10f, 0.18f, 0.28f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルロウ", new float[]{0.15f, 0.25f, 0.35f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルトライセプスエクステンション", new float[]{0.05f, 0.09f, 0.13f});
        WEIGHT_GUIDE_PERCENT.put("ダンベルスクワットプレス", new float[]{0.10f, 0.18f, 0.28f});
    }

    private EditText weightKgInput, heightCmInput, bgmUrlInput;
    private TextView bmiText, caloriesText, proteinText, fatText, carbsText, weightGuideHint;
    private LinearLayout bgmListContainer, weightGuideContainer;

    private final List<String> bgmPlaylist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        weightKgInput = findViewById(R.id.weightKgInput);
        heightCmInput = findViewById(R.id.heightCmInput);
        bgmUrlInput = findViewById(R.id.bgmUrlInput);
        bmiText = findViewById(R.id.bmiText);
        caloriesText = findViewById(R.id.caloriesText);
        proteinText = findViewById(R.id.proteinText);
        fatText = findViewById(R.id.fatText);
        carbsText = findViewById(R.id.carbsText);
        weightGuideHint = findViewById(R.id.weightGuideHint);
        weightGuideContainer = findViewById(R.id.weightGuideContainer);
        bgmListContainer = findViewById(R.id.bgmListContainer);

        findViewById(R.id.saveProfileBtn).setOnClickListener(v -> saveProfile());
        findViewById(R.id.addBgmBtn).setOnClickListener(v -> addBgmUrl());
        findViewById(R.id.closeSettingsBtn).setOnClickListener(v -> finish());

        loadProfile();
        loadBgmPlaylist();
        refreshBgmList();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void loadProfile() {
        SharedPreferences prefs = prefs();
        float weight = prefs.getFloat(KEY_WEIGHT_KG, -1f);
        float height = prefs.getFloat(KEY_HEIGHT_CM, -1f);
        if (weight > 0) {
            weightKgInput.setText(trimNumber(weight));
        }
        if (height > 0) {
            heightCmInput.setText(trimNumber(height));
        }
        if (weight > 0 && height > 0) {
            updateNutritionDisplay(weight, height);
        }
        if (weight > 0) {
            updateWeightGuide(weight);
        }
    }

    private void saveProfile() {
        String weightStr = weightKgInput.getText().toString();
        String heightStr = heightCmInput.getText().toString();
        if (weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(this, "体重と身長を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float weight = Float.parseFloat(weightStr);
            float height = Float.parseFloat(heightStr);
            if (weight <= 0 || height <= 0) {
                Toast.makeText(this, "有効な数値を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs().edit();
            editor.putFloat(KEY_WEIGHT_KG, weight);
            editor.putFloat(KEY_HEIGHT_CM, height);
            editor.apply();

            updateNutritionDisplay(weight, height);
            updateWeightGuide(weight);
            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "有効な数値を入力してください", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNutritionDisplay(float weightKg, float heightCm) {
        double bmi = weightKg / Math.pow(heightCm / 100.0, 2);

        // 筋トレをしている人向けの目安栄養素(体重ベースの簡易計算・医学的な診断ではありません)
        double calories = weightKg * 33.0;
        double proteinG = weightKg * 2.0;
        double fatG = weightKg * 1.0;
        double carbsG = Math.max(0, (calories - proteinG * 4 - fatG * 9) / 4);

        bmiText.setText(String.format(Locale.getDefault(), "BMI: %.1f", bmi));
        caloriesText.setText(String.format(Locale.getDefault(), "目安カロリー: %.0f kcal/日", calories));
        proteinText.setText(String.format(Locale.getDefault(), "目安タンパク質: %.0f g/日", proteinG));
        fatText.setText(String.format(Locale.getDefault(), "目安脂質: %.0f g/日", fatG));
        carbsText.setText(String.format(Locale.getDefault(), "目安炭水化物: %.0f g/日", carbsG));
    }

    private void updateWeightGuide(float weightKg) {
        weightGuideHint.setVisibility(View.GONE);
        weightGuideContainer.removeAllViews();

        for (MainActivity.WorkoutSet set : MainActivity.getWorkoutPlan()) {
            for (MainActivity.Exercise exercise : set.exercises) {
                float[] percent = WEIGHT_GUIDE_PERCENT.getOrDefault(exercise.name, DEFAULT_WEIGHT_GUIDE_PERCENT);
                double beginner = roundToHalf(weightKg * percent[0]);
                double intermediate = roundToHalf(weightKg * percent[1]);
                double advanced = roundToHalf(weightKg * percent[2]);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = dpToPx(10);
                row.setLayoutParams(rowParams);

                TextView nameText = new TextView(this);
                nameText.setText(exercise.name);
                nameText.setTextColor(Color.WHITE);
                nameText.setTextSize(13f);

                TextView levelsText = new TextView(this);
                levelsText.setText(String.format(Locale.getDefault(),
                        "初心者: %.1fkg　中級者: %.1fkg　上級者: %.1fkg", beginner, intermediate, advanced));
                levelsText.setTextColor(Color.parseColor("#b2bec3"));
                levelsText.setTextSize(12f);

                row.addView(nameText);
                row.addView(levelsText);
                weightGuideContainer.addView(row);
            }
        }
    }

    private double roundToHalf(double value) {
        return Math.round(value * 2) / 2.0;
    }

    private String trimNumber(float value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    private void loadBgmPlaylist() {
        String stored = prefs().getString(KEY_BGM_PLAYLIST, "");
        bgmPlaylist.clear();
        if (!stored.isEmpty()) {
            for (String url : stored.split("\n")) {
                if (!url.trim().isEmpty()) {
                    bgmPlaylist.add(url.trim());
                }
            }
        }
    }

    private void saveBgmPlaylist() {
        prefs().edit().putString(KEY_BGM_PLAYLIST, String.join("\n", bgmPlaylist)).apply();
    }

    private void addBgmUrl() {
        String url = bgmUrlInput.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "URLを入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "有効なURLを入力してください", Toast.LENGTH_SHORT).show();
            return;
        }

        bgmPlaylist.add(url);
        saveBgmPlaylist();
        bgmUrlInput.setText("");
        refreshBgmList();
    }

    private void removeBgmUrl(String url) {
        bgmPlaylist.remove(url);
        saveBgmPlaylist();
        refreshBgmList();
    }

    private void refreshBgmList() {
        bgmListContainer.removeAllViews();

        if (bgmPlaylist.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("プレイリストは空です");
            emptyText.setTextColor(Color.parseColor("#b2bec3"));
            emptyText.setTextSize(13f);
            bgmListContainer.addView(emptyText);
            return;
        }

        for (String url : bgmPlaylist) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dpToPx(8);
            row.setLayoutParams(rowParams);

            TextView urlText = new TextView(this);
            urlText.setText(url);
            urlText.setTextColor(Color.WHITE);
            urlText.setTextSize(12f);
            urlText.setSingleLine(true);
            urlText.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            urlText.setLayoutParams(urlParams);

            Button playBtn = new Button(this);
            playBtn.setText("再生");
            playBtn.setTextSize(11f);
            playBtn.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));

            Button deleteBtn = new Button(this);
            deleteBtn.setText("削除");
            deleteBtn.setTextSize(11f);
            deleteBtn.setOnClickListener(v -> removeBgmUrl(url));

            row.addView(urlText);
            row.addView(playBtn);
            row.addView(deleteBtn);
            bgmListContainer.addView(row);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
