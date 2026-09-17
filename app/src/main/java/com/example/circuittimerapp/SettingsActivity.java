package com.example.circuittimerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String KEY_WEIGHT_KG = "user_weight_kg";
    private static final String KEY_HEIGHT_CM = "user_height_cm";
    private static final String KEY_BGM_PLAYLIST = "bgm_playlist";

    private EditText weightKgInput, heightCmInput, bgmUrlInput;
    private TextView bmiText, caloriesText, proteinText, fatText, carbsText;
    private LinearLayout bgmListContainer;

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
