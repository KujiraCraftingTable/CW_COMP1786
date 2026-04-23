package com.example.expensetrackerapp;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Project;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UploadActivity extends AppCompatActivity {

    private TextView tvNetworkIndicator, tvNetworkStatus;
    private TextView tvSyncTotal, tvSyncSynced, tvSyncPending;
    private TextInputEditText etServerUrl;
    private LinearProgressIndicator progressBar;
    private TextView tvUploadLog;

    private DatabaseHelper db;
    private UploadManager uploadManager;
    private SharedPreferences prefs;
    private boolean uploadInProgress = false;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        setTitle("Upload to Cloud");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db            = DatabaseHelper.getInstance(this);
        uploadManager = new UploadManager();
        prefs         = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE);

        bindViews();
        loadSavedUrl();
        refreshNetworkStatus();
        refreshSyncStats();

        // Test connection
        findViewById(R.id.btnTestConnection).setOnClickListener(v -> testConnection());

        // Save URL to SharedPreferences
        findViewById(R.id.btnSaveUrl).setOnClickListener(v -> saveUrl());

        // Upload all
        findViewById(R.id.btnUploadAll).setOnClickListener(v -> startUpload());

        // Clear log
        findViewById(R.id.btnClearLog).setOnClickListener(v -> {
            tvUploadLog.setText("— Log cleared —");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNetworkStatus();
        refreshSyncStats();

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshSyncStats();
                refreshHandler.postDelayed(this, 3_000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 3_000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    // ── Bind views ────────────────────────────────────────────────────────────

    private void bindViews() {
        tvNetworkIndicator = findViewById(R.id.tvNetworkIndicator);
        tvNetworkStatus    = findViewById(R.id.tvNetworkStatus);
        tvSyncTotal        = findViewById(R.id.tvSyncTotal);
        tvSyncSynced       = findViewById(R.id.tvSyncSynced);
        tvSyncPending      = findViewById(R.id.tvSyncPending);
        etServerUrl        = findViewById(R.id.etServerUrl);
        progressBar        = findViewById(R.id.progressBar);
        tvUploadLog        = findViewById(R.id.tvUploadLog);
    }

    // ── URL helpers ───────────────────────────────────────────────────────────

    private void loadSavedUrl() {
        String saved = prefs.getString(AppConstants.PREF_SERVER_URL,
                AppConstants.DEFAULT_SERVER_URL);
        etServerUrl.setText(saved);
    }

    private void saveUrl() {
        String url = Objects.requireNonNull(etServerUrl.getText()).toString().trim();
        if (url.isEmpty()) {
            etServerUrl.setError("Please enter a server URL");
            return;
        }
        prefs.edit().putString(AppConstants.PREF_SERVER_URL, url).apply();
        Toast.makeText(this, "Server URL saved", Toast.LENGTH_SHORT).show();
        appendLog("Server URL saved: " + url);
    }

    private String getSavedUrl() {
        return prefs.getString(AppConstants.PREF_SERVER_URL,
                AppConstants.DEFAULT_SERVER_URL);
    }

    // ── Network status ────────────────────────────────────────────────────────

    private void refreshNetworkStatus() {
        boolean online = NetworkUtils.isNetworkAvailable(this);
        String type    = NetworkUtils.getConnectionType(this);

        if (online) {
            tvNetworkStatus.setText("Connected · " + type);
            tvNetworkIndicator.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.statusActive)));
        } else {
            tvNetworkStatus.setText("No network connection");
            tvNetworkIndicator.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.errorColor)));
        }
    }

    // ── Sync stats ────────────────────────────────────────────────────────────

    private void refreshSyncStats() {
        List<Project> all = db.getAllProjects();
        int total   = all.size();
        int pending = db.getUnsyncedProjectCount();
        int synced  = total - pending;

        tvSyncTotal.setText(String.valueOf(total));
        tvSyncSynced.setText(String.valueOf(synced));
        tvSyncPending.setText(String.valueOf(pending));
    }

    // ── Test connection ───────────────────────────────────────────────────────

    private void testConnection() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No network connection", Toast.LENGTH_SHORT).show();
            return;
        }
        setUiEnabled(false);
        appendLog("Testing connection to: " + getSavedUrl());

        uploadManager.testConnection(getSavedUrl(), new UploadManager.UploadCallback() {
            @Override public void onProgress(String msg) { appendLog(msg); }

            @Override public void onSuccess(int p, int e, String response) {
                appendLog("✓ " + response);
                Toast.makeText(UploadActivity.this,
                        "Connection successful", Toast.LENGTH_SHORT).show();
                setUiEnabled(true);
            }

            @Override public void onError(String error) {
                appendLog("✗ " + error);
                Toast.makeText(UploadActivity.this,
                        "Connection failed: " + error, Toast.LENGTH_LONG).show();
                setUiEnabled(true);
            }
        });
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    private void startUpload() {
        if (uploadInProgress) return;

        // 1. Check network
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this,
                    "No network connection. Please connect to the internet first.",
                    Toast.LENGTH_LONG).show();
            appendLog("✗ Upload aborted — no network connection");
            return;
        }

        // 2. Check there is data to upload
        List<Project> projects = db.getAllProjects();
        if (projects.isEmpty()) {
            Toast.makeText(this, "No projects to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Check URL is configured
        String url = getSavedUrl();
        if (url.trim().isEmpty()) {
            Toast.makeText(this,
                    "Please configure a valid server URL first",
                    Toast.LENGTH_LONG).show();
            return;
        }

        setUiEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        appendLog("─────────────────────────────────");
        appendLog("Upload started  " + timestamp());
        appendLog("Network: " + NetworkUtils.getConnectionType(this));
        appendLog("Server:  " + url);
        appendLog("Projects to upload: " + projects.size());

        uploadManager.uploadAll(this, url, new UploadManager.UploadCallback() {
            @Override public void onProgress(String msg) {
                appendLog(msg);
            }

            @Override public void onSuccess(int projectCount, int expenseCount,
                                            String serverResponse) {
                progressBar.setVisibility(View.GONE);
                appendLog("✓ Upload successful  " + timestamp());
                appendLog("  Projects: " + projectCount
                        + "  |  Expenses: " + expenseCount);
                appendLog("  Server: " + serverResponse);
                appendLog("─────────────────────────────────");

                refreshSyncStats();
                setUiEnabled(true);
                Toast.makeText(UploadActivity.this,
                        projectCount + " project(s) uploaded successfully",
                        Toast.LENGTH_LONG).show();
            }

            @Override public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                appendLog("✗ Upload failed  " + timestamp());
                appendLog("  Error: " + error);
                appendLog("─────────────────────────────────");

                setUiEnabled(true);
                Toast.makeText(UploadActivity.this,
                        "Upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void setUiEnabled(boolean enabled) {
        uploadInProgress = !enabled;
        findViewById(R.id.btnUploadAll).setEnabled(enabled);
        findViewById(R.id.btnTestConnection).setEnabled(enabled);
        findViewById(R.id.btnSaveUrl).setEnabled(enabled);
    }

    private void appendLog(String line) {
        String current = tvUploadLog.getText().toString();
        if (current.equals("— No uploads yet —") || current.equals("— Log cleared —")) {
            tvUploadLog.setText(line);
        } else {
            tvUploadLog.setText(current + "\n" + line);
        }
    }

    private String timestamp() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
