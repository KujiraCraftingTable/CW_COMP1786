package com.example.expensetrackerapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetrackerapp.model.Project;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Objects;

public class AddEditProjectActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT = "extra_project";

    private TextInputLayout tilProjectCode, tilProjectName, tilDescription,
            tilManager, tilStartDate, tilEndDate, tilStatus, tilPriority, tilBudget;

    private TextInputEditText etProjectCode, etProjectName, etDescription,
            etManager, etStartDate, etEndDate, etBudget,
            etSpecialReqs, etClientDept, etNotes;

    private AutoCompleteTextView actvStatus, actvPriority;

    private Project editingProject; // non-null when editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_project);

        bindViews();
        setupDropdowns();
        setupDatePickers();

        // Check if editing an existing project
        if (getIntent().hasExtra(EXTRA_PROJECT)) {
            editingProject = (Project) getIntent().getSerializableExtra(EXTRA_PROJECT);
            setTitle("Edit Project");
            populateFields(editingProject);
            // Project code is the unique key on the server — changing it during edit
            // would create a duplicate instead of updating the existing record.
            etProjectCode.setEnabled(false);
            tilProjectCode.setHelperText("Project code cannot be changed after creation");
        } else {
            setTitle("Add New Project");
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.btnNext).setOnClickListener(v -> {
            if (validateFields()) {
                launchConfirmation();
            }
        });
    }

    private void bindViews() {
        tilProjectCode  = findViewById(R.id.tilProjectCode);
        tilProjectName  = findViewById(R.id.tilProjectName);
        tilDescription  = findViewById(R.id.tilDescription);
        tilManager      = findViewById(R.id.tilManager);
        tilStartDate    = findViewById(R.id.tilStartDate);
        tilEndDate      = findViewById(R.id.tilEndDate);
        tilStatus       = findViewById(R.id.tilStatus);
        tilPriority     = findViewById(R.id.tilPriority);
        tilBudget       = findViewById(R.id.tilBudget);

        etProjectCode   = findViewById(R.id.etProjectCode);
        etProjectName   = findViewById(R.id.etProjectName);
        etDescription   = findViewById(R.id.etDescription);
        etManager       = findViewById(R.id.etManager);
        etStartDate     = findViewById(R.id.etStartDate);
        etEndDate       = findViewById(R.id.etEndDate);
        etBudget        = findViewById(R.id.etBudget);
        etSpecialReqs   = findViewById(R.id.etSpecialReqs);
        etClientDept    = findViewById(R.id.etClientDept);
        etNotes         = findViewById(R.id.etNotes);

        actvStatus      = findViewById(R.id.actvStatus);
        actvPriority    = findViewById(R.id.actvPriority);
    }

    private void setupDropdowns() {
        String[] statuses = {"Active", "Completed", "On Hold"};
        actvStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, statuses));

        String[] priorities = {"High", "Medium", "Low"};
        actvPriority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, priorities));
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        tilStartDate.setEndIconOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        tilEndDate.setEndIconOnClickListener(v -> showDatePicker(etEndDate));
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) ->
                        target.setText(String.format("%04d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void populateFields(Project p) {
        etProjectCode.setText(p.getProjectCode());
        etProjectName.setText(p.getProjectName());
        etDescription.setText(p.getDescription());
        etManager.setText(p.getManager());
        etStartDate.setText(p.getStartDate());
        etEndDate.setText(p.getEndDate());
        actvStatus.setText(p.getStatus(), false);
        actvPriority.setText(p.getPriority(), false);
        etBudget.setText(String.valueOf(p.getBudget()));
        etSpecialReqs.setText(p.getSpecialRequirements());
        etClientDept.setText(p.getClientDepartment());
        etNotes.setText(p.getNotes());
    }

    private boolean validateFields() {
        boolean valid = true;

        if (isEmpty(etProjectCode)) {
            tilProjectCode.setError(getString(R.string.error_enter_project_code));
            valid = false;
        } else { tilProjectCode.setError(null); }

        if (isEmpty(etProjectName)) {
            tilProjectName.setError(getString(R.string.error_enter_project_name));
            valid = false;
        } else { tilProjectName.setError(null); }

        if (isEmpty(etDescription)) {
            tilDescription.setError(getString(R.string.error_enter_description));
            valid = false;
        } else { tilDescription.setError(null); }

        if (isEmpty(etManager)) {
            tilManager.setError(getString(R.string.error_enter_manager));
            valid = false;
        } else { tilManager.setError(null); }

        if (isEmpty(etStartDate)) {
            tilStartDate.setError(getString(R.string.error_enter_start_date));
            valid = false;
        } else { tilStartDate.setError(null); }

        if (isEmpty(etEndDate)) {
            tilEndDate.setError(getString(R.string.error_enter_end_date));
            valid = false;
        } else { tilEndDate.setError(null); }

        if (actvStatus.getText().toString().trim().isEmpty()) {
            tilStatus.setError(getString(R.string.error_enter_status));
            valid = false;
        } else { tilStatus.setError(null); }

        if (actvPriority.getText().toString().trim().isEmpty()) {
            tilPriority.setError(getString(R.string.error_enter_priority));
            valid = false;
        } else { tilPriority.setError(null); }

        if (isEmpty(etBudget)) {
            tilBudget.setError(getString(R.string.error_enter_budget));
            valid = false;
        } else {
            try {
                Double.parseDouble(Objects.requireNonNull(etBudget.getText()).toString().trim());
                tilBudget.setError(null);
            } catch (NumberFormatException e) {
                tilBudget.setError(getString(R.string.error_invalid_budget));
                valid = false;
            }
        }

        // Date order check
        if (valid && !isEmpty(etStartDate) && !isEmpty(etEndDate)) {
            String start = etStartDate.getText().toString().trim();
            String end   = etEndDate.getText().toString().trim();
            if (start.compareTo(end) > 0) {
                tilEndDate.setError(getString(R.string.error_date_order));
                valid = false;
            } else {
                tilEndDate.setError(null);
            }
        }

        return valid;
    }

    private void launchConfirmation() {
        Project p = new Project();
        p.setProjectCode(text(etProjectCode));
        p.setProjectName(text(etProjectName));
        p.setDescription(text(etDescription));
        p.setManager(text(etManager));
        p.setStartDate(text(etStartDate));
        p.setEndDate(text(etEndDate));
        p.setStatus(actvStatus.getText().toString().trim());
        p.setPriority(actvPriority.getText().toString().trim());
        p.setBudget(Double.parseDouble(text(etBudget)));
        p.setSpecialRequirements(text(etSpecialReqs));
        p.setClientDepartment(text(etClientDept));
        p.setNotes(text(etNotes));

        if (editingProject != null) {
            p.setId(editingProject.getId());
            p.setCreatedAt(editingProject.getCreatedAt());
        } else {
            p.setCreatedAt(java.time.LocalDate.now().toString());
        }

        Intent intent = new Intent(this, ProjectConfirmActivity.class);
        intent.putExtra(ProjectConfirmActivity.EXTRA_PROJECT, p);
        startActivity(intent);
    }

    private boolean isEmpty(TextInputEditText et) {
        return Objects.requireNonNull(et.getText()).toString().trim().isEmpty();
    }

    private String text(TextInputEditText et) {
        return Objects.requireNonNull(et.getText()).toString().trim();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
