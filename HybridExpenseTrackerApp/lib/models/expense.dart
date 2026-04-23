/// Expense model – mirrors the JSON payload produced by the Android Admin app.
class Expense {
  final int localId;
  final String expenseCode;
  final String expenseDate;
  final double amount;
  final String currency;
  final String expenseType;
  final String paymentMethod;
  final String claimant;
  final String paymentStatus;
  final String description;
  final String location;
  final String receiptNumber;
  final String createdAt;

  const Expense({
    required this.localId,
    required this.expenseCode,
    required this.expenseDate,
    required this.amount,
    required this.currency,
    required this.expenseType,
    required this.paymentMethod,
    required this.claimant,
    required this.paymentStatus,
    this.description = '',
    this.location = '',
    this.receiptNumber = '',
    this.createdAt = '',
  });

  factory Expense.fromJson(Map<String, dynamic> json) {
    return Expense(
      localId:       json['localId']       as int?    ?? 0,
      expenseCode:   json['expenseCode']   as String? ?? '',
      expenseDate:   json['expenseDate']   as String? ?? '',
      amount:        (json['amount'] as num?)?.toDouble() ?? 0.0,
      currency:      json['currency']      as String? ?? '',
      expenseType:   json['expenseType']   as String? ?? '',
      paymentMethod: json['paymentMethod'] as String? ?? '',
      claimant:      json['claimant']      as String? ?? '',
      paymentStatus: json['paymentStatus'] as String? ?? '',
      description:   json['description']  as String? ?? '',
      location:      json['location']      as String? ?? '',
      receiptNumber: json['receiptNumber'] as String? ?? '',
      createdAt:     json['createdAt']     as String? ?? '',
    );
  }
}
