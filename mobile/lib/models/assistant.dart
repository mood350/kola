import 'json.dart';

/// Message d'une conversation avec l'assistant KOLA.
class AssistantMessage {
  const AssistantMessage({
    required this.id,
    required this.role,
    required this.content,
    required this.createdAt,
  });

  final String id;
  final String role;
  final String content;
  final DateTime createdAt;

  bool get fromUser => role == 'USER';

  factory AssistantMessage.fromJson(Map<String, dynamic> json) {
    return AssistantMessage(
      id: asString(json['id']),
      role: asString(json['role'], 'ASSISTANT'),
      content: asString(json['content']),
      createdAt: asDate(json['createdAt']) ?? DateTime.now(),
    );
  }
}

/// Conversation listée dans l'historique de l'assistant.
class AssistantConversation {
  const AssistantConversation({
    required this.id,
    required this.title,
    required this.lastMessageAt,
    this.messages = const [],
  });

  final String id;
  final String title;
  final DateTime? lastMessageAt;
  final List<AssistantMessage> messages;

  factory AssistantConversation.fromJson(Map<String, dynamic> json) {
    return AssistantConversation(
      id: asString(json['id']),
      title: asString(json['title'], 'Conversation'),
      lastMessageAt: asDate(json['lastMessageAt']),
      messages: asList(json['messages'], AssistantMessage.fromJson),
    );
  }
}

/// Réponse de l'assistant à une question, avec le quota restant du jour.
class AssistantReply {
  const AssistantReply({
    required this.conversationId,
    required this.title,
    required this.answer,
    required this.remainingToday,
  });

  final String conversationId;
  final String title;
  final AssistantMessage answer;
  final int remainingToday;

  factory AssistantReply.fromJson(Map<String, dynamic> json) {
    return AssistantReply(
      conversationId: asString(json['conversationId']),
      title: asString(json['title']),
      answer: AssistantMessage.fromJson(
        (json['answer'] as Map?)?.cast<String, dynamic>() ?? const {},
      ),
      remainingToday: asInt(json['remainingToday']),
    );
  }
}
