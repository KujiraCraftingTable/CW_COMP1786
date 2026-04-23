package com.example.expensetrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetrackerapp.adapter.ProjectAdapter;
import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Project;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity
        implements ProjectAdapter.OnProjectClickListener {

    private TextInputEditText etSearchQuery;
    private TextInputEditText etFilterDate, etFilterOwner;
    private AutoCompleteTextView actvFilterStatus;

    private View panelBasicSearch, panelAdvancedSearch;
    private TextView tvResultCount, tvNoResults;
    private RecyclerView rvSearchResults;

    private ProjectAdapter adapter;
    private final List<Project> resultList = new ArrayList<>();
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setTitle("Search Projects");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = DatabaseHelper.getInstance(this);
        bindViews();
        setupTabs();
        setupAdapter();
        setupStatusDropdown();
        setupListeners();
    }

    private void bindViews() {
        etSearchQuery     = findViewById(R.id.etSearchQuery);
        etFilterDate      = findViewById(R.id.etFilterDate);
        etFilterOwner     = findViewById(R.id.etFilterOwner);
        actvFilterStatus  = findViewById(R.id.actvFilterStatus);
        panelBasicSearch  = findViewById(R.id.panelBasicSearch);
        panelAdvancedSearch = findViewById(R.id.panelAdvancedSearch);
        tvResultCount     = findViewById(R.id.tvResultCount);
        tvNoResults       = findViewById(R.id.tvNoResults);
        rvSearchResults   = findViewById(R.id.rvSearchResults);
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Basic Search"));
        tabLayout.addTab(tabLayout.newTab().setText("Advanced Search"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    panelBasicSearch.setVisibility(View.VISIBLE);
                    panelAdvancedSearch.setVisibility(View.GONE);
                } else {
                    panelBasicSearch.setVisibility(View.GONE);
                    panelAdvancedSearch.setVisibility(View.VISIBLE);
                }
                clearResults();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAdapter() {
        adapter = new ProjectAdapter(this, resultList, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupStatusDropdown() {
        String[] statuses = {"", "Active", "Completed", "On Hold"};
        actvFilterStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, statuses));
    }

    private void setupListeners() {
        // Basic search
        findViewById(R.id.btnBasicSearch).setOnClickListener(v -> runBasicSearch());
        etSearchQuery.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                runBasicSearch();
                return true;
            }
            return false;
        });

        // Advanced search
        findViewById(R.id.btnAdvancedSearch).setOnClickListener(v -> runAdvancedSearch());
        findViewById(R.id.btnClearFilters).setOnClickListener(v -> {
            etFilterDate.setText("");
            actvFilterStatus.setText("", false);
            etFilterOwner.setText("");
            clearResults();
        });
    }

    private void runBasicSearch() {
        String query = Objects.requireNonNull(etSearchQuery.getText()).toString().trim();
        List<Project> results = query.isEmpty()
                ? db.getAllProjects()
                : db.searchProjects(query);
        showResults(results);
    }

    private void runAdvancedSearch() {
        String date   = Objects.requireNonNull(etFilterDate.getText()).toString().trim();
        String status = actvFilterStatus.getText().toString().trim();
        String owner  = Objects.requireNonNull(etFilterOwner.getText()).toString().trim();
        List<Project> results = db.advancedSearchProjects(date, status, owner);
        showResults(results);
    }

    private void showResults(List<Project> results) {
        resultList.clear();
        resultList.addAll(results);
        adapter.notifyDataSetChanged();

        tvResultCount.setText(results.size() + " result(s)");
        boolean empty = results.isEmpty();
        rvSearchResults.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvNoResults.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void clearResults() {
        resultList.clear();
        adapter.notifyDataSetChanged();
        tvResultCount.setText("");
        rvSearchResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);
    }

    // ── ProjectAdapter callbacks ───────────────────────────────────────────────

    @Override
    public void onProjectClick(Project project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getId());
        startActivity(intent);
    }

    @Override
    public void onProjectEdit(Project project) {
        Intent intent = new Intent(this, AddEditProjectActivity.class);
        intent.putExtra(AddEditProjectActivity.EXTRA_PROJECT, project);
        startActivity(intent);
    }

    @Override
    public void onProjectDelete(Project project) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_project_title)
                .setMessage(R.string.dialog_delete_project_msg)
                .setPositiveButton(R.string.dialog_yes_delete, (d, w) -> {
                    db.deleteProject(project.getId());
                    android.widget.Toast.makeText(this, R.string.msg_project_deleted,
                            android.widget.Toast.LENGTH_SHORT).show();
                    runBasicSearch(); // refresh
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
