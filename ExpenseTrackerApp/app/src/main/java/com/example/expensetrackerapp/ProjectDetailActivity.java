package com.example.expensetrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetrackerapp.adapter.ExpenseAdapter;
import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Expense;
import com.example.expensetrackerapp.model.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectDetailActivity extends AppCompatActivity
        implements ExpenseAdapter.OnExpenseClickListener {

    public static final String EXTRA_PROJECT_ID = "extra_project_id";

    private int projectId;
    private Project project;
    private DatabaseHelper db;

    private TextView tvDetailProjectName, tvDetailProjectCode, tvDetailStatus;
    private TextView tvBudgetAmount, tvTotalSpent, tvRemaining, tvExpenseCount, tvNoExpenses;
    private RecyclerView rvExpenses;
    private ExpenseAdapter expenseAdapter;
    private final List<Expense> expenseList = new ArrayList<>();
    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = DatabaseHelper.getInstance(this);
        projectId = getIntent().getIntExtra(EXTRA_PROJECT_ID, -1);
        if (projectId == -1) { finish(); return; }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindViews();
        setupExpenseList();

        findViewById(R.id.fabAddExpense).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditExpenseActivity.class);
            intent.putExtra(AddEditExpenseActivity.EXTRA_PROJECT_ID, projectId);
            startActivity(intent);
        });
    }

    private void bindViews() {
        tvDetailProjectName  = findViewById(R.id.tvDetailProjectName);
        tvDetailProjectCode  = findViewById(R.id.tvDetailProjectCode);
        tvDetailStatus       = findViewById(R.id.tvDetailStatus);
        tvBudgetAmount       = findViewById(R.id.tvBudgetAmount);
        tvTotalSpent         = findViewById(R.id.tvTotalSpent);
        tvRemaining          = findViewById(R.id.tvRemaining);
        tvExpenseCount       = findViewById(R.id.tvExpenseCount);
        tvNoExpenses         = findViewById(R.id.tvNoExpenses);
        rvExpenses           = findViewById(R.id.rvExpenses);
    }

    private void setupExpenseList() {
        expenseAdapter = new ExpenseAdapter(this, expenseList, this);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);
        rvExpenses.setNestedScrollingEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        project = db.getProjectById(projectId);
        if (project == null) { finish(); return; }
        setTitle(project.getProjectName());
        populateProjectHeader();
        populateInfoRows();
        loadExpenses();
        pullFromServer();
    }

    private void populateProjectHeader() {
        tvDetailProjectName.setText(project.getProjectName());
        tvDetailProjectCode.setText(project.getProjectCode());
        tvDetailStatus.setText(project.getStatus());
    }

    private void populateInfoRows() {
        setRow(R.id.rowDetailDesc, "Description", project.getDescription());
        setRow(R.id.rowDetailManager, "Project Manager", project.getManager());
        setRow(R.id.rowDetailDates, "Date Range",
                project.getStartDate() + " → " + project.getEndDate());
        setRow(R.id.rowDetailPriority, "Priority", project.getPriority());
        setRow(R.id.rowDetailClient, "Client / Department",
                nullSafe(project.getClientDepartment()));
        setRow(R.id.rowDetailSpecialReqs, "Special Requirements",
                nullSafe(project.getSpecialRequirements()));
        setRow(R.id.rowDetailNotes, "Notes", nullSafe(project.getNotes()));
    }

    private void pullFromServer() {
        android.content.Context appCtx = getApplicationContext();
        String projectCode = project.getProjectCode();
        executor.execute(() -> {
            try {
                SharedPreferences prefs = appCtx.getSharedPreferences(
                        AppConstants.PREF_NAME, android.content.Context.MODE_PRIVATE);
                String base = prefs.getString(
                                AppConstants.PREF_SERVER_URL, AppConstants.DEFAULT_SERVER_URL)
                        .trim().replaceAll("/+$", "");

                URL url = new URL(base + "/projects");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(AppConstants.CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(AppConstants.READ_TIMEOUT_MS);

                int httpCode = conn.getResponseCode();
                if (httpCode != 200) { conn.disconnect(); return; }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject response  = new JSONObject(sb.toString());
                JSONArray  projects  = response.getJSONArray("projects");

                JSONObject serverProject = null;
                for (int i = 0; i < projects.length(); i++) {
                    JSONObject p = projects.getJSONObject(i);
                    if (projectCode.equals(p.optString("projectCode"))) {
                        serverProject = p;
                        break;
                    }
                }
                if (serverProject == null) return;

                JSONArray serverExpenses = serverProject.optJSONArray("expenses");

                // Build set of expense codes that exist on the server
                java.util.Set<String> serverCodes = new java.util.HashSet<>();
                if (serverExpenses != null) {
                    for (int i = 0; i < serverExpenses.length(); i++) {
                        String code = serverExpenses.getJSONObject(i).optString("expenseCode", "");
                        if (!code.isEmpty()) serverCodes.add(code);
                    }
                }

                // 1. Add expenses from server that are missing locally
                int added = 0;
                if (serverExpenses != null) {
                    for (int i = 0; i < serverExpenses.length(); i++) {
                        JSONObject ej      = serverExpenses.getJSONObject(i);
                        String     expCode = ej.optString("expenseCode", "");
                        if (expCode.isEmpty()) continue;
                        if (db.getExpenseByCode(projectId, expCode) != null) continue;

                        Expense e = new Expense();
                        e.setProjectId(projectId);
                        e.setExpenseCode(expCode);
                        e.setExpenseDate(ej.optString("expenseDate", ""));
                        e.setAmount(ej.optDouble("amount", 0));
                        e.setCurrency(ej.optString("currency", ""));
                        e.setExpenseType(ej.optString("expenseType", ""));
                        e.setPaymentMethod(ej.optString("paymentMethod", ""));
                        e.setClaimant(ej.optString("claimant", ""));
                        e.setPaymentStatus(ej.optString("paymentStatus", ""));
                        e.setDescription(ej.optString("description", ""));
                        e.setLocation(ej.optString("location", ""));
                        e.setReceiptNumber(ej.optString("receiptNumber", ""));
                        e.setCreatedAt(ej.optString("createdAt", ""));
                        db.insertExpense(e);
                        added++;
                    }
                }

                // 2. Remove local expenses that were deleted on the server
                List<Expense> localExpenses = db.getExpensesByProject(projectId);
                int removed = 0;
                for (Expense local : localExpenses) {
                    if (!serverCodes.contains(local.getExpenseCode())) {
                        db.deleteExpense(local.getId());
                        removed++;
                    }
                }

                if (added > 0 || removed > 0) {
                    final int a = added, r = removed;
                    mainHandler.post(() -> {
                        loadExpenses();
                        String msg = "";
                        if (a > 0) msg += a + " new expense(s) synced. ";
                        if (r > 0) msg += r + " expense(s) removed.";
                        Toast.makeText(appCtx, msg.trim(), Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception ignored) {
                // Server offline — silently skip
            }
        });
    }

    private void loadExpenses() {
        expenseList.clear();
        expenseList.addAll(db.getExpensesByProject(projectId));
        expenseAdapter.notifyDataSetChanged();

        double total = db.getTotalExpensesByProject(projectId);
        double remaining = project.getBudget() - total;

        tvBudgetAmount.setText(String.format(Locale.getDefault(), "$%.2f", project.getBudget()));
        tvTotalSpent.setText(String.format(Locale.getDefault(), "$%.2f", total));
        tvRemaining.setText(String.format(Locale.getDefault(), "$%.2f", remaining));
        tvExpenseCount.setText(expenseList.size() + " record(s)");

        boolean empty = expenseList.isEmpty();
        tvNoExpenses.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvExpenses.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void setRow(int rowId, String label, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvValue)).setText(
                (value == null || value.isEmpty()) ? "—" : value);
    }

    private String nullSafe(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s.trim();
    }

    // ── Expense callbacks ─────────────────────────────────────────────────────

    @Override
    public void onExpenseEdit(Expense expense) {
        Intent intent = new Intent(this, AddEditExpenseActivity.class);
        intent.putExtra(AddEditExpenseActivity.EXTRA_PROJECT_ID, projectId);
        intent.putExtra(AddEditExpenseActivity.EXTRA_EXPENSE, expense);
        startActivity(intent);
    }

    @Override
    public void onExpenseDelete(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_expense_title)
                .setMessage(R.string.dialog_delete_expense_msg)
                .setPositiveButton(R.string.dialog_yes_delete, (d, w) -> {
                    db.deleteExpense(expense.getId());
                    Toast.makeText(this, R.string.msg_expense_deleted,
                            Toast.LENGTH_SHORT).show();
                    loadExpenses();

                    // Sync deletion to server so Flutter users see the change
                    android.content.Context appCtx = getApplicationContext();
                    CloudSyncManager.getInstance().deleteExpense(
                            appCtx,
                            project.getProjectCode(),
                            expense.getExpenseCode(),
                            (success, message) -> {
                                if (!success) {
                                    Toast.makeText(appCtx,
                                            "⚠ Cloud sync failed: " + message,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    // ── Options menu ──────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_project) {
            Intent intent = new Intent(this, AddEditProjectActivity.class);
            intent.putExtra(AddEditProjectActivity.EXTRA_PROJECT, project);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete_project) {
            confirmDeleteProject();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteProject() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_project_title)
                .setMessage(R.string.dialog_delete_project_msg)
                .setPositiveButton(R.string.dialog_yes_delete, (d, w) -> {

                    String projectCode = project != null ? project.getProjectCode() : null;
                    android.util.Log.d("DELETE_SYNC", "confirmDeleteProject fired: " + projectCode);

                    db.deleteProject(projectId);

                    android.content.Context appCtx = getApplicationContext();
                    Toast.makeText(appCtx, "Deleted locally. Syncing to cloud...",
                            Toast.LENGTH_SHORT).show();

                    if (projectCode != null) {
                        CloudSyncManager.getInstance().deleteProject(
                                appCtx, projectCode,
                                (success, message) -> {
                                    if (success) {
                                        Toast.makeText(appCtx,
                                                "Project removed from server!",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(appCtx,
                                                "Sync failed: " + message,
                                                Toast.LENGTH_LONG).show();
                                    }
                                    finish();
                                });
                    } else {
                        finish();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
