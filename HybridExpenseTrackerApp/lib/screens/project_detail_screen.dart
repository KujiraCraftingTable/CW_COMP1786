import 'dart:async';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/project.dart';
import '../models/expense.dart';
import '../services/favourites_service.dart';
import '../services/api_service.dart';
import 'add_expense_screen.dart';

/// Displays all details of a single [Project], including its expense list.
/// The user can add new expenses and upload them to the cloud server.
/// Also allows the user to toggle the project as a favourite.
class ProjectDetailScreen extends StatefulWidget {
  final Project project;

  const ProjectDetailScreen({super.key, required this.project});

  @override
  State<ProjectDetailScreen> createState() => _ProjectDetailScreenState();
}

class _ProjectDetailScreenState extends State<ProjectDetailScreen> {
  bool _isFavourite = false;
  // Local copy of expenses so we can update after adding
  late List<Expense> _expenses;
  Timer? _autoRefreshTimer;
  final _favService  = FavouritesService.instance;
  final _apiService  = ApiService.instance;
  final _currencyFmt = NumberFormat.currency(symbol: '\$');

  @override
  void initState() {
    super.initState();
    _expenses = List.from(widget.project.expenses);
    _loadFavouriteState();
    // Auto-refresh expenses every 10 seconds
    _autoRefreshTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      _refreshExpenses();
    });
  }

  @override
  void dispose() {
    _autoRefreshTimer?.cancel();
    super.dispose();
  }

  Future<void> _refreshExpenses() async {
    try {
      final projects = await _apiService.fetchProjects();
      // If the project no longer exists on the server (admin deleted it),
      // pop back to the list so the user doesn't sit on stale data.
      final stillExists = projects.any(
        (p) => p.projectCode == widget.project.projectCode,
      );
      if (!stillExists) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('This project was deleted on the server.'),
            duration: Duration(seconds: 3),
          ));
          Navigator.pop(context);
        }
        return;
      }
      final updated = projects.firstWhere(
        (p) => p.projectCode == widget.project.projectCode,
      );
      if (mounted) setState(() => _expenses = List.from(updated.expenses));
    } catch (_) {
      // Server unreachable — keep current list
    }
  }

  Future<void> _loadFavouriteState() async {
    final fav = await _favService.isFavourite(widget.project.projectCode);
    if (mounted) setState(() => _isFavourite = fav);
  }

  Future<void> _toggleFavourite() async {
    final nowFav =
        await _favService.toggleFavourite(widget.project.projectCode);
    if (mounted) {
      setState(() => _isFavourite = nowFav);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(nowFav
            ? '${widget.project.projectName} added to Favourites'
            : 'Removed from Favourites'),
        duration: const Duration(seconds: 2),
      ));
    }
  }

  // ── Add expense ────────────────────────────────────────────────────────────

  Future<void> _goToAddExpense() async {
    final added = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
          builder: (_) => AddExpenseScreen(project: widget.project)),
    );
    if (added == true) {
      // Reload the project list to get the updated expenses from server
      await _refreshExpenses();
    }
  }

  // ── Build ──────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final p     = widget.project;
    final theme = Theme.of(context);
    final totalExpenses =
        _expenses.fold(0.0, (sum, e) => sum + e.amount);

    return Scaffold(
      appBar: AppBar(
        title: Text(p.projectName, overflow: TextOverflow.ellipsis),
        backgroundColor: theme.colorScheme.primaryContainer,
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: _refreshExpenses,
          ),
          IconButton(
            tooltip: _isFavourite ? 'Remove favourite' : 'Add to favourites',
            icon: Icon(
              _isFavourite ? Icons.star : Icons.star_border,
              color: _isFavourite ? Colors.amber : null,
            ),
            onPressed: _toggleFavourite,
          ),
        ],
      ),
      // ── FAB → Add Expense ────────────────────────────────────────────────
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _goToAddExpense,
        icon: const Icon(Icons.add),
        label: const Text('Add Expense'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ── Status banner ──────────────────────────────────────────────
            _StatusBanner(status: p.status),
            const SizedBox(height: 16),

            // ── Project details card ───────────────────────────────────────
            _SectionCard(
              title: 'Project Details',
              children: [
                _DetailRow('Project Code', p.projectCode),
                _DetailRow('Project Name', p.projectName),
                _DetailRow('Description',  p.description),
                _DetailRow('Manager',      p.manager),
                _DetailRow('Start Date',   p.startDate),
                _DetailRow('End Date',     p.endDate),
                _DetailRow('Status',       p.status),
                if (p.priority.isNotEmpty)
                  _DetailRow('Priority', p.priority),
                _DetailRow('Budget',       _currencyFmt.format(p.budget)),
                if (p.clientDepartment.isNotEmpty)
                  _DetailRow('Client / Dept', p.clientDepartment),
                if (p.specialRequirements.isNotEmpty)
                  _DetailRow('Special Reqs', p.specialRequirements),
                if (p.notes.isNotEmpty)
                  _DetailRow('Notes', p.notes),
              ],
            ),
            const SizedBox(height: 16),

            // ── Expenses section ───────────────────────────────────────────
            Row(
              children: [
                Text('Expenses (${_expenses.length})',
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.bold)),
                const Spacer(),
                Text('Total: ${_currencyFmt.format(totalExpenses)}',
                    style: theme.textTheme.labelLarge
                        ?.copyWith(fontWeight: FontWeight.bold,
                            color: theme.colorScheme.primary)),
              ],
            ),
            const SizedBox(height: 8),

            if (_expenses.isEmpty)
              const _EmptyExpenses()
            else
              ..._expenses.map((e) => _ExpenseCard(expense: e)),
          ],
        ),
      ),
    );
  }
}

// ── Helper widgets ─────────────────────────────────────────────────────────────

class _StatusBanner extends StatelessWidget {
  final String status;
  const _StatusBanner({required this.status});

  @override
  Widget build(BuildContext context) {
    Color color;
    IconData icon;
    switch (status.toLowerCase()) {
      case 'active':
        color = Colors.green; icon = Icons.play_circle_outline; break;
      case 'completed':
        color = Colors.blue;  icon = Icons.check_circle_outline; break;
      case 'on hold':
        color = Colors.orange; icon = Icons.pause_circle_outline; break;
      default:
        color = Colors.grey;  icon = Icons.circle_outlined;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withOpacity(0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: color, size: 18),
          const SizedBox(width: 6),
          Text(status,
              style: TextStyle(
                  color: color, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  final String title;
  final List<Widget> children;
  const _SectionCard({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title,
                style: Theme.of(context).textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.bold)),
            const Divider(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;
  const _DetailRow(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(label,
                style: const TextStyle(
                    color: Colors.grey, fontSize: 13)),
          ),
          Expanded(
            child: Text(value.isEmpty ? '—' : value,
                style: const TextStyle(fontSize: 13)),
          ),
        ],
      ),
    );
  }
}

class _EmptyExpenses extends StatelessWidget {
  const _EmptyExpenses();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: const Text('No expenses recorded for this project.',
          style: TextStyle(color: Colors.grey)),
    );
  }
}

class _ExpenseCard extends StatelessWidget {
  final Expense expense;
  const _ExpenseCard({required this.expense});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.secondaryContainer,
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(expense.expenseCode,
                      style: theme.textTheme.labelSmall?.copyWith(
                          fontWeight: FontWeight.bold)),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(expense.expenseType,
                      style: theme.textTheme.bodyMedium
                          ?.copyWith(fontWeight: FontWeight.w600)),
                ),
                Text(
                  '${expense.currency} ${expense.amount.toStringAsFixed(2)}',
                  style: theme.textTheme.titleSmall?.copyWith(
                      color: theme.colorScheme.primary,
                      fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Row(children: [
              _ExpenseChip(expense.paymentMethod),
              const SizedBox(width: 6),
              _ExpenseChip(expense.paymentStatus),
            ]),
            const SizedBox(height: 4),
            Text('Claimant: ${expense.claimant}  ·  Date: ${expense.expenseDate}',
                style: theme.textTheme.labelSmall
                    ?.copyWith(color: Colors.grey[600])),
            if (expense.description.isNotEmpty) ...[
              const SizedBox(height: 4),
              Text(expense.description,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: Colors.grey[600])),
            ],
          ],
        ),
      ),
    );
  }
}

class _ExpenseChip extends StatelessWidget {
  final String label;
  const _ExpenseChip(this.label);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Text(label,
          style: const TextStyle(fontSize: 11, color: Colors.black54)),
    );
  }
}
