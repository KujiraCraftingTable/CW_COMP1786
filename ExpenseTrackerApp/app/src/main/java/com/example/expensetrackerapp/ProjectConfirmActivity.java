package com.example.expensetrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Project;

import java.util.Locale;

public class ProjectConfirmActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT = "extra_project_confirm";

    private Project project;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_confirm);

        db = DatabaseHelper.getInstance(this);

        project = (Project) getIntent().getSerializableExtra(EXTRA_PROJECT);
        if (project == null) { finish(); return; }

        setTitle(project.getId() > 0 ? "Confirm Edit" : "Confirm Project");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        populateConfirmation();

        findViewById(R.id.btnBackEdit).setOnClickListener(v -> finish());
        findViewById(R.id.btnConfirmSave).setOnClickListener(v -> saveProject());
    }

    private void populateConfirmation() {
        setRow(R.id.rowProjectCode, "Project ID / Code", project.getProjectCode());
        setRow(R.id.rowProjectName, "Project Name", project.getProjectName());
        setRow(R.id.rowDescription, "Description", project.getDescription());
        setRow(R.id.rowManager, "Project Manager", project.getManager());
        setRow(R.id.rowStartDate, "Start Date", project.getStartDate());
        setRow(R.id.rowEndDate, "End Date", project.getEndDate());
        setRow(R.id.rowStatus, "Status", project.getStatus());
        setRow(R.id.rowPriority, "Priority", project.getPriority());
        setRow(R.id.rowBudget, "Budget",
                String.format(Locale.getDefault(), "$%.2f", project.getBudget()));
        setRow(R.id.rowSpecialReqs, "Special Requirements",
                nullSafe(project.getSpecialRequirements()));
        setRow(R.id.rowClientDept, "Client / Department",
                nullSafe(project.getClientDepartment()));
        setRow(R.id.rowNotes, "Notes", nullSafe(project.getNotes()));
    }

    private void setRow(int rowId, String label, String value) {
        android.view.View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvValue)).setText(
                (value == null || value.isEmpty()) ? "—" : value);
    }

    private String nullSafe(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }

    private void saveProject() {
        if (project.getId() > 0) {
            db.updateProject(project);
        } else {
            long newId = db.insertProject(project);
            project.setId((int) newId);
        }

        Toast.makeText(this, "Saved locally. Syncing to cloud...", Toast.LENGTH_SHORT).show();

        android.content.Context appCtx = getApplicationContext();
        CloudSyncManager.getInstance().syncProject(
                this,
                project,
                (success, message) -> {
                    if (success) {
                        Toast.makeText(appCtx, "Cloud synced!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(appCtx, "Sync failed: " + message, Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Navigate to project detail with clean back stack
        Intent detailIntent = new Intent(this, ProjectDetailActivity.class);
        detailIntent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getId());
        androidx.core.app.TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(detailIntent)
                .startActivities();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
