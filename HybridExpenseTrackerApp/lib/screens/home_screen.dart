import 'package:flutter/material.dart';
import 'projects_screen.dart';
import 'favourites_screen.dart';
import 'settings_screen.dart';

/// Root screen with a bottom navigation bar containing three tabs:
/// Projects, Favourites, and Settings.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  static const List<Widget> _screens = [
    ProjectsScreen(),
    FavouritesScreen(),
    SettingsScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_currentIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) =>
            setState(() => _currentIndex = index),
        destinations: const [
          NavigationDestination(
            icon:          Icon(Icons.folder_outlined),
            selectedIcon:  Icon(Icons.folder),
            label:         'Projects',
          ),
          NavigationDestination(
            icon:          Icon(Icons.star_border),
            selectedIcon:  Icon(Icons.star),
            label:         'Favourites',
          ),
          NavigationDestination(
            icon:          Icon(Icons.settings_outlined),
            selectedIcon:  Icon(Icons.settings),
            label:         'Settings',
          ),
        ],
      ),
    );
  }
}
