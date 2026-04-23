package com.example.expensetrackerapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Expense;
import com.example.expensetrackerapp.model.Project;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles background upload of all project and expense data to a REST endpoint.
 *
 * <p>POST  {serverUrl}/upload
 * <pre>
 * {
 *   "uploadedAt": "2024-01-01 10:00:00",
 *   "totalProjects": 3,
 *   "projects": [
 *     {
 *       "localId": 1,
 *       "projectCode": "PRJ-001",
 *       ...all project fields...,
 *       "expenses": [
 *         { "localId": 1, "expenseCode": "EXP-001", ... }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
public class UploadManager {

    public interface UploadCallback {
        /** Called on the main thread to report progress messages. */
        void onProgress(String message);
        /** Called on the main thread when the upload completes successfully. */
        void onSuccess(int projectCount, int expenseCount, String serverResponse);
        /** Called on the main thread when the upload fails. */
        void onError(String errorMessage);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler  = new Handler(Looper.getMainLooper());

    /**
     * Upload every project (and its expenses) to the configured server endpoint.
     * Network I/O runs on a background thread; callbacks are delivered on the main thread.
     */
    public void uploadAll(Context context, String serverUrl, UploadCallback callback) {
        executor.execute(() -> {
            try {
                DatabaseHelper db = DatabaseHelper.getInstance(context);

                post(callback, "Building payload…");
                JSONObject payload = buildPayload(context, db);
                int projectCount = payload.getJSONArray("projects").length();
                int expenseCount = payload.getInt("totalExpenses");

                post(callback, "Connecting to server…");
                String fullUrl = serverUrl.trim().replaceAll("/+$", "") + AppConstants.UPLOAD_PATH;
                String response = httpPost(fullUrl, payload.toString());

                // Mark all projects as synced on success
                db.markAllProjectsSynced();

                mainHandler.post(() -> callback.onSuccess(projectCount, expenseCount, response));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null
                        ? e.getMessage() : e.getClass().getSimpleName()));
            }
        });
    }

    /**
     * Sends a lightweight GET to the server root to verify reachability.
     */
    public void testConnection(String serverUrl, UploadCallback callback) {
        executor.execute(() -> {
            try {
                post(callback, "Testing connection…");
                String fullUrl = serverUrl.trim().replaceAll("/+$", "");
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(AppConstants.CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(AppConstants.READ_TIMEOUT_MS);
                conn.connect();

                int code = conn.getResponseCode();
                conn.disconnect();

                if (code > 0) {
                    mainHandler.post(() ->
                            callback.onSuccess(0, 0,
                                    "Server reachable — HTTP " + code));
                } else {
                    mainHandler.post(() -> callback.onError("No HTTP response received"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        "Cannot reach server: " + (e.getMessage() != null
                                ? e.getMessage() : e.getClass().getSimpleName())));
            }
        });
    }

    // ── JSON builder ──────────────────────────────────────────────────────────

    private JSONObject buildPayload(Context context, DatabaseHelper db) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("uploadedAt", new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        payload.put("appVersion", "1.0");

        List<Project> projects = db.getAllProjects();
        JSONArray projectsArray = new JSONArray();
        int totalExpenses = 0;

        for (Project p : projects) {
            JSONObject pJson = new JSONObject();
            pJson.put("localId",             p.getId());
            pJson.put("projectCode",         p.getProjectCode());
            pJson.put("projectName",         p.getProjectName());
            pJson.put("description",         nullSafe(p.getDescription()));
            pJson.put("startDate",           p.getStartDate());
            pJson.put("endDate",             p.getEndDate());
            pJson.put("manager",             p.getManager());
            pJson.put("status",              p.getStatus());
            pJson.put("budget",              p.getBudget());
            pJson.put("priority",            p.getPriority());
            pJson.put("specialRequirements", nullSafe(p.getSpecialRequirements()));
            pJson.put("clientDepartment",    nullSafe(p.getClientDepartment()));
            pJson.put("notes",               nullSafe(p.getNotes()));
            pJson.put("createdAt",           nullSafe(p.getCreatedAt()));

            List<Expense> expenses = db.getExpensesByProject(p.getId());
            JSONArray expArray = new JSONArray();
            for (Expense e : expenses) {
                JSONObject eJson = new JSONObject();
                eJson.put("localId",       e.getId());
                eJson.put("expenseCode",   e.getExpenseCode());
                eJson.put("expenseDate",   e.getExpenseDate());
                eJson.put("amount",        e.getAmount());
                eJson.put("currency",      e.getCurrency());
                eJson.put("expenseType",   e.getExpenseType());
                eJson.put("paymentMethod", e.getPaymentMethod());
                eJson.put("claimant",      e.getClaimant());
                eJson.put("paymentStatus", e.getPaymentStatus());
                eJson.put("description",   nullSafe(e.getDescription()));
                eJson.put("location",      nullSafe(e.getLocation()));
                eJson.put("receiptNumber", nullSafe(e.getReceiptNumber()));
                eJson.put("createdAt",     nullSafe(e.getCreatedAt()));
                expArray.put(eJson);
            }
            pJson.put("expenses",     expArray);
            pJson.put("expenseCount", expenses.size());
            totalExpenses += expenses.size();
            projectsArray.put(pJson);
        }

        payload.put("totalProjects", projects.size());
        payload.put("totalExpenses", totalExpenses);
        payload.put("projects",      projectsArray);
        return payload;
    }

    // ── HTTP POST ─────────────────────────────────────────────────────────────

    private String httpPost(String urlString, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(AppConstants.CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(AppConstants.READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            byte[] data = jsonBody.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(data.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
                os.flush();
            }

            int code = conn.getResponseCode();
            // Read body (success or error stream)
            BufferedReader reader;
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), "UTF-8"));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            if (code >= 200 && code < 300) {
                return "HTTP " + code + " — " + sb;
            } else {
                throw new Exception("Server returned HTTP " + code + ": " + sb);
            }
        } finally {
            conn.disconnect();
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void post(UploadCallback cb, String msg) {
        mainHandler.post(() -> cb.onProgress(msg));
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
