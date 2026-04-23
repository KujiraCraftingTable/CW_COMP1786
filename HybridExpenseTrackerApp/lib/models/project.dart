import 'expense.dart';

/// Project model – mirrors the JSON payload produced by the Android Admin app.
class Project {
  final int localId;
  final String projectCode;
  final String projectName;
  final String description;
  final String startDate;
  final String endDate;
  final String manager;
  final String status;
  final double budget;
  final String priority;
  final String specialRequirements;
  final String clientDepartment;
  final String notes;
  final String createdAt;
  final List<Expense> expenses;

  const Project({
    required this.localId,
    required this.projectCode,
    required this.projectName,
    required this.description,
    required this.startDate,
    required this.endDate,
    required this.manager,
    required this.status,
    required this.budget,
    this.priority = '',
    this.specialRequirements = '',
    this.clientDepartment = '',
    this.notes = '',
    this.createdAt = '',
    this.expenses = const [],
  });

  factory Project.fromJson(Map<String, dynamic> json) {
    final rawExpenses = json['expenses'] as List<dynamic>? ?? [];
    return Project(
      localId:              json['localId']              as int?    ?? 0,
      projectCode:          json['projectCode']          as String? ?? '',
      projectName:          json['projectName']          as String? ?? '',
      description:          json['description']          as String? ?? '',
      startDate:            json['startDate']            as String? ?? '',
      endDate:              json['endDate']              as String? ?? '',
      manager:              json['manager']              as String? ?? '',
      status:               json['status']               as String? ?? '',
      budget:               (json['budget'] as num?)?.toDouble() ?? 0.0,
      priority:             json['priority']             as String? ?? '',
      specialRequirements:  json['specialRequirements']  as String? ?? '',
      clientDepartment:     json['clientDepartment']     as String? ?? '',
      notes:                json['notes']                as String? ?? '',
      createdAt:            json['createdAt']            as String? ?? '',
      expenses: rawExpenses
          .map((e) => Expense.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// Total amount of all expenses for this project.
  double get totalExpenses =>
      expenses.fold(0.0, (sum, e) => sum + e.amount);

  /// Returns true if the project started after [date] or ends before [date].
  bool matchesDateFilter(String date) {
    if (date.isEmpty) return true;
    return startDate.contains(date) || endDate.contains(date);
  }
}
