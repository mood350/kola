import '../models/assistant.dart';
import '../models/json.dart';
import 'api_client.dart';

class AssistantService {
  AssistantService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<ApiResult<List<AssistantConversation>>> conversations() {
    return _client.get(
      '/api/v1/assistant/conversations',
      decode: (json) => asList(json, AssistantConversation.fromJson),
    );
  }

  Future<ApiResult<AssistantConversation>> conversation(String id) {
    return _client.get(
      '/api/v1/assistant/conversations/$id',
      decode: (json) =>
          AssistantConversation.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<AssistantReply>> ask(
    String message, [
    String? conversationId,
  ]) {
    return _client.post(
      '/api/v1/assistant/messages',
      body: {'conversationId': conversationId, 'message': message},
      decode: (json) =>
          AssistantReply.fromJson((json as Map).cast<String, dynamic>()),
    );
  }

  Future<ApiResult<void>> remove(String id) {
    return _client.deleteEmpty('/api/v1/assistant/conversations/$id');
  }
}
