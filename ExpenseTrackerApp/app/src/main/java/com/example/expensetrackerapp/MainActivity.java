package com.example.expensetrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Project;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = DatabaseHelper.getInstance(this);

        findViewById(R.id.btnAddProject).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditProjectActivity.class)));

        findViewById(R.id.btnViewProjects).setOnClickListener(v ->
                startActivity(new Intent(this, ProjectListActivity.class)));

        findViewById(R.id.btnSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        findViewById(R.id.btnUploadCloud).setOnClickListener(v ->
                startActivity(new Intent(this, UploadActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        List<Project> projects = db.getAllProjects();
        int total = projects.size();
        int active = 0, completed = 0;
        for (Project p : projects) {
            if ("Active".equals(p.getStatus())) active++;
            else if ("Completed".equals(p.getStatus())) completed++;
        }

        ((TextView) findViewById(R.id.tvTotalProjects)).setText(String.valueOf(total));
        ((TextView) findViewById(R.id.tvActiveProjects)).setText(String.valueOf(active));
        ((TextView) findViewById(R.id.tvCompletedProjects)).setText(String.valueOf(completed));
        ((TextView) findViewById(R.id.tvPendingSync))
                .setText(String.valueOf(db.getUnsyncedProjectCount()));
    }
}
