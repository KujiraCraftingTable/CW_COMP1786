package com.example.expensetrackerapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Expense;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.expensetrackerapp.model.Project;

import java.util.Calendar;
import java.util.Objects;

public class AddEditExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT_ID = "extra_expense_project_id";
    public static final String EXTRA_EXPENSE    = "extra_expense";

    private int projectId;
    private Expense editingExpense;
    private DatabaseHelper db;

    private TextInputLayout tilExpenseCode, tilExpenseDate, tilAmount, tilCurrency,
            tilExpenseType, tilPaymentMethod, tilClaimant, tilPaymentStatus;

    private TextInputEditText etExpenseCode, etExpenseDate, etAmount,
            etClaimant, etExpenseDesc, etLocation, etReceiptNumber;

    private AutoCompleteTextView actvCurrency, actvExpenseType,
            actvPaymentMethod, actvPaymentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_expense);

        db = DatabaseHelper.getInstance(this);
        projectId = getIntent().getIntExtra(EXTRA_PROJECT_ID, -1);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindViews();
        setupDropdowns();

        etExpenseDate.setOnClickListener(v -> showDatePicker());
        ((TextInputLayout) findViewById(R.id.tilExpenseDate))
                .setEndIconOnClickListener(v -> showDatePicker());

        if (getIntent().hasExtra(EXTRA_EXPENSE)) {
            editingExpense = (Expense) getIntent().getSerializableExtra(EXTRA_EXPENSE);
            setTitle("Edit Expense");
            populateFields(editingExpense);
            // Expense code is the unique key on the server — changing it during edit
            // would create a duplicate instead of updating the existing record.
            etExpenseCode.setEnabled(false);
            tilExpenseCode.setHelperText("Code cannot be changed after creation");
        } else {
            setTitle("Add Expense");
        }

        findViewById(R.id.btnSaveExpense).setOnClickListener(v -> {
            if (validateFields()) saveExpense();
        });
    }

    private void bindViews() {
        tilExpenseCode    = findViewById(R.id.tilExpenseCode);
        tilExpenseDate    = findViewById(R.id.tilExpenseDate);
        tilAmount         = findViewById(R.id.tilAmount);
        tilCurrency       = findViewById(R.id.tilCurrency);
        tilExpenseType    = findViewById(R.id.tilExpenseType);
        tilPaymentMethod  = findViewById(R.id.tilPaymentMethod);
        tilClaimant       = findViewById(R.id.tilClaimant);
        tilPaymentStatus  = findViewById(R.id.tilPaymentStatus);

        etExpenseCode     = findViewById(R.id.etExpenseCode);
        etExpenseDate     = findViewById(R.id.etExpenseDate);
        etAmount          = findViewById(R.id.etAmount);
        etClaimant        = findViewById(R.id.etClaimant);
        etExpenseDesc     = findViewById(R.id.etExpenseDesc);
        etLocation        = findViewById(R.id.etLocation);
        etReceiptNumber   = findViewById(R.id.etReceiptNumber);

        actvCurrency      = findViewById(R.id.actvCurrency);
        actvExpenseType   = findViewById(R.id.actvExpenseType);
        actvPaymentMethod = findViewById(R.id.actvPaymentMethod);
        actvPaymentStatus = findViewById(R.id.actvPaymentStatus);
    }

    private void setupDropdowns() {
        String[] currencies = {"USD", "EUR", "GBP", "AUD", "CAD", "JPY", "CNY", "SGD", "OTHER"};
        actvCurrency.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, currencies));

        String[] types = {"Travel", "Equipment", "Materials", "Services",
                "Software/Licenses", "Labour Costs", "Utilities", "Miscellaneous"};
        actvExpenseType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, types));

        String[] methods = {"Cash", "Credit Card", "Bank Transfer", "Cheque"};
        actvPaymentMethod.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, methods));

        String[] statuses = {"Paid", "Pending", "Reimbursed"};
        actvPaymentStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, statuses));
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) ->
                        etExpenseDate.setText(
                                String.format("%04d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void populateFields(Expense e) {
        etExpenseCode.setText(e.getExpenseCode());
        etExpenseDate.setText(e.getExpenseDate());
        etAmount.setText(String.valueOf(e.getAmount()));
        actvCurrency.setText(e.getCurrency(), false);
        actvExpenseType.setText(e.getExpenseType(), false);
        actvPaymentMethod.setText(e.getPaymentMethod(), false);
        etClaimant.setText(e.getClaimant());
        actvPaymentStatus.setText(e.getPaymentStatus(), false);
        etExpenseDesc.setText(e.getDescription());
        etLocation.setText(e.getLocation());
        etReceiptNumber.setText(e.getReceiptNumber());
    }

    private boolean validateFields() {
        boolean valid = true;

        if (isEmpty(etExpenseCode)) {
            tilExpenseCode.setError(getString(R.string.error_enter_expense_code));
            valid = false;
        } else { tilExpenseCode.setError(null); }

        if (isEmpty(etExpenseDate)) {
            tilExpenseDate.setError(getString(R.string.error_enter_expense_date));
            valid = false;
        } else { tilExpenseDate.setError(null); }

        if (isEmpty(etAmount)) {
            tilAmount.setError(getString(R.string.error_enter_amount));
            valid = false;
        } else {
            try {
                Double.parseDouble(
                        Objects.requireNonNull(etAmount.getText()).toString().trim());
                tilAmount.setError(null);
            } catch (NumberFormatException e) {
                tilAmount.setError(getString(R.string.error_invalid_amount));
                valid = false;
            }
        }

        if (actvCurrency.getText().toString().trim().isEmpty()) {
            tilCurrency.setError(getString(R.string.error_enter_currency));
            valid = false;
        } else { tilCurrency.setError(null); }

        if (actvExpenseType.getText().toString().trim().isEmpty()) {
            tilExpenseType.setError(getString(R.string.error_enter_expense_type));
            valid = false;
        } else { tilExpenseType.setError(null); }

        if (actvPaymentMethod.getText().toString().trim().isEmpty()) {
            tilPaymentMethod.setError(getString(R.string.error_enter_payment_method));
            valid = false;
        } else { tilPaymentMethod.setError(null); }

        if (isEmpty(etClaimant)) {
            tilClaimant.setError(getString(R.string.error_enter_claimant));
            valid = false;
        } else { tilClaimant.setError(null); }

        if (actvPaymentStatus.getText().toString().trim().isEmpty()) {
            tilPaymentStatus.setError(getString(R.string.error_enter_payment_status));
            valid = false;
        } else { tilPaymentStatus.setError(null); }

        return valid;
    }

    private void saveExpense() {
        Expense e = editingExpense != null ? editingExpense : new Expense();
        e.setProjectId(projectId);
        e.setExpenseCode(text(etExpenseCode));
        e.setExpenseDate(text(etExpenseDate));
        e.setAmount(Double.parseDouble(text(etAmount)));
        e.setCurrency(actvCurrency.getText().toString().trim());
        e.setExpenseType(actvExpenseType.getText().toString().trim());
        e.setPaymentMethod(actvPaymentMethod.getText().toString().trim());
        e.setClaimant(text(etClaimant));
        e.setPaymentStatus(actvPaymentStatus.getText().toString().trim());
        e.setDescription(text(etExpenseDesc));
        e.setLocation(text(etLocation));
        e.setReceiptNumber(text(etReceiptNumber));

        if (editingExpense != null) {
            db.updateExpense(e);
        } else {
            e.setCreatedAt(java.time.LocalDate.now().toString());
            db.insertExpense(e);
        }

        Toast.makeText(this, "Saved locally. Syncing to cloud...", Toast.LENGTH_SHORT).show();
        android.content.Context appCtx = getApplicationContext();
        Project project = db.getProjectById(projectId);
        if (project != null) {
            CloudSyncManager.getInstance().syncExpense(
                    appCtx,
                    project.getProjectCode(),
                    e,
                    (success, message) -> {
                        if (success) {
                            Toast.makeText(appCtx, "Expense synced to cloud!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(appCtx, "Sync failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        }
        finish();
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
