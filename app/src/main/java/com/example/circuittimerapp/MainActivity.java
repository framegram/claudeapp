package com.example.circuittimerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView setLabel, exerciseName, timerDisplay, instructionText;
    private ProgressBar progressBar;
    private Button startBtn, pauseBtn, resetBtn, saveWeightBtn, clearDataBtn, settingsBtn, referenceUrlBtn;
    private EditText weightInput;
    private LinearLayout weightSection, exercisePreview;
    private ImageView exerciseImage;
    private Exercise displayedExercise;

    private int currentSetIndex = 0;
    private int currentExerciseIndex = 0;
    private boolean isResting = false;
    private boolean isWorkoutComplete = false;
    private int timeLeft = 0;
    private int totalTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private ToneGenerator toneGenerator;
    private static final int ALARM_COUNTDOWN_SECONDS = 5;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            if (timeLeft > 0) {
                timeLeft--;
                updateDisplay();
                if (timeLeft <= ALARM_COUNTDOWN_SECONDS) {
                    playCountdownBeep(timeLeft == 0);
                }
            }

            if (timeLeft > 0) {
                handler.postDelayed(this, 1000);
            } else {
                advanceToNextPhase();
            }
        }
    };

    // ワークアウトプラン(全種目ダンベル)
    private static final List<WorkoutSet> workoutPlan = Arrays.asList(
            new WorkoutSet(1, Arrays.asList(
                    new Exercise("ダンベルベンチプレス", 150, "胸", false),
                    new Exercise("ダンベルキックバック", 150, "三頭筋", false)
            ), 60),
            new WorkoutSet(2, Arrays.asList(
                    new Exercise("ダンベルデッドリフト", 150, "背筋・足", false),
                    new Exercise("ダンベルカール", 150, "二頭筋", false)
            ), 60),
            new WorkoutSet(3, Arrays.asList(
                    new Exercise("ダンベルショルダープレス", 150, "肩・三頭", false),
                    new Exercise("ダンベルランジ", 150, "足", false)
            ), 60),
            new WorkoutSet(4, Arrays.asList(
                    new Exercise("ダンベルロウ", 150, "背筋", false),
                    new Exercise("ダンベルトライセプスエクステンション", 150, "三頭筋", false)
            ), 60),
            new WorkoutSet(5, Arrays.asList(
                    new Exercise("ダンベルスクワットプレス", 300, "全身フィニッシャー", false)
            ), 0)
    );

    // ローカルストレージ用
    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String WEIGHT_KEY_PREFIX = "weight_set_";

    static List<WorkoutSet> getWorkoutPlan() {
        return workoutPlan;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadWeightsFromStorage();
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        } catch (RuntimeException e) {
            // 一部端末ではアラームストリームの初期化に失敗することがあるため、
            // その場合はビープ音なしで動作を継続する
            toneGenerator = null;
        }

        startBtn.setOnClickListener(v -> startTimer());
        pauseBtn.setOnClickListener(v -> pauseTimer());
        resetBtn.setOnClickListener(v -> resetTimer());
        saveWeightBtn.setOnClickListener(v -> saveWeight());
        clearDataBtn.setOnClickListener(v -> clearData());
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        referenceUrlBtn.setOnClickListener(v -> openReferenceVideo());

        updateDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(tickRunnable);
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    private void playCountdownBeep(boolean isPhaseEnd) {
        if (toneGenerator == null) return;
        toneGenerator.startTone(
                isPhaseEnd ? ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD : ToneGenerator.TONE_PROP_BEEP,
                isPhaseEnd ? 400 : 150);
    }

    private void initViews() {
        setLabel = findViewById(R.id.setLabel);
        exerciseName = findViewById(R.id.exerciseName);
        timerDisplay = findViewById(R.id.timerDisplay);
        progressBar = findViewById(R.id.progressBar);
        instructionText = findViewById(R.id.instructionText);

        startBtn = findViewById(R.id.startBtn);
        pauseBtn = findViewById(R.id.pauseBtn);
        resetBtn = findViewById(R.id.resetBtn);
        saveWeightBtn = findViewById(R.id.saveWeightBtn);
        clearDataBtn = findViewById(R.id.clearDataBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        referenceUrlBtn = findViewById(R.id.referenceUrlBtn);
        weightInput = findViewById(R.id.weightInput);
        weightSection = findViewById(R.id.weightSection);
        exercisePreview = findViewById(R.id.exercisePreview);
        exerciseImage = findViewById(R.id.exerciseImage);
    }

    private void openReferenceVideo() {
        if (displayedExercise == null) return;
        String query = Uri.encode(displayedExercise.name + " やり方");
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + query)));
    }

    private void startTimer() {
        if (isRunning) return;

        if (isWorkoutComplete) {
            resetTimer();
            return;
        }

        isRunning = true;
        startBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);

        if (totalTime == 0) {
            // 新しいフェーズの開始(一時停止からの再開時はtimeLeft/totalTimeを維持して続きから再生)
            WorkoutSet currentSet = workoutPlan.get(currentSetIndex);
            Exercise currentExercise = currentSet.exercises.get(currentExerciseIndex);
            totalTime = currentExercise.duration;
            timeLeft = totalTime;

            // 重量調整可能な種目なので常に重量入力欄を表示
            weightSection.setVisibility(View.VISIBLE);
        }

        updateDisplay();
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
    }

    private void pauseTimer() {
        isRunning = false;
        handler.removeCallbacks(tickRunnable);
        pauseBtn.setVisibility(View.GONE);
        startBtn.setVisibility(View.VISIBLE);
    }

    private void resetTimer() {
        isRunning = false;
        handler.removeCallbacks(tickRunnable);

        currentSetIndex = 0;
        currentExerciseIndex = 0;
        isResting = false;
        isWorkoutComplete = false;
        timeLeft = 0;
        totalTime = 0;

        startBtn.setVisibility(View.VISIBLE);
        startBtn.setEnabled(true);
        pauseBtn.setVisibility(View.GONE);
        weightSection.setVisibility(View.GONE);

        updateDisplay();
    }

    private void advanceToNextPhase() {
        if (!isResting) {
            // エクササイズ終了
            currentExerciseIndex++;
            if (currentExerciseIndex >= workoutPlan.get(currentSetIndex).exercises.size()) {
                // セット終了、休憩へ
                currentExerciseIndex = 0;
                currentSetIndex++;
                isResting = true;
                totalTime = currentSetIndex < workoutPlan.size() ?
                        workoutPlan.get(currentSetIndex - 1).restAfter : 0;
            } else {
                totalTime = workoutPlan.get(currentSetIndex).exercises.get(currentExerciseIndex).duration;
            }
        } else {
            // 休憩終了
            isResting = false;
            if (currentSetIndex >= workoutPlan.size()) {
                finishWorkout();
                return;
            }
            totalTime = workoutPlan.get(currentSetIndex).exercises.get(currentExerciseIndex).duration;
        }

        timeLeft = totalTime;
        updateDisplay();

        boolean hasActiveExercise = !isResting && currentSetIndex < workoutPlan.size();
        weightSection.setVisibility(hasActiveExercise ? View.VISIBLE : View.GONE);

        handler.post(tickRunnable);
    }

    private void finishWorkout() {
        isRunning = false;
        isWorkoutComplete = true;
        timeLeft = 0;
        totalTime = 0;
        displayedExercise = null;
        weightSection.setVisibility(View.GONE);
        exercisePreview.setVisibility(View.GONE);
        setLabel.setText("完了");
        exerciseName.setText("トレーニング終了！");
        timerDisplay.setText("00:00");
        progressBar.setProgress(0);
        startBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.GONE);
    }

    private void updateDisplay() {
        timerDisplay.setText(formatTime(timeLeft));
        progressBar.setMax(totalTime > 0 ? totalTime : 1);
        if (totalTime > 0) {
            int progress = (int) ((double) (totalTime - timeLeft) / totalTime * 100);
            progressBar.setProgress(progress);
        } else {
            progressBar.setProgress(0);
        }

        if (!isResting) {
            WorkoutSet currentSet = workoutPlan.get(currentSetIndex);
            Exercise currentExercise = currentSet.exercises.get(currentExerciseIndex);
            displayedExercise = currentExercise;
            setLabel.setText(String.format(Locale.getDefault(), "SET %d / %d", currentSetIndex + 1, workoutPlan.size()));
            String text = currentExercise.name;
            if (currentExercise.weight != null) {
                text += "\n重量: " + formatWeight(currentExercise.weight) + "kg";
            }
            exerciseName.setText(text);
            exerciseImage.setImageResource(currentExercise.isBarbell ? R.drawable.ic_barbell : R.drawable.ic_dumbbell);
            exercisePreview.setVisibility(View.VISIBLE);
        } else {
            displayedExercise = null;
            exercisePreview.setVisibility(View.GONE);
            setLabel.setText("休憩中");
            exerciseName.setText("休憩中\n次のセットへ");
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int sec = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, sec);
    }

    private String formatWeight(float weight) {
        if (weight == Math.floor(weight)) {
            return String.valueOf((int) weight);
        }
        return String.valueOf(weight);
    }

    private void saveWeight() {
        try {
            String weightStr = weightInput.getText().toString();
            if (weightStr.isEmpty()) {
                Toast.makeText(this, "重量を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            float weight = Float.parseFloat(weightStr);

            WorkoutSet currentSet = workoutPlan.get(currentSetIndex);
            Exercise currentExercise = currentSet.exercises.get(currentExerciseIndex);
            currentExercise.weight = weight;

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String key = WEIGHT_KEY_PREFIX + currentSetIndex + "_" + currentExerciseIndex;
            editor.putFloat(key, weight);
            editor.apply();

            updateDisplay();
            Toast.makeText(this, "重量を保存しました", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "有効な数字を入力してください", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWeightsFromStorage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        for (int i = 0; i < workoutPlan.size(); i++) {
            WorkoutSet set = workoutPlan.get(i);
            for (int j = 0; j < set.exercises.size(); j++) {
                Exercise ex = set.exercises.get(j);
                String key = WEIGHT_KEY_PREFIX + i + "_" + j;
                float weight = prefs.getFloat(key, -1f);
                if (weight != -1f) {
                    ex.weight = weight;
                }
            }
        }
    }

    private void clearData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        for (WorkoutSet set : workoutPlan) {
            for (Exercise ex : set.exercises) {
                ex.weight = null;
            }
        }

        Toast.makeText(this, "データを初期化しました", Toast.LENGTH_SHORT).show();
        resetTimer();
    }

    // データクラス
    static class WorkoutSet {
        int set;
        List<Exercise> exercises;
        int restAfter;

        WorkoutSet(int set, List<Exercise> exercises, int restAfter) {
            this.set = set;
            this.exercises = exercises;
            this.restAfter = restAfter;
        }
    }

    static class Exercise {
        String name;
        int duration;
        String muscle;
        boolean isBarbell;
        Float weight; // nullで初期化

        Exercise(String name, int duration, String muscle, boolean isBarbell) {
            this.name = name;
            this.duration = duration;
            this.muscle = muscle;
            this.isBarbell = isBarbell;
            this.weight = null;
        }
    }
}
