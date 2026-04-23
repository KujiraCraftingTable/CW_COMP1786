package com.example.expensetrackerapp.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetrackerapp.R;
import com.example.expensetrackerapp.model.Expense;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseClickListener {
        void onExpenseEdit(Expense expense);
        void onExpenseDelete(Expense expense);
    }

    private final List<Expense> expenses;
    private final Context context;
    private final OnExpenseClickListener listener;

    public ExpenseAdapter(Context context, List<Expense> expenses,
                          OnExpenseClickListener listener) {
        this.context = context;
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder h, int position) {
        Expense e = expenses.get(position);

        h.tvExpenseCode.setText(e.getExpenseCode());
        h.tvExpenseDate.setText(e.getExpenseDate());
        h.tvExpenseType.setText(e.getExpenseType());
        h.tvClaimant.setText(e.getClaimant());
        h.tvAmount.setText(String.format(Locale.getDefault(),
                "%s %.2f", e.getCurrency(), e.getAmount()));
        h.tvPaymentMethod.setText(e.getPaymentMethod());
        h.tvPaymentStatus.setText(e.getPaymentStatus());

        // Payment status color
        int statusColor;
        switch (e.getPaymentStatus()) {
            case "Paid":
                statusColor = ContextCompat.getColor(context, R.color.paymentPaid);
                break;
            case "Reimbursed":
                statusColor = ContextCompat.getColor(context, R.color.paymentReimbursed);
                break;
            default: // Pending
                statusColor = ContextCompat.getColor(context, R.color.paymentPending);
                break;
        }
        h.tvPaymentStatus.setBackgroundTintList(ColorStateList.valueOf(statusColor));

        // Optional fields
        if (e.getDescription() != null && !e.getDescription().isEmpty()) {
            h.tvDescription.setVisibility(View.VISIBLE);
            h.tvDescription.setText(e.getDescription());
        } else {
            h.tvDescription.setVisibility(View.GONE);
        }

        if (e.getLocation() != null && !e.getLocation().isEmpty()) {
            h.tvLocation.setVisibility(View.VISIBLE);
            h.tvLocation.setText(e.getLocation());
        } else {
            h.tvLocation.setVisibility(View.GONE);
        }

        h.btnEditExpense.setOnClickListener(v -> listener.onExpenseEdit(e));
        h.btnDeleteExpense.setOnClickListener(v -> listener.onExpenseDelete(e));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void updateData(List<Expense> newList) {
        expenses.clear();
        expenses.addAll(newList);
        notifyDataSetChanged();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpenseCode, tvExpenseDate, tvExpenseType, tvClaimant,
                 tvAmount, tvPaymentMethod, tvPaymentStatus,
                 tvDescription, tvLocation;
        View btnEditExpense, btnDeleteExpense;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExpenseCode   = itemView.findViewById(R.id.tvExpenseCode);
            tvExpenseDate   = itemView.findViewById(R.id.tvExpenseDate);
            tvExpenseType   = itemView.findViewById(R.id.tvExpenseType);
            tvClaimant      = itemView.findViewById(R.id.tvClaimant);
            tvAmount        = itemView.findViewById(R.id.tvAmount);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvDescription   = itemView.findViewById(R.id.tvExpenseDescription);
            tvLocation      = itemView.findViewById(R.id.tvExpenseLocation);
            btnEditExpense  = itemView.findViewById(R.id.btnEditExpense);
            btnDeleteExpense= itemView.findViewById(R.id.btnDeleteExpense);
        }
    }
}
