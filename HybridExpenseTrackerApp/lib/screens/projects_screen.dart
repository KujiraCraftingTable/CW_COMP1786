import 'dart:async';
import 'package:flutter/material.dart';
import '../models/project.dart';
import '../services/api_service.dart';
import '../services/favourites_service.dart';
import '../widgets/project_card.dart';
import 'project_detail_screen.dart';
import 'search_screen.dart';
import 'settings_screen.dart';

// Thin wrapper so we can push SettingsScreen from the error state
class _SettingsPage extends StatelessWidget {
  const _SettingsPage();
  @override
  Widget build(BuildContext context) => const SettingsScreen();
}

/// Fetches all projects from the cloud service and displays them as a list.
/// Supports pull-to-refresh and navigation to project details.
class ProjectsScreen extends StatefulWidget {
  const ProjectsScreen({super.key});

  @override
  State<ProjectsScreen> createState() => _ProjectsScreenState();
}

class _ProjectsScreenState extends State<ProjectsScreen> {
  List<Project> _projects    = [];
  Set<String>   _favourites  = {};
  bool          _loading     = false;
  String?       _error;
  Timer?        _autoRefreshTimer;

  final _apiService  = ApiService.instance;
  final _favService  = FavouritesService.instance;

  @override
  void initState() {
    super.initState();
    _loadData();
    // Auto-refresh every 10 seconds so changes from admin/other users appear automatically
    _autoRefreshTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      if (!_loading) _loadData();
    });
  }

  @override
  void dispose() {
    _autoRefreshTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() { _loading = true; _error = null; });
    try {
      final results = await Future.wait([
        _apiService.fetchProjects(),
        _favService.getFavouriteCodes(),
      ]);
      if (mounted) {
        setState(() {
          _projects   = results[0] as List<Project>;
          _favourites = results[1] as Set<String>;
          _loading    = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error   = e.toString();
          _loading = false;
        });
      }
    }
  }

  Future<void> _toggleFavourite(Project project) async {
    final nowFav =
        await _favService.toggleFavourite(project.projectCode);
    if (mounted) {
      setState(() {
        if (nowFav) {
          _favourites.add(project.projectCode);
        } else {
          _favourites.remove(project.projectCode);
        }
      });
    }
  }

  void _openDetail(Project project) {
    Navigator.push(
      context,
      MaterialPageRoute(
          builder: (_) => ProjectDetailScreen(project: project)),
    ).then((_) => _reloadFavourites());
  }

  Future<void> _reloadFavourites() async {
    final favs = await _favService.getFavouriteCodes();
    if (mounted) setState(() => _favourites = favs);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Projects'),
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
        actions: [
          IconButton(
            tooltip: 'Search',
            icon: const Icon(Icons.search),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(
                  builder: (_) => SearchScreen(allProjects: _projects)),
            ).then((_) => _reloadFavourites()),
          ),
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: _loading ? null : _loadData,
          ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    // Loading state
    if (_loading) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 12),
            Text('Loading projects from server…'),
          ],
        ),
      );
    }

    // Error state
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off, size: 64, color: Colors.grey),
              const SizedBox(height: 12),
              Text('Could not load projects',
                  style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              Text(_error!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.grey)),
              const SizedBox(height: 20),
              ElevatedButton.icon(
                onPressed: _loadData,
                icon: const Icon(Icons.refresh),
                label: const Text('Try Again'),
              ),
              const SizedBox(height: 8),
              TextButton(
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const _SettingsPage()),
                ),
                child: const Text('Check server settings'),
              ),
            ],
          ),
        ),
      );
    }

    // Empty state
    if (_projects.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.folder_open, size: 64, color: Colors.grey),
            const SizedBox(height: 12),
            const Text('No projects found on the server.'),
            const SizedBox(height: 8),
            TextButton.icon(
              onPressed: _loadData,
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
          ],
        ),
      );
    }

    // Project list with pull-to-refresh
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: _projects.length,
        itemBuilder: (context, index) {
          final project = _projects[index];
          return ProjectCard(
            project: project,
            isFavourite: _favourites.contains(project.projectCode),
            onTap: () => _openDetail(project),
            onFavouriteToggle: () => _toggleFavourite(project),
          );
        },
      ),
    );
  }
}
