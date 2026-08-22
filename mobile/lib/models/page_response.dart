/// Wrapper générique pour une page Spring Data (`Page<T>`), utilisé par
/// GET /api/transactions/wallet/{walletId}.
class PageResponse<T> {
  final List<T> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final int size;
  final bool last;

  const PageResponse({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.last,
  });

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) itemParser,
  ) {
    final rawContent = json['content'] as List<dynamic>? ?? [];
    return PageResponse<T>(
      content: rawContent
          .map((e) => itemParser(e as Map<String, dynamic>))
          .toList(),
      totalElements: json['totalElements'] as int? ?? 0,
      totalPages: json['totalPages'] as int? ?? 0,
      number: json['number'] as int? ?? 0,
      size: json['size'] as int? ?? 0,
      last: json['last'] as bool? ?? true,
    );
  }
}
