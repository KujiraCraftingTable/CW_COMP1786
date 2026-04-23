package com.example.expensetrackerapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetrackerapp.R;
import com.example.expensetrackerapp.database.DatabaseHelper;
import com.example.expensetrackerapp.model.Project;

import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
        void onProjectEdit(Project project);
        void onProjectDelete(Project project);
    }

    private final List<Project> projects;
    private final Context context;
    private final OnProjectClickListener listener;

    public ProjectAdapter(Context context, List<Project> projects,
                          OnProjectClickListener listener) {
        this.context = context;
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder h, int position) {
        Project p = projects.get(position);

        h.tvProjectName.setText(p.getProjectName());
        h.tvProjectCode.setText(p.getProjectCode());
        h.tvManager.setText(p.getManager());
        h.tvDateRange.setText(String.format("%s → %s", p.getStartDate(), p.getEndDate()));
        h.tvStatus.setText(p.getStatus());
        h.tvPriority.setText(p.getPriority());

        // Status badge color
        int statusColor;
        switch (p.getStatus()) {
            case "Active":
                statusColor = ContextCompat.getColor(context, R.color.statusActive);
                break;
            case "Completed":
                statusColor = ContextCompat.getColor(context, R.color.statusCompleted);
                break;
            default: // On Hold
                statusColor = ContextCompat.getColor(context, R.color.statusOnHold);
                break;
        }
        h.tvStatus.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(statusColor));

        // Priority badge color
        int priorityColor;
        switch (p.getPriority()) {
            case "High":
                priorityColor = ContextCompat.getColor(context, R.color.priorityHigh);
                break;
            case "Medium":
                priorityColor = ContextCompat.getColor(context, R.color.priorityMedium);
                break;
            default: // Low
                priorityColor = ContextCompat.getColor(context, R.color.priorityLow);
                break;
        }
        h.tvPriority.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(priorityColor));

        // Budget and expense total
        double total = DatabaseHelper.getInstance(context)
                .getTotalExpensesByProject(p.getId());
        h.tvBudget.setText(String.format(Locale.getDefault(),
                "Budget: $%.2f", p.getBudget()));
        h.tvExpenseTotal.setText(String.format(Locale.getDefault(),
                "Spent: $%.2f", total));

        // Sync badge
        if (p.isSynced()) {
            h.tvSyncBadge.setText("✓ Synced");
            h.tvSyncBadge.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.statusActive)));
        } else {
            h.tvSyncBadge.setText("↑ Pending");
            h.tvSyncBadge.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.statusOnHold)));
        }

        // Click handlers
        h.itemView.setOnClickListener(v -> listener.onProjectClick(p));
        h.btnEdit.setOnClickListener(v -> listener.onProjectEdit(p));
        h.btnDelete.setOnClickListener(v -> listener.onProjectDelete(p));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void updateData(List<Project> newList) {
        projects.clear();
        projects.addAll(newList);
        notifyDataSetChanged();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectName, tvProjectCode, tvManager, tvDateRange,
                 tvStatus, tvPriority, tvBudget, tvExpenseTotal, tvSyncBadge;
        View btnEdit, btnDelete;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProjectName  = itemView.findViewById(R.id.tvProjectName);
            tvProjectCode  = itemView.findViewById(R.id.tvProjectCode);
            tvManager      = itemView.findViewById(R.id.tvManager);
            tvDateRange    = itemView.findViewById(R.id.tvDateRange);
            tvStatus       = itemView.findViewById(R.id.tvStatus);
            tvPriority     = itemView.findViewById(R.id.tvPriority);
            tvBudget       = itemView.findViewById(R.id.tvBudget);
            tvExpenseTotal = itemView.findViewById(R.id.tvExpenseTotal);
            tvSyncBadge    = itemView.findViewById(R.id.tvSyncBadge);
            btnEdit        = itemView.findViewById(R.id.btnEdit);
            btnDelete      = itemView.findViewById(R.id.btnDelete);
        }
    }
}
