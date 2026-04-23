package com.example.expensetrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetrackerapp.adapter.ProjectAdapter;
import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectListActivity extends AppCompatActivity
        implements ProjectAdapter.OnProjectClickListener {

    private RecyclerView rvProjects;
    private View layoutEmpty;
    private ProjectAdapter adapter;
    private DatabaseHelper db;
    private final List<Project> projectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);
        setTitle("All Projects");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = DatabaseHelper.getInstance(this);

        rvProjects   = findViewById(R.id.rvProjects);
        layoutEmpty  = findViewById(R.id.layoutEmpty);

        adapter = new ProjectAdapter(this, projectList, this);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);

        findViewById(R.id.fabAddProject).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditProjectActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void loadProjects() {
        projectList.clear();
        projectList.addAll(db.getAllProjects());
        adapter.notifyDataSetChanged();

        boolean empty = projectList.isEmpty();
        rvProjects.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

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
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_project_title)
                .setMessage(R.string.dialog_delete_project_msg)
                .setPositiveButton(R.string.dialog_yes_delete, (d, w) -> {
                    android.util.Log.d("DELETE_SYNC", "onProjectDelete fired: " + project.getProjectCode());
                    db.deleteProject(project.getId());
                    loadProjects();

                    // Pattern mirrors AddEditExpenseActivity.saveExpense():
                    // immediate "syncing" toast, then final result toast.
                    Toast.makeText(this, "Deleted locally. Syncing to cloud...",
                            Toast.LENGTH_SHORT).show();
                    android.content.Context appCtx = getApplicationContext();
                    CloudSyncManager.getInstance().deleteProject(
                            appCtx, project.getProjectCode(),
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
                            });
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reset_db) {
            confirmResetDatabase();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmResetDatabase() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_reset_db_title)
                .setMessage(R.string.dialog_reset_db_msg)
                .setPositiveButton(R.string.dialog_yes_reset, (d, w) -> {
                    db.resetDatabase();
                    Toast.makeText(this, R.string.msg_db_reset,
                            Toast.LENGTH_SHORT).show();
                    loadProjects();
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
