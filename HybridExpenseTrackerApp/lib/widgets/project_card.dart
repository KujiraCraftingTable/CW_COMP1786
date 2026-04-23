import 'package:flutter/material.dart';
import '../models/project.dart';

/// A reusable card widget that displays a summary of a [Project].
/// Tapping the card navigates to the project detail screen.
class ProjectCard extends StatelessWidget {
  final Project project;
  final bool isFavourite;
  final VoidCallback onTap;
  final VoidCallback onFavouriteToggle;

  const ProjectCard({
    super.key,
    required this.project,
    required this.isFavourite,
    required this.onTap,
    required this.onFavouriteToggle,
  });

  @override
  Widget build(BuildContext context) {
    final theme      = Theme.of(context);
    final statusColor = _statusColor(project.status);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ── Header row ────────────────────────────────────────────────
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Project code chip
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      project.projectCode,
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.onPrimaryContainer,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  // Project name
                  Expanded(
                    child: Text(
                      project.projectName,
                      style: theme.textTheme.titleMedium
                          ?.copyWith(fontWeight: FontWeight.bold),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  // Favourite button
                  GestureDetector(
                    onTap: onFavouriteToggle,
                    child: Icon(
                      isFavourite ? Icons.star : Icons.star_border,
                      color: isFavourite ? Colors.amber : Colors.grey,
                      size: 22,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),

              // ── Description ────────────────────────────────────────────────
              Text(
                project.description,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: Colors.grey[600]),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 10),

              // ── Footer row ────────────────────────────────────────────────
              Row(
                children: [
                  // Status badge
                  _StatusBadge(label: project.status, color: statusColor),
                  const SizedBox(width: 8),
                  // Manager
                  Icon(Icons.person_outline, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 3),
                  Expanded(
                    child: Text(
                      project.manager,
                      style: theme.textTheme.labelSmall
                          ?.copyWith(color: Colors.grey[600]),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  // Expense count
                  Icon(Icons.receipt_long_outlined, size: 14,
                      color: Colors.grey[600]),
                  const SizedBox(width: 3),
                  Text(
                    '${project.expenses.length} expense(s)',
                    style: theme.textTheme.labelSmall
                        ?.copyWith(color: Colors.grey[600]),
                  ),
                ],
              ),
              const SizedBox(height: 4),

              // ── Date range ────────────────────────────────────────────────
              Row(
                children: [
                  Icon(Icons.date_range, size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 3),
                  Text(
                    '${project.startDate}  →  ${project.endDate}',
                    style: theme.textTheme.labelSmall
                        ?.copyWith(color: Colors.grey[500]),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Color _statusColor(String status) {
    switch (status.toLowerCase()) {
      case 'active':    return Colors.green;
      case 'completed': return Colors.blue;
      case 'on hold':   return Colors.orange;
      default:          return Colors.grey;
    }
  }
}

class _StatusBadge extends StatelessWidget {
  final String label;
  final Color color;
  const _StatusBadge({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withOpacity(0.5)),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          color: color.withAlpha(220),
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
