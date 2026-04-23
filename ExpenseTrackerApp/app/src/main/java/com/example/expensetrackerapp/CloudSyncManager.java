package com.example.expensetrackerapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Expense;
import com.example.expensetrackerapp.model.Project;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
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

public class CloudSyncManager {
    public interface SyncCallback {
        void onResult(boolean success, String message);
    }
    private static CloudSyncManager instance;
    private CloudSyncManager() {}
    public static synchronized CloudSyncManager getInstance() {
        if (instance == null) instance = new CloudSyncManager();
        return instance;
    }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public void syncProject(Context context, Project project, @Nullable SyncCallback callback) {
        executor.execute(() -> {
            try {
                DatabaseHelper db = DatabaseHelper.getInstance(context);
                List<Expense> expenses = db.getExpensesByProject(project.getId());
                String url = serverUrl(context) + "/upload";
                JSONObject payload = buildUploadPayload(project, expenses);
                httpPost(url, payload.toString());
                db.markProjectSynced(project.getId());
                deliver(callback, true, "Synced!");
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                deliver(callback, false, msg);
            }
        });
    }
    public void syncExpense(Context context, String projectCode, Expense expense, @Nullable SyncCallback callback) {
        executor.execute(() -> {
            try {
                String url = serverUrl(context) + "/expenses";
                JSONObject payload = new JSONObject();
                payload.put("projectCode", projectCode);
                payload.put("expense", expenseToJson(expense));
                httpPost(url, payload.toString());
                deliver(callback, true, "Expense synced!");
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                deliver(callback, false, msg);
            }
        });
    }
    private String serverUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        String base = prefs.getString(AppConstants.PREF_SERVER_URL, AppConstants.DEFAULT_SERVER_URL);
        return base.trim().replaceAll("/+$", "");
    }
    private JSONObject buildUploadPayload(Project project, List<Expense> expenses) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("uploadedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        payload.put("appVersion", "1.0");
        payload.put("totalProjects", 1);
        payload.put("totalExpenses", expenses.size());
        JSONObject pJson = projectToJson(project);
        JSONArray expArr = new JSONArray();
        for (Expense e : expenses) expArr.put(expenseToJson(e));
        pJson.put("expenses", expArr);
        pJson.put("expenseCount", expenses.size());
        JSONArray projectsArr = new JSONArray();
        projectsArr.put(pJson);
        payload.put("projects", projectsArr);
        return payload;
    }
    private JSONObject projectToJson(Project p) throws Exception {
        JSONObject j = new JSONObject();
        j.put("localId", p.getId());
        j.put("projectCode", p.getProjectCode());
        j.put("projectName", p.getProjectName());
        j.put("description", safe(p.getDescription()));
        j.put("startDate", p.getStartDate());
        j.put("endDate", p.getEndDate());
        j.put("manager", p.getManager());
        j.put("status", p.getStatus());
        j.put("budget", p.getBudget());
        j.put("priority", p.getPriority());
        j.put("specialRequirements", safe(p.getSpecialRequirements()));
        j.put("clientDepartment", safe(p.getClientDepartment()));
        j.put("notes", safe(p.getNotes()));
        j.put("createdAt", safe(p.getCreatedAt()));
        return j;
    }
    private JSONObject expenseToJson(Expense e) throws Exception {
        JSONObject j = new JSONObject();
        j.put("localId", e.getId());
        j.put("expenseCode", e.getExpenseCode());
        j.put("expenseDate", e.getExpenseDate());
        j.put("amount", e.getAmount());
        j.put("currency", e.getCurrency());
        j.put("expenseType", e.getExpenseType());
        j.put("paymentMethod", e.getPaymentMethod());
        j.put("claimant", e.getClaimant());
        j.put("paymentStatus", e.getPaymentStatus());
        j.put("description", safe(e.getDescription()));
        j.put("location", safe(e.getLocation()));
        j.put("receiptNumber", safe(e.getReceiptNumber()));
        j.put("createdAt", safe(e.getCreatedAt()));
        return j;
    }
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
            try (OutputStream os = conn.getOutputStream()) { os.write(data); os.flush(); }
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            if (code >= 200 && code < 300) return sb.toString();
            throw new Exception("Server error HTTP " + code + ": " + sb);
        } finally { conn.disconnect(); }
    }
    private void deliver(@Nullable SyncCallback cb, boolean success, String msg) {
        if (cb != null) mainHandler.post(() -> cb.onResult(success, msg));
    }
    private String safe(String s) { return s != null ? s : ""; }

    public void deleteExpense(Context context, String projectCode, String expenseCode,
                              @Nullable SyncCallback callback) {
        executor.execute(() -> {
            try {
                String url = serverUrl(context) + "/expenses/" + projectCode + "/" + expenseCode;
                httpDelete(url);
                deliver(callback, true, "Deleted from server!");
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                deliver(callback, false, msg);
            }
        });
    }

    public void deleteProject(Context context, String projectCode,
                              @Nullable SyncCallback callback) {
        executor.execute(() -> {
            try {
                String url = serverUrl(context) + "/projects/" + projectCode;
                httpDelete(url);
                deliver(callback, true, "Project deleted from server!");
            } catch (Exception e) {
                deliver(callback, false, e.getMessage());
            }
        });
    }

    private void httpDelete(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(AppConstants.CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(AppConstants.READ_TIMEOUT_MS);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new Exception("Server error HTTP " + code);
            }
        } finally { conn.disconnect(); }
    }
}
