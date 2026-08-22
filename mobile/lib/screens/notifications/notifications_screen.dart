import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/utils/date_format_utils.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/skeleton_list_item.dart';
import '../../models/notification.dart';
import '../../providers/notification_provider.dart';

/// Liste des notifications in-app (cloche de Home).
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<NotificationProvider>().loadNotifications();
    });
    _scrollController.addListener(() {
      if (_scrollController.position.pixels >=
          _scrollController.position.maxScrollExtent - 200) {
        context.read<NotificationProvider>().loadMore();
      }
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  IconData _iconFor(AppNotificationType type) {
    switch (type) {
      case AppNotificationType.transaction:
        return Icons.receipt_long_rounded;
      case AppNotificationType.security:
        return Icons.shield_outlined;
      case AppNotificationType.system:
        return Icons.info_outline_rounded;
    }
  }

  Color _colorFor(AppNotificationType type) {
    switch (type) {
      case AppNotificationType.transaction:
        return AppColors.primary;
      case AppNotificationType.security:
        return AppColors.warning;
      case AppNotificationType.system:
        return AppColors.onSurfaceVariant;
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<NotificationProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Notifications', style: AppTypography.headingSm),
        actions: [
          if (provider.unreadCount > 0)
            TextButton(
              onPressed: () =>
                  context.read<NotificationProvider>().markAllAsRead(),
              child: const Text('Tout marquer comme lu'),
            ),
        ],
      ),
      body: SafeArea(
        child: Builder(
          builder: (context) {
            if (provider.isLoading && provider.notifications.isEmpty) {
              return const Padding(
                padding: EdgeInsets.all(AppSpacing.marginMobile),
                child: SkeletonList(),
              );
            }
            if (provider.errorMessage != null &&
                provider.notifications.isEmpty) {
              return EmptyState(
                icon: Icons.error_outline_rounded,
                title: 'Impossible de charger les notifications',
                subtitle: provider.errorMessage,
              );
            }
            if (provider.notifications.isEmpty) {
              return const EmptyState(
                icon: Icons.notifications_none_rounded,
                title: 'Aucune notification pour le moment',
              );
            }

            return ListView.separated(
              controller: _scrollController,
              padding: const EdgeInsets.all(AppSpacing.marginMobile),
              itemCount:
                  provider.notifications.length + (provider.hasMore ? 1 : 0),
              separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
              itemBuilder: (context, index) {
                if (index >= provider.notifications.length) {
                  return const Padding(
                    padding: EdgeInsets.symmetric(vertical: AppSpacing.md),
                    child: Center(
                      child: CircularProgressIndicator(
                        color: AppColors.primary,
                      ),
                    ),
                  );
                }

                final notification = provider.notifications[index];
                return InkWell(
                  onTap: () => context.read<NotificationProvider>().markAsRead(
                    notification.id,
                  ),
                  borderRadius: BorderRadius.circular(AppRadius.lg),
                  child: Container(
                    padding: const EdgeInsets.all(AppSpacing.md),
                    decoration: BoxDecoration(
                      color: notification.read
                          ? AppColors.surfaceCard
                          : AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(AppRadius.lg),
                      border: Border.all(color: AppColors.secondaryContainer),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          width: AppDimens.iconBadgeSize,
                          height: AppDimens.iconBadgeSize,
                          decoration: BoxDecoration(
                            color: _colorFor(
                              notification.type,
                            ).withValues(alpha: 0.1),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            _iconFor(notification.type),
                            color: _colorFor(notification.type),
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Expanded(
                                    child: Text(
                                      notification.title,
                                      style: notification.read
                                          ? AppTypography.bodyMd
                                          : AppTypography.bodyMdBold,
                                    ),
                                  ),
                                  if (!notification.read)
                                    Container(
                                      width: 8,
                                      height: 8,
                                      margin: const EdgeInsets.only(
                                        left: AppSpacing.xs,
                                      ),
                                      decoration: const BoxDecoration(
                                        color: AppColors.primary,
                                        shape: BoxShape.circle,
                                      ),
                                    ),
                                ],
                              ),
                              const SizedBox(height: AppSpacing.xxs),
                              Text(
                                notification.body,
                                style: AppTypography.bodySm,
                              ),
                              const SizedBox(height: AppSpacing.xxs),
                              Text(
                                formatRelativeDate(notification.createdAt),
                                style: AppTypography.labelXs,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }
}
