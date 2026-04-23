import 'package:flutter/material.dart';
import '../models/project.dart';
import '../services/favourites_service.dart';
import '../widgets/project_card.dart';
import 'project_detail_screen.dart';

/// Allows the user to search projects by name or date.
/// Receives the full project list so it works offline once data is loaded.
class SearchScreen extends StatefulWidget {
  final List<Project> allProjects;

  const SearchScreen({super.key, required this.allProjects});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final _nameController = TextEditingController();
  final _dateController = TextEditingController();

  List<Project> _results   = [];
  Set<String>   _favourites = {};
  bool _hasSearched = false;

  final _favService = FavouritesService.instance;

  @override
  void initState() {
    super.initState();
    _loadFavourites();
  }

  Future<void> _loadFavourites() async {
    final favs = await _favService.getFavouriteCodes();
    if (mounted) setState(() => _favourites = favs);
  }

  void _runSearch() {
    final nameQuery = _nameController.text.trim().toLowerCase();
    final dateQuery = _dateController.text.trim();

    final results = widget.allProjects.where((p) {
      // Name filter: project name OR code starts with / contains query
      final nameMatch = nameQuery.isEmpty ||
          p.projectName.toLowerCase().contains(nameQuery) ||
          p.projectCode.toLowerCase().contains(nameQuery);

      // Date filter: start date or end date contains the typed string
      final dateMatch = dateQuery.isEmpty ||
          p.startDate.contains(dateQuery) ||
          p.endDate.contains(dateQuery);

      return nameMatch && dateMatch;
    }).toList();

    setState(() {
      _results    = results;
      _hasSearched = true;
    });
  }

  void _clearSearch() {
    _nameController.clear();
    _dateController.clear();
    setState(() { _results = []; _hasSearched = false; });
  }

  Future<void> _toggleFavourite(Project project) async {
    final nowFav = await _favService.toggleFavourite(project.projectCode);
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

  @override
  void dispose() {
    _nameController.dispose();
    _dateController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Search Projects'),
        backgroundColor: theme.colorScheme.primaryContainer,
      ),
      body: Column(
        children: [
          // ── Search filters ───────────────────────────────────────────────
          Container(
            color: theme.colorScheme.surfaceContainerHighest.withOpacity(0.3),
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: Column(
              children: [
                // Name / code search
                TextField(
                  controller: _nameController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Search by name or code',
                    prefixIcon: Icon(Icons.search),
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                  onChanged: (_) => _runSearch(),
                ),
                const SizedBox(height: 10),
                // Date filter
                TextField(
                  controller: _dateController,
                  keyboardType: TextInputType.datetime,
                  textInputAction: TextInputAction.search,
                  decoration: const InputDecoration(
                    labelText: 'Filter by date (e.g. 2024 or 2024-06)',
                    prefixIcon: Icon(Icons.calendar_month_outlined),
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                  onChanged: (_) => _runSearch(),
                  onSubmitted: (_) => _runSearch(),
                ),
                const SizedBox(height: 10),
                // Buttons row
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton.icon(
                        onPressed: _runSearch,
                        icon: const Icon(Icons.search, size: 18),
                        label: const Text('Search'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    OutlinedButton.icon(
                      onPressed: _clearSearch,
                      icon: const Icon(Icons.clear, size: 18),
                      label: const Text('Clear'),
                    ),
                  ],
                ),
              ],
            ),
          ),

          // ── Result count ─────────────────────────────────────────────────
          if (_hasSearched)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Row(
                children: [
                  Text('${_results.length} result(s) found',
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: Colors.grey[600])),
                ],
              ),
            ),

          // ── Results list ─────────────────────────────────────────────────
          Expanded(child: _buildResults(theme)),
        ],
      ),
    );
  }

  Widget _buildResults(ThemeData theme) {
    if (!_hasSearched) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.manage_search, size: 64, color: Colors.grey),
            SizedBox(height: 12),
            Text('Enter a name or date to search projects.',
                style: TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }

    if (_results.isEmpty) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.search_off, size: 64, color: Colors.grey),
            SizedBox(height: 12),
            Text('No projects matched your search.',
                style: TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(vertical: 8),
      itemCount: _results.length,
      itemBuilder: (context, index) {
        final project = _results[index];
        return ProjectCard(
          project: project,
          isFavourite: _favourites.contains(project.projectCode),
          onTap: () => Navigator.push(
            context,
            MaterialPageRoute(
                builder: (_) => ProjectDetailScreen(project: project)),
          ).then((_) => _loadFavourites()),
          onFavouriteToggle: () => _toggleFavourite(project),
        );
      },
    );
  }
}
