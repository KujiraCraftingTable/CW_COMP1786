import 'package:flutter/material.dart';
import '../services/api_service.dart';
import '../constants/app_constants.dart';

/// Settings screen – lets the user configure the cloud server URL.
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _urlController = TextEditingController();
  bool _testing  = false;
  String? _testResult;
  bool   _testOk = false;

  @override
  void initState() {
    super.initState();
    _loadSavedUrl();
  }

  Future<void> _loadSavedUrl() async {
    final url = await ApiService.instance.getServerUrl();
    if (mounted) _urlController.text = url;
  }

  Future<void> _saveUrl() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      _showSnack('Please enter a server URL', isError: true);
      return;
    }
    await ApiService.instance.saveServerUrl(url);
    if (mounted) _showSnack('Server URL saved!');
  }

  Future<void> _testConnection() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      _showSnack('Please enter a server URL first', isError: true);
      return;
    }
    setState(() { _testing = true; _testResult = null; });

    final ok = await ApiService.instance.testConnection(url);
    if (mounted) {
      setState(() {
        _testing    = false;
        _testOk     = ok;
        _testResult = ok
            ? 'Connection successful!'
            : 'Could not reach server. Check the URL and try again.';
      });
    }
  }

  void _showSnack(String msg, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(msg),
      backgroundColor: isError ? Colors.red : Colors.green,
    ));
  }

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        backgroundColor: theme.colorScheme.primaryContainer,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ── Section header ─────────────────────────────────────────────
            Text('Cloud Server', style: theme.textTheme.titleMedium
                ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(
              'Enter the URL of the server used by the Android Admin app.',
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: Colors.grey[600]),
            ),
            const SizedBox(height: 16),

            // ── URL input ──────────────────────────────────────────────────
            TextField(
              controller: _urlController,
              keyboardType: TextInputType.url,
              decoration: InputDecoration(
                labelText: 'Server URL',
                hintText: AppConstants.defaultServerUrl,
                border: const OutlineInputBorder(),
                prefixIcon: const Icon(Icons.cloud_outlined),
                helperText: 'Example: http://192.168.1.100:3000',
              ),
            ),
            const SizedBox(height: 12),

            // ── Buttons ────────────────────────────────────────────────────
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _testing ? null : _testConnection,
                    icon: _testing
                        ? const SizedBox(
                            width: 16, height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.wifi_tethering),
                    label: Text(_testing ? 'Testing…' : 'Test Connection'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _saveUrl,
                    icon: const Icon(Icons.save_outlined),
                    label: const Text('Save URL'),
                  ),
                ),
              ],
            ),

            // ── Test result ────────────────────────────────────────────────
            if (_testResult != null) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: (_testOk ? Colors.green : Colors.red)
                      .withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: (_testOk ? Colors.green : Colors.red)
                        .withOpacity(0.4),
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      _testOk ? Icons.check_circle : Icons.error_outline,
                      color: _testOk ? Colors.green : Colors.red,
                    ),
                    const SizedBox(width: 8),
                    Expanded(child: Text(_testResult!)),
                  ],
                ),
              ),
            ],

            const SizedBox(height: 32),
            const Divider(),
            const SizedBox(height: 16),

            // ── Info ───────────────────────────────────────────────────────
            Text('About', style: theme.textTheme.titleMedium
                ?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            _InfoRow(label: 'App',     value: 'Expense Tracker (User App)'),
            _InfoRow(label: 'Version', value: '1.0.0'),
            _InfoRow(label: 'Course',  value: 'COMP1786'),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;
  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(
            width: 70,
            child: Text(label,
                style: const TextStyle(
                    fontWeight: FontWeight.w600, color: Colors.grey)),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}
