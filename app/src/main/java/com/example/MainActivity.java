package com.example;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * MainActivity for the Stopwatch Application.
 *
 * This class demonstrates core Android fundamentals:
 * 1. UI Control with Standard Views and XML layouts.
 * 2. Accurate time calculation using SystemClock.elapsedRealtime() (prevents timing drift).
 * 3. Non-blocking UI updates using Handler and Runnable.
 * 4. Safe Activity Lifecycle handling (onPause, onResume, onSaveInstanceState).
 * 5. Dynamic view generation for the Lap recording feature.
 */
public class MainActivity extends Activity {

    // =========================================================================
    // UI View References
    // =========================================================================
    private TextView tvTimer;
    private TextView tvTimerStatus;
    private TextView tvLapCountBadge;
    private TextView tvEmptyLaps;
    private LinearLayout lapContainer;
    private ScrollView scrollLaps;

    private Button btnStart;
    private Button btnPause;
    private Button btnReset;
    private Button btnLap;

    // =========================================================================
    // Stopwatch Timing & State Variables
    // =========================================================================

    /**
     * Handler attached to the Main (UI) Looper for scheduling display updates.
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * isRunning: Flag indicating whether the stopwatch is currently active.
     */
    private boolean isRunning = false;

    /**
     * startTime: The timestamp from SystemClock.elapsedRealtime() when the
     * current running session started.
     */
    private long startTime = 0L;

    /**
     * pausedElapsedTime: Stores accumulated elapsed time from previous running
     * intervals before the most recent pause.
     */
    private long pausedElapsedTime = 0L;

    /**
     * lapCount: The total number of recorded laps.
     */
    private int lapCount = 0;

    /**
     * List to maintain recorded lap elapsed times in milliseconds.
     */
    private final ArrayList<Long> lapTimes = new ArrayList<>();

    // Bundle keys for state restoration on configuration change
    private static final String KEY_IS_RUNNING = "key_is_running";
    private static final String KEY_START_TIME = "key_start_time";
    private static final String KEY_PAUSED_TIME = "key_paused_time";
    private static final String KEY_LAP_COUNT = "key_lap_count";
    private static final String KEY_LAP_TIMES = "key_lap_times";

    // =========================================================================
    // Handler Runnable for Periodic UI Refresh
    // =========================================================================

    /**
     * Runnable that periodically recalculates the exact elapsed time
     * using SystemClock.elapsedRealtime() and refreshes the timer TextView.
     *
     * Note: We NEVER do 'seconds++' here. UI callbacks may be delayed by system
     * load. SystemClock.elapsedRealtime() guarantees timestamp accuracy.
     */
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                // Calculate total elapsed time: current session elapsed + previously paused time
                long currentElapsed = (SystemClock.elapsedRealtime() - startTime) + pausedElapsedTime;

                // Update the timer display
                tvTimer.setText(formatTime(currentElapsed));

                // Schedule the next UI update in 50 milliseconds (~20 frames per second)
                handler.postDelayed(this, 50);
            }
        }
    };

    // =========================================================================
    // Activity Lifecycle Methods
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize View references from XML layout
        initializeViews();

        // 2. Setup button click listeners
        setupClickListeners();

        // 3. Restore state if activity was recreated (e.g., screen rotation)
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        } else {
            // Initial button states
            updateButtonStates();
            updateLapBadge();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When returning to the foreground:
        if (isRunning) {
            // Calculate elapsed time and update UI immediately
            long currentElapsed = (SystemClock.elapsedRealtime() - startTime) + pausedElapsedTime;
            tvTimer.setText(formatTime(currentElapsed));

            // Prevent duplicate callbacks by removing any existing one first, then posting
            handler.removeCallbacks(updateTimeRunnable);
            handler.post(updateTimeRunnable);
        } else if (pausedElapsedTime > 0) {
            tvTimer.setText(formatTime(pausedElapsedTime));
        } else {
            tvTimer.setText(getString(R.string.initial_time));
        }
        updateButtonStates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When leaving the foreground, remove UI update callbacks to prevent memory leaks and save battery.
        // The timing reference (startTime and pausedElapsedTime) is preserved!
        handler.removeCallbacks(updateTimeRunnable);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_RUNNING, isRunning);
        outState.putLong(KEY_START_TIME, startTime);
        outState.putLong(KEY_PAUSED_TIME, pausedElapsedTime);
        outState.putInt(KEY_LAP_COUNT, lapCount);

        long[] timesArray = new long[lapTimes.size()];
        for (int i = 0; i < lapTimes.size(); i++) {
            timesArray[i] = lapTimes.get(i);
        }
        outState.putLongArray(KEY_LAP_TIMES, timesArray);
    }

    // =========================================================================
    // View Initialization & Listeners
    // =========================================================================

    private void initializeViews() {
        tvTimer = findViewById(R.id.tvTimer);
        tvTimerStatus = findViewById(R.id.tvTimerStatus);
        tvLapCountBadge = findViewById(R.id.tvLapCountBadge);
        tvEmptyLaps = findViewById(R.id.tvEmptyLaps);
        lapContainer = findViewById(R.id.lapContainer);
        scrollLaps = findViewById(R.id.scrollLaps);

        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnReset = findViewById(R.id.btnReset);
        btnLap = findViewById(R.id.btnLap);
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopwatch();
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseStopwatch();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStopwatch();
            }
        });

        btnLap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordLap();
            }
        });
    }

    // =========================================================================
    // Stopwatch Core Operations
    // =========================================================================

    /**
     * Starts or Resumes the Stopwatch.
     * Guarded against rapid taps using `if (!isRunning)`.
     */
    private void startStopwatch() {
        if (!isRunning) {
            // Mark the starting reference time using elapsedRealtime
            startTime = SystemClock.elapsedRealtime();
            isRunning = true;

            // Remove any pending callbacks before posting to prevent duplicate loops
            handler.removeCallbacks(updateTimeRunnable);
            handler.post(updateTimeRunnable);

            updateButtonStates();
        }
    }

    /**
     * Pauses the Stopwatch.
     * Freezes current elapsed time and preserves it in `pausedElapsedTime`.
     */
    private void pauseStopwatch() {
        if (isRunning) {
            // Accumulate elapsed time up to this exact moment
            pausedElapsedTime += SystemClock.elapsedRealtime() - startTime;
            isRunning = false;

            // Stop UI updates
            handler.removeCallbacks(updateTimeRunnable);

            // Display frozen elapsed time
            tvTimer.setText(formatTime(pausedElapsedTime));

            updateButtonStates();
        }
    }

    /**
     * Resets the Stopwatch to initial state.
     * Clears all elapsed times, stops handler callbacks, and clears laps list.
     */
    private void resetStopwatch() {
        isRunning = false;
        handler.removeCallbacks(updateTimeRunnable);

        startTime = 0L;
        pausedElapsedTime = 0L;
        lapCount = 0;
        lapTimes.clear();

        // Reset timer display
        tvTimer.setText(getString(R.string.initial_time));

        // Clear lap views and show empty placeholder
        lapContainer.removeAllViews();
        if (tvEmptyLaps != null) {
            lapContainer.addView(tvEmptyLaps);
            tvEmptyLaps.setVisibility(View.VISIBLE);
        }

        updateLapBadge();
        updateButtonStates();
    }

    /**
     * Records a new lap with the current elapsed time.
     * Only operational while the stopwatch is running.
     */
    private void recordLap() {
        if (isRunning) {
            long currentElapsed = (SystemClock.elapsedRealtime() - startTime) + pausedElapsedTime;
            lapCount++;
            lapTimes.add(currentElapsed);

            addLapRow(lapCount, currentElapsed);
            updateLapBadge();
        }
    }

    /**
     * Dynamically inflates and adds a lap row to the lap list layout.
     * Newer laps are added at the top (index 0) for immediate visibility.
     */
    private void addLapRow(int lapNumber, long elapsedMillis) {
        // Hide empty state text if present
        if (tvEmptyLaps != null && tvEmptyLaps.getVisibility() == View.VISIBLE) {
            tvEmptyLaps.setVisibility(View.GONE);
            lapContainer.removeView(tvEmptyLaps);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View lapRow = inflater.inflate(R.layout.item_lap, lapContainer, false);

        TextView tvLapNumber = lapRow.findViewById(R.id.tvLapNumber);
        TextView tvLapTime = lapRow.findViewById(R.id.tvLapTime);

        tvLapNumber.setText(String.format(Locale.getDefault(), getString(R.string.lap_format), lapNumber));
        tvLapTime.setText(formatTime(elapsedMillis));

        // Add newest lap at the top of the container
        lapContainer.addView(lapRow, 0);

        // Smoothly scroll to the top of the lap list
        if (scrollLaps != null) {
            scrollLaps.post(new Runnable() {
                @Override
                public void run() {
                    scrollLaps.smoothScrollTo(0, 0);
                }
            });
        }
    }

    // =========================================================================
    // Helper Methods: UI & State Management
    // =========================================================================

    /**
     * Updates button enabled/disabled states and status indicators
     * according to the current stopwatch state.
     */
    private void updateButtonStates() {
        btnStart.setEnabled(!isRunning);
        btnPause.setEnabled(isRunning);
        btnReset.setEnabled(true);
        btnLap.setEnabled(isRunning);

        if (isRunning) {
            tvTimerStatus.setText("RUNNING");
            tvTimerStatus.setTextColor(getResources().getColor(R.color.btn_start_bg, getTheme()));
        } else if (pausedElapsedTime > 0) {
            tvTimerStatus.setText("PAUSED");
            tvTimerStatus.setTextColor(getResources().getColor(R.color.btn_pause_bg, getTheme()));
        } else {
            tvTimerStatus.setText("STOPPED");
            tvTimerStatus.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
        }
    }

    private void updateLapBadge() {
        if (tvLapCountBadge != null) {
            if (lapCount == 1) {
                tvLapCountBadge.setText("1 Lap");
            } else {
                tvLapCountBadge.setText(String.format(Locale.getDefault(), "%d Laps", lapCount));
            }
        }
    }

    /**
     * Converts elapsed milliseconds into a standard HH:MM:SS format string.
     *
     * Example conversions:
     * 0 ms      -> 00:00:00
     * 1,000 ms  -> 00:00:01
     * 65,000 ms -> 00:01:05
     * 3,665,000 -> 01:01:05
     *
     * @param elapsedMillis The elapsed time in milliseconds.
     * @return Formatted time string "HH:MM:SS".
     */
    public static String formatTime(long elapsedMillis) {
        if (elapsedMillis < 0) {
            elapsedMillis = 0;
        }
        long hours = elapsedMillis / 3600000;
        long minutes = (elapsedMillis % 3600000) / 60000;
        long seconds = (elapsedMillis % 60000) / 1000;

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Restores internal state and UI after activity recreation.
     */
    private void restoreSavedState(Bundle savedInstanceState) {
        isRunning = savedInstanceState.getBoolean(KEY_IS_RUNNING, false);
        startTime = savedInstanceState.getLong(KEY_START_TIME, 0L);
        pausedElapsedTime = savedInstanceState.getLong(KEY_PAUSED_TIME, 0L);
        lapCount = savedInstanceState.getInt(KEY_LAP_COUNT, 0);

        long[] times = savedInstanceState.getLongArray(KEY_LAP_TIMES);
        if (times != null) {
            for (int i = 0; i < times.length; i++) {
                lapTimes.add(times[i]);
                addLapRow(i + 1, times[i]);
            }
        }

        if (isRunning) {
            long currentElapsed = (SystemClock.elapsedRealtime() - startTime) + pausedElapsedTime;
            tvTimer.setText(formatTime(currentElapsed));
            handler.post(updateTimeRunnable);
        } else if (pausedElapsedTime > 0) {
            tvTimer.setText(formatTime(pausedElapsedTime));
        } else {
            tvTimer.setText(getString(R.string.initial_time));
        }

        updateLapBadge();
        updateButtonStates();
    }
}
