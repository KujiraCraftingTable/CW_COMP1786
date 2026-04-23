import 'package:flutter/material.dart';
import '../models/project.dart';
import '../services/api_service.dart';

/// Form screen that lets the user add a new expense to a project
/// and upload it to the cloud server.
class AddExpenseScreen extends StatefulWidget {
  final Project project;

  const AddExpenseScreen({super.key, required this.project});

  @override
  State<AddExpenseScreen> createState() => _AddExpenseScreenState();
}

class _AddExpenseScreenState extends State<AddExpenseScreen> {
  final _formKey = GlobalKey<FormState>();
  bool _saving   = false;

  // ── Controllers ────────────────────────────────────────────────────────────
  final _codeCtrl        = TextEditingController();
  final _dateCtrl        = TextEditingController();
  final _amountCtrl      = TextEditingController();
  final _claimantCtrl    = TextEditingController();
  final _descCtrl        = TextEditingController();
  final _locationCtrl    = TextEditingController();
  final _receiptCtrl     = TextEditingController();

  // ── Dropdown values ────────────────────────────────────────────────────────
  String? _currency;
  String? _expenseType;
  String? _paymentMethod;
  String? _paymentStatus;

  static const _currencies = [
    'USD', 'EUR', 'GBP', 'AUD', 'CAD', 'JPY', 'CNY', 'SGD', 'OTHER'
  ];
  static const _expenseTypes = [
    'Travel', 'Equipment', 'Materials', 'Services',
    'Software/Licenses', 'Labour Costs', 'Utilities', 'Miscellaneous',
  ];
  static const _paymentMethods = [
    'Cash', 'Credit Card', 'Bank Transfer', 'Cheque',
  ];
  static const _paymentStatuses = [
    'Paid', 'Pending', 'Reimbursed',
  ];

  @override
  void dispose() {
    _codeCtrl.dispose();
    _dateCtrl.dispose();
    _amountCtrl.dispose();
    _claimantCtrl.dispose();
    _descCtrl.dispose();
    _locationCtrl.dispose();
    _receiptCtrl.dispose();
    super.dispose();
  }

  // ── Date picker ────────────────────────────────────────────────────────────
  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (picked != null && mounted) {
      _dateCtrl.text =
          '${picked.year.toString().padLeft(4, '0')}-'
          '${picked.month.toString().padLeft(2, '0')}-'
          '${picked.day.toString().padLeft(2, '0')}';
    }
  }

  // ── Submit ─────────────────────────────────────────────────────────────────
  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _saving = true);

    final expenseJson = {
      'expenseCode':   _codeCtrl.text.trim(),
      'expenseDate':   _dateCtrl.text.trim(),
      'amount':        double.parse(_amountCtrl.text.trim()),
      'currency':      _currency!,
      'expenseType':   _expenseType!,
      'paymentMethod': _paymentMethod!,
      'claimant':      _claimantCtrl.text.trim(),
      'paymentStatus': _paymentStatus!,
      'description':   _descCtrl.text.trim(),
      'location':      _locationCtrl.text.trim(),
      'receiptNumber': _receiptCtrl.text.trim(),
    };

    try {
      await ApiService.instance.addExpense(
        projectCode:  widget.project.projectCode,
        expenseJson:  expenseJson,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Expense added successfully!'),
            backgroundColor: Colors.green,
          ),
        );
        Navigator.pop(context, true); // return true = data changed
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to add expense: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  // ── Build ──────────────────────────────────────────────────────────────────
  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text('Add Expense · ${widget.project.projectCode}',
            overflow: TextOverflow.ellipsis),
        backgroundColor: theme.colorScheme.primaryContainer,
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ── Required fields notice ─────────────────────────────────
              Text('* Required fields',
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: Colors.grey[600])),
              const SizedBox(height: 12),

              // ── Expense Code ───────────────────────────────────────────
              _buildTextField(
                controller: _codeCtrl,
                label: 'Expense Code *',
                hint: 'e.g. EXP-001',
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Required' : null,
              ),
              _gap,

              // ── Date ───────────────────────────────────────────────────
              TextFormField(
                controller: _dateCtrl,
                readOnly: true,
                onTap: _pickDate,
                decoration: InputDecoration(
                  labelText: 'Date of Expense *',
                  border: const OutlineInputBorder(),
                  suffixIcon: const Icon(Icons.calendar_today),
                  hintText: 'Select date',
                ),
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Required' : null,
              ),
              _gap,

              // ── Amount ─────────────────────────────────────────────────
              _buildTextField(
                controller: _amountCtrl,
                label: 'Amount *',
                hint: '0.00',
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                validator: (v) {
                  if (v == null || v.trim().isEmpty) return 'Required';
                  if (double.tryParse(v.trim()) == null) {
                    return 'Enter a valid number';
                  }
                  return null;
                },
              ),
              _gap,

              // ── Currency ───────────────────────────────────────────────
              _buildDropdown(
                label: 'Currency *',
                value: _currency,
                items: _currencies,
                onChanged: (v) => setState(() => _currency = v),
                validator: (v) => v == null ? 'Required' : null,
              ),
              _gap,

              // ── Expense Type ───────────────────────────────────────────
              _buildDropdown(
                label: 'Type of Expense *',
                value: _expenseType,
                items: _expenseTypes,
                onChanged: (v) => setState(() => _expenseType = v),
                validator: (v) => v == null ? 'Required' : null,
              ),
              _gap,

              // ── Payment Method ─────────────────────────────────────────
              _buildDropdown(
                label: 'Payment Method *',
                value: _paymentMethod,
                items: _paymentMethods,
                onChanged: (v) => setState(() => _paymentMethod = v),
                validator: (v) => v == null ? 'Required' : null,
              ),
              _gap,

              // ── Claimant ───────────────────────────────────────────────
              _buildTextField(
                controller: _claimantCtrl,
                label: 'Claimant *',
                hint: 'Person submitting the claim',
                validator: (v) =>
                    (v == null || v.trim().isEmpty) ? 'Required' : null,
              ),
              _gap,

              // ── Payment Status ─────────────────────────────────────────
              _buildDropdown(
                label: 'Payment Status *',
                value: _paymentStatus,
                items: _paymentStatuses,
                onChanged: (v) => setState(() => _paymentStatus = v),
                validator: (v) => v == null ? 'Required' : null,
              ),
              _gap,

              const Divider(),
              Text('Optional',
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: Colors.grey)),
              _gap,

              // ── Description ────────────────────────────────────────────
              _buildTextField(
                controller: _descCtrl,
                label: 'Description',
                maxLines: 2,
              ),
              _gap,

              // ── Location ───────────────────────────────────────────────
              _buildTextField(
                controller: _locationCtrl,
                label: 'Location',
              ),
              _gap,

              // ── Receipt Number ─────────────────────────────────────────
              _buildTextField(
                controller: _receiptCtrl,
                label: 'Receipt Number',
              ),
              const SizedBox(height: 24),

              // ── Submit button ──────────────────────────────────────────
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton.icon(
                  onPressed: _saving ? null : _submit,
                  icon: _saving
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.cloud_upload_outlined),
                  label: Text(_saving ? 'Saving…' : 'Save & Upload Expense'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: theme.colorScheme.primary,
                    foregroundColor: theme.colorScheme.onPrimary,
                  ),
                ),
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  // ── Helper builders ────────────────────────────────────────────────────────

  static const _gap = SizedBox(height: 14);

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    String? hint,
    int maxLines = 1,
    TextInputType? keyboardType,
    String? Function(String?)? validator,
  }) {
    return TextFormField(
      controller: controller,
      maxLines: maxLines,
      keyboardType: keyboardType,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        border: const OutlineInputBorder(),
      ),
      validator: validator,
    );
  }

  Widget _buildDropdown({
    required String label,
    required String? value,
    required List<String> items,
    required void Function(String?) onChanged,
    String? Function(String?)? validator,
  }) {
    return DropdownButtonFormField<String>(
      value: value,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
      items: items
          .map((e) => DropdownMenuItem(value: e, child: Text(e)))
          .toList(),
      onChanged: onChanged,
      validator: validator,
    );
  }
}
