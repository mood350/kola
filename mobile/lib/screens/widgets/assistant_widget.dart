import 'package:flutter/material.dart';
import '../../core/theme/kola_icons.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../models/assistant.dart';
import '../../services/assistant_service.dart';

AssistantMessage _welcome() => AssistantMessage(
  id: 'welcome',
  role: 'ASSISTANT',
  content:
      'Bonjour ! Je suis l’assistant KOLA. Posez-moi une question sur votre compte, Bankivi, vos prêts ou l’utilisation de l’application.',
  createdAt: DateTime.now(),
);

/// Bouton flottant d'accès à l'assistant, posé au-dessus de la barre de
/// navigation, et panneau de conversation.
class AssistantWidget extends StatefulWidget {
  const AssistantWidget({super.key});

  @override
  State<AssistantWidget> createState() => _AssistantWidgetState();
}

class _AssistantWidgetState extends State<AssistantWidget> {
  final _assistant = AssistantService();

  bool _loading = true;
  bool _unavailable = false;

  @override
  void initState() {
    super.initState();
    _probe();
  }

  /// Vérifie une fois au démarrage que le service répond, pour colorer la
  /// pastille du bouton avant même que l'utilisateur ne l'ouvre.
  Future<void> _probe() async {
    final result = await _assistant.conversations();
    if (!mounted) return;
    setState(() {
      _loading = false;
      _unavailable = result.error?.statusCode == 503;
    });
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).padding.bottom;

    return Positioned(
      right: 19,
      bottom: (bottomInset > 10 ? bottomInset : 10) + 78,
      child: GestureDetector(
        onTap: _open,
        child: Container(
          width: 56,
          height: 56,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: AppColors.yellow,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: AppColors.yellow.withValues(alpha: 0.4),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              if (_loading)
                const SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(strokeWidth: 2.4),
                )
              else
                const Icon(
                  KolaIcons.sparkles,
                  size: 25,
                  color: AppColors.primary,
                ),
              Positioned(
                right: 2,
                bottom: 3,
                child: Container(
                  width: 13,
                  height: 13,
                  decoration: BoxDecoration(
                    color: _unavailable ? AppColors.red : AppColors.green,
                    shape: BoxShape.circle,
                    border: Border.all(color: AppColors.white, width: 2),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _open() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const _AssistantPanel(),
    );
  }
}

class _AssistantPanel extends StatefulWidget {
  const _AssistantPanel();

  @override
  State<_AssistantPanel> createState() => _AssistantPanelState();
}

class _AssistantPanelState extends State<_AssistantPanel> {
  final _assistant = AssistantService();
  final _input = TextEditingController();
  final _scroll = ScrollController();

  List<AssistantMessage> _messages = [_welcome()];
  String? _conversationId;
  int? _remaining;
  String _error = '';
  bool _sending = false;
  bool _unavailable = false;

  @override
  void initState() {
    super.initState();
    _restoreLastConversation();
  }

  @override
  void dispose() {
    _input.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _restoreLastConversation() async {
    final list = await _assistant.conversations();
    if (!mounted) return;
    if (list.error?.statusCode == 503) {
      setState(() {
        _unavailable = true;
        _error =
            'L’assistant KOLA est temporairement indisponible. Vous pourrez réessayer dans un instant.';
      });
      return;
    }

    final conversations = list.data ?? const <AssistantConversation>[];
    if (conversations.isEmpty) return;

    final detail = await _assistant.conversation(conversations.first.id);
    if (!mounted || !detail.success) return;
    setState(() {
      _conversationId = detail.data!.id;
      final messages = detail.data!.messages;
      if (messages.isNotEmpty) _messages = messages;
    });
    _scrollToEnd();
  }

  void _newConversation() {
    setState(() {
      _conversationId = null;
      _messages = [_welcome()];
      _input.clear();
      _error = '';
      _remaining = null;
    });
  }

  void _scrollToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scroll.hasClients) return;
      _scroll.animateTo(
        _scroll.position.maxScrollExtent,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
  }

  Future<void> _send() async {
    final question = _input.text.trim();
    if (question.isEmpty || _sending) return;

    // Le message part à l'écran immédiatement, et n'est retiré qu'en cas
    // d'échec : attendre la réponse du serveur donnerait l'impression que
    // l'envoi n'a pas été pris en compte.
    final optimistic = AssistantMessage(
      id: 'local-${DateTime.now().microsecondsSinceEpoch}',
      role: 'USER',
      content: question,
      createdAt: DateTime.now(),
    );
    setState(() {
      _messages = [..._messages, optimistic];
      _input.clear();
      _error = '';
      _unavailable = false;
      _sending = true;
    });
    _scrollToEnd();

    final reply = await _assistant.ask(question, _conversationId);
    if (!mounted) return;

    if (!reply.success) {
      final status = reply.error?.statusCode;
      setState(() {
        _sending = false;
        _messages = _messages.where((m) => m.id != optimistic.id).toList();
        _input.text = question;
        _unavailable = status == 503;
        _error = switch (status) {
          429 =>
            'Vous avez atteint votre quota de messages pour aujourd’hui.',
          503 =>
            'L’assistant KOLA est temporairement indisponible. Votre question est conservée, vous pouvez réessayer.',
          _ =>
            reply.error?.message ??
                'L’assistant ne répond pas pour le moment.',
        };
      });
      return;
    }

    setState(() {
      _sending = false;
      _conversationId = reply.data!.conversationId;
      _remaining = reply.data!.remainingToday;
      _messages = [..._messages, reply.data!.answer];
    });
    _scrollToEnd();
  }

  @override
  Widget build(BuildContext context) {
    final remaining = _remaining;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        height: MediaQuery.of(context).size.height * 0.82,
        decoration: const BoxDecoration(
          color: AppColors.bg,
          borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          children: [
            Container(
              height: 72,
              padding: const EdgeInsets.symmetric(horizontal: 17),
              decoration: const BoxDecoration(
                color: AppColors.white,
                border: Border(
                  bottom: BorderSide(color: AppColors.border),
                ),
              ),
              child: Row(
                children: [
                  Container(
                    width: 43,
                    height: 43,
                    alignment: Alignment.center,
                    decoration: const BoxDecoration(
                      color: AppColors.yellow,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      KolaIcons.sparkles,
                      size: 21,
                      color: AppColors.primary,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          'Assistant KOLA',
                          style: AppTypography.cardTitle.copyWith(
                            fontSize: 16,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          _unavailable
                              ? 'Temporairement indisponible'
                              : 'En ligne · Conseils personnalisés',
                          style: AppTypography.caption.copyWith(
                            fontSize: 9,
                            color: _unavailable
                                ? AppColors.red
                                : AppColors.greenDark,
                          ),
                        ),
                      ],
                    ),
                  ),
                  _HeaderButton(
                    icon: KolaIcons.add,
                    color: AppColors.primary,
                    onTap: _newConversation,
                  ),
                  const SizedBox(width: 6),
                  _HeaderButton(
                    icon: KolaIcons.close,
                    color: AppColors.ink,
                    onTap: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
            ),
            Expanded(
              child: ListView(
                controller: _scroll,
                padding: const EdgeInsets.all(16),
                children: [
                  for (final message in _messages)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _Bubble(message: message),
                    ),
                  if (_sending)
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 11,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.white,
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(color: AppColors.border),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const SizedBox(
                              width: 15,
                              height: 15,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Text(
                              'KOLA réfléchit…',
                              style: AppTypography.small,
                            ),
                          ],
                        ),
                      ),
                    ),
                  if (_error.isNotEmpty)
                    Container(
                      margin: const EdgeInsets.only(top: 3),
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFF0F0),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            KolaIcons.alertCircleOutline,
                            size: 17,
                            color: AppColors.red,
                          ),
                          const SizedBox(width: 7),
                          Expanded(
                            child: Text(
                              _error,
                              style: AppTypography.caption.copyWith(
                                color: AppColors.red,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
            if (remaining != null && remaining <= 5)
              Padding(
                padding: const EdgeInsets.only(bottom: 5),
                child: Text(
                  '$remaining message${remaining > 1 ? 's' : ''} restant${remaining > 1 ? 's' : ''} aujourd’hui',
                  style: AppTypography.caption.copyWith(
                    fontSize: 9,
                    color: AppColors.yellowDark,
                  ),
                ),
              ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14),
              child: Container(
                constraints: const BoxConstraints(
                  minHeight: 52,
                  maxHeight: 110,
                ),
                padding: const EdgeInsets.only(left: 16, right: 5),
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(26),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _input,
                        enabled: !_sending,
                        maxLines: null,
                        maxLength: 1000,
                        textInputAction: TextInputAction.send,
                        onSubmitted: (_) => _send(),
                        style: AppTypography.body.copyWith(fontSize: 13),
                        decoration: const InputDecoration(
                          hintText: 'Écrivez votre question…',
                          counterText: '',
                          border: InputBorder.none,
                          enabledBorder: InputBorder.none,
                          focusedBorder: InputBorder.none,
                          disabledBorder: InputBorder.none,
                          filled: false,
                          contentPadding: EdgeInsets.symmetric(vertical: 14),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(bottom: 5),
                      child: GestureDetector(
                        onTap: _send,
                        child: Opacity(
                          opacity: _sending ? 0.35 : 1,
                          child: Container(
                            width: 42,
                            height: 42,
                            alignment: Alignment.center,
                            decoration: const BoxDecoration(
                              color: AppColors.primary,
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(
                              KolaIcons.arrowUp,
                              size: 20,
                              color: AppColors.white,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.md,
                7,
                AppSpacing.md,
                12,
              ),
              child: Text(
                'Ne partagez jamais votre code PIN ou votre OTP.',
                style: AppTypography.caption.copyWith(fontSize: 8),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HeaderButton extends StatelessWidget {
  const _HeaderButton({
    required this.icon,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 37,
        height: 37,
        alignment: Alignment.center,
        decoration: const BoxDecoration(
          color: AppColors.pale,
          shape: BoxShape.circle,
        ),
        child: Icon(icon, size: 22, color: color),
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.message});

  final AssistantMessage message;

  @override
  Widget build(BuildContext context) {
    final fromUser = message.fromUser;

    return Align(
      alignment: fromUser ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.84,
        ),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
          decoration: BoxDecoration(
            color: fromUser ? AppColors.primary : AppColors.white,
            border: fromUser ? null : Border.all(color: AppColors.border),
            borderRadius: BorderRadius.only(
              topLeft: const Radius.circular(18),
              topRight: const Radius.circular(18),
              bottomLeft: Radius.circular(fromUser ? 18 : 5),
              bottomRight: Radius.circular(fromUser ? 5 : 18),
            ),
          ),
          child: Text(
            message.content,
            style: AppTypography.body.copyWith(
              fontSize: 13,
              height: 1.46,
              color: fromUser ? AppColors.white : AppColors.ink,
            ),
          ),
        ),
      ),
    );
  }
}
