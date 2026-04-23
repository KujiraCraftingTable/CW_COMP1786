/// Application-wide constants.
/// The server URL is the same endpoint used by the Android Admin app.
class AppConstants {
  AppConstants._();

  // ── SharedPreferences keys ─────────────────────────────────────────────────
  static const String prefServerUrl    = 'server_url';
  static const String prefFavourites   = 'favourite_project_codes';

  // ── Default server URL (same as Android app placeholder) ──────────────────
  static const String defaultServerUrl = 'http://10.0.2.2:3000';

  // ── API paths ──────────────────────────────────────────────────────────────
  /// GET  {serverUrl}/projects  → list of all projects with expenses
  static const String pathProjects     = '/projects';
  /// GET  {serverUrl}/          → health / reachability check
  static const String pathHealth       = '/';

  // ── Timeouts ───────────────────────────────────────────────────────────────
  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration readTimeout    = Duration(seconds: 15);
}
