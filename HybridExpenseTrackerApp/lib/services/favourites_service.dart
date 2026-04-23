import 'package:shared_preferences/shared_preferences.dart';
import '../constants/app_constants.dart';

/// Manages the user's favourite projects using local SharedPreferences storage.
/// Favourites are stored as a set of project codes.
class FavouritesService {
  FavouritesService._();
  static final FavouritesService instance = FavouritesService._();

  // ── Read ───────────────────────────────────────────────────────────────────

  /// Returns the set of favourited project codes.
  Future<Set<String>> getFavouriteCodes() async {
    final prefs = await SharedPreferences.getInstance();
    final list  = prefs.getStringList(AppConstants.prefFavourites) ?? [];
    return list.toSet();
  }

  /// Returns true if the project with [projectCode] is favourited.
  Future<bool> isFavourite(String projectCode) async {
    final favs = await getFavouriteCodes();
    return favs.contains(projectCode);
  }

  // ── Write ──────────────────────────────────────────────────────────────────

  /// Adds [projectCode] to favourites. Returns the updated set.
  Future<Set<String>> addFavourite(String projectCode) async {
    final prefs = await SharedPreferences.getInstance();
    final favs  = await getFavouriteCodes();
    favs.add(projectCode);
    await prefs.setStringList(AppConstants.prefFavourites, favs.toList());
    return favs;
  }

  /// Removes [projectCode] from favourites. Returns the updated set.
  Future<Set<String>> removeFavourite(String projectCode) async {
    final prefs = await SharedPreferences.getInstance();
    final favs  = await getFavouriteCodes();
    favs.remove(projectCode);
    await prefs.setStringList(AppConstants.prefFavourites, favs.toList());
    return favs;
  }

  /// Toggles the favourite state of [projectCode].
  /// Returns true if the project is now a favourite, false otherwise.
  Future<bool> toggleFavourite(String projectCode) async {
    final isFav = await isFavourite(projectCode);
    if (isFav) {
      await removeFavourite(projectCode);
      return false;
    } else {
      await addFavourite(projectCode);
      return true;
    }
  }
}
