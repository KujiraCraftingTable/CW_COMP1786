import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../constants/app_constants.dart';
import '../models/project.dart';
import '../models/expense.dart';

/// Handles all HTTP communication with the cloud service.
///
/// The server is the same endpoint configured in the Android Admin app.
/// Expected GET /projects response format:
/// {
///   "projects": [ { "localId": 1, "projectCode": "PRJ-001", ... } ]
/// }
/// or the full upload payload format (for compatibility):
/// {
///   "totalProjects": 3,
///   "projects": [ ... ]
/// }
class ApiService {
  ApiService._();
  static final ApiService instance = ApiService._();

  // ── Server URL ─────────────────────────────────────────────────────────────

  Future<String> getServerUrl() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(AppConstants.prefServerUrl) ??
        AppConstants.defaultServerUrl;
  }

  Future<void> saveServerUrl(String url) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(AppConstants.prefServerUrl, url.trim());
  }

  // ── Fetch projects ─────────────────────────────────────────────────────────

  /// Fetches all projects from the cloud service.
  /// Throws an [Exception] if the request fails.
  Future<List<Project>> fetchProjects() async {
    final base = await getServerUrl();
    final url  = Uri.parse(
        '${base.trimRight()}/'.replaceAll(RegExp(r'/+$'), '') +
        AppConstants.pathProjects);

    final response = await http
        .get(url, headers: {'Accept': 'application/json'})
        .timeout(AppConstants.connectTimeout);

    if (response.statusCode == 200) {
      return _parseProjects(response.body);
    } else {
      throw Exception('Server returned HTTP ${response.statusCode}');
    }
  }

  List<Project> _parseProjects(String body) {
    final json = jsonDecode(body);

    // Support both { "projects": [...] } and [ ... ] (bare array)
    List<dynamic> rawList;
    if (json is Map && json.containsKey('projects')) {
      rawList = json['projects'] as List<dynamic>;
    } else if (json is List) {
      rawList = json;
    } else {
      throw Exception('Unexpected response format from server');
    }

    return rawList
        .map((e) => Project.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // ── Add expense ────────────────────────────────────────────────────────────

  /// Posts a new [expense] to [projectCode] on the cloud service.
  /// Returns the saved Expense (with server-assigned localId).
  /// Throws an [Exception] if the request fails.
  Future<Expense> addExpense({
    required String projectCode,
    required Map<String, dynamic> expenseJson,
  }) async {
    final base = await getServerUrl();
    final url  = Uri.parse(
        base.trimRight().replaceAll(RegExp(r'/+$'), '') + '/expenses');

    final response = await http
        .post(
          url,
          headers: {
            'Content-Type': 'application/json',
            'Accept':       'application/json',
          },
          body: jsonEncode({
            'projectCode': projectCode,
            'expense':     expenseJson,
          }),
        )
        .timeout(AppConstants.connectTimeout);

    if (response.statusCode == 200 || response.statusCode == 201) {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      return Expense.fromJson(body['expense'] as Map<String, dynamic>);
    } else {
      final body = jsonDecode(response.body);
      throw Exception(body['error'] ?? 'Server error ${response.statusCode}');
    }
  }

  // ── Delete expense ─────────────────────────────────────────────────────────

  /// Deletes an expense from the server.
  Future<void> deleteExpense({
    required String projectCode,
    required String expenseCode,
  }) async {
    final base = await getServerUrl();
    final url  = Uri.parse(
        '${base.trimRight().replaceAll(RegExp(r'/+$'), '')}'
        '/expenses/$projectCode/$expenseCode');

    final response = await http
        .delete(url, headers: {'Accept': 'application/json'})
        .timeout(AppConstants.connectTimeout);

    if (response.statusCode != 200) {
      throw Exception('Could not delete expense (HTTP ${response.statusCode})');
    }
  }

  // ── Test connection ────────────────────────────────────────────────────────

  /// Returns true if the server is reachable.
  Future<bool> testConnection(String url) async {
    try {
      final uri = Uri.parse(
          url.trimRight().replaceAll(RegExp(r'/+$'), '') +
          AppConstants.pathHealth);
      final response = await http
          .get(uri)
          .timeout(AppConstants.connectTimeout);
      return response.statusCode > 0;
    } catch (_) {
      return false;
    }
  }
}
