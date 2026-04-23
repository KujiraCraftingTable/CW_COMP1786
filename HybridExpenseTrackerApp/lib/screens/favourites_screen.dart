import 'dart:async';
import 'package:flutter/material.dart';
import '../models/project.dart';
import '../services/api_service.dart';
import '../services/favourites_service.dart';
import '../widgets/project_card.dart';
import 'project_detail_screen.dart';

/// Displays only the projects that the user has marked as favourites.
/// Feature h – Favourite Projects (5%)
class FavouritesScreen extends StatefulWidget {
  const FavouritesScreen({super.key});

  @override
  State<FavouritesScreen> createState() => _FavouritesScreenState();
}

class _FavouritesScreenState extends State<FavouritesScreen> {
  List<Project> _favouriteProjects = [];
  Set<String>   _favouriteCodes    = {};
  bool          _loading           = true;
  String?       _error;

  Timer? _autoRefreshTimer;

  final _apiService = ApiService.instance;
  final _favService = FavouritesService.instance;

  @override
  void initState() {
    super.initState();
    _loadFavourites();
  }

  @override
  void dispose() {
    _autoRefreshTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadFavourites() async {
    setState(() { _loading = true; _error = null; });
    try {
      final results = await Future.wait([
        _apiService.fetchProjects(),
        _favService.getFavouriteCodes(),
      ]);

      final allProjects = results[0] as List<Project>;
      final codes       = results[1] as Set<String>;

      final favProjects = allProjects
          .where((p) => codes.contains(p.projectCode))
          .toList();

      if (mounted) {
        setState(() {
          _favouriteProjects = favProjects;
          _favouriteCodes    = codes;
          _loading           = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() { _error = e.toString(); _loading = false; });
      }
    }
  }

  Future<void> _removeFavourite(Project project) async {
    await _favService.removeFavourite(project.projectCode);
    if (mounted) {
      setState(() {
        _favouriteCodes.remove(project.projectCode);
        _favouriteProjects
            .removeWhere((p) => p.projectCode == project.projectCode);
      });
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('${project.projectName} removed from Favourites'),
        duration: const Duration(seconds: 2),
      ));
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Favourites'),
        backgroundColor: theme.colorScheme.primaryContainer,
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: _loading ? null : _loadFavourites,
          ),
        ],
      ),
      body: _buildBody(theme),
    );
  }

  Widget _buildBody(ThemeData theme) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off, size: 64, color: Colors.grey),
            const SizedBox(height: 12),
            Text('Could not load data', style: theme.textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(_error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _loadFavourites,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      );
    }

    if (_favouriteProjects.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.star_border, size: 64, color: Colors.grey),
            const SizedBox(height: 12),
            const Text('No favourite projects yet.',
                style: TextStyle(color: Colors.grey)),
            const SizedBox(height: 8),
            const Text(
              'Tap the ★ icon on any project to add it here.',
              style: TextStyle(color: Colors.grey, fontSize: 13),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadFavourites,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: _favouriteProjects.length,
        itemBuilder: (context, index) {
          final project = _favouriteProjects[index];
          return ProjectCard(
            project: project,
            isFavourite: true,
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(
                  builder: (_) => ProjectDetailScreen(project: project)),
            ).then((_) => _loadFavourites()),
            onFavouriteToggle: () => _removeFavourite(project),
          );
        },
      ),
    );
  }
}
