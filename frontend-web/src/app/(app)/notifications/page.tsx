"use client";

import { useCallback, useState } from "react";
import { notificationApi } from "@/lib/services";
import { describeActionError, useResource } from "@/lib/use-resource";
import { formatRelative } from "@/lib/format";
import {
  NOTIFICATION_TYPE_LABEL,
  NOTIFICATION_TYPE_TONE,
} from "@/lib/labels";
import { cn } from "@/lib/cn";
import {
  Alert,
  Badge,
  Button,
  EmptyState,
  LoadError,
  SkeletonList,
} from "@/components/ui/primitives";
import { PageHeader } from "@/components/layout/page-header";
import { BellIcon } from "@/components/ui/icons";
import type { AppNotification } from "@/lib/types";

const PAGE_SIZE = 20;

/**
 * Notifications.
 *
 * ELLES NE SE MARQUENT PAS LUES TOUTES SEULES À L'AFFICHAGE. Une alerte de
 * sécurité — « connexion depuis un nouvel appareil » — perdrait sa raison
 * d'être si elle s'éteignait au premier coup d'œil sur la liste. Le passage en
 * « lu » est donc un geste : cliquer la notification, ou tout marquer d'un
 * coup.
 */
export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const load = useCallback(
    (signal: AbortSignal) => notificationApi.list(page, PAGE_SIZE, signal),
    [page]
  );
  const notifications = useResource(load);

  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const markOne = async (notification: AppNotification) => {
    if (notification.read) return;
    setError(null);
    try {
      await notificationApi.markAsRead(notification.id);
      notifications.reload();
    } catch (caught) {
      setError(describeActionError(caught));
    }
  };

  const markAll = async () => {
    setError(null);
    setBusy(true);
    try {
      await notificationApi.markAllAsRead();
      notifications.reload();
    } catch (caught) {
      setError(describeActionError(caught));
    } finally {
      setBusy(false);
    }
  };

  const data = notifications.data;
  const hasUnread = (data?.content ?? []).some((item) => !item.read);

  return (
    <>
      <PageHeader
        title="Notifications"
        action={
          hasUnread ? (
            <Button variant="secondary" size="sm" loading={busy} onClick={() => void markAll()}>
              Tout marquer comme lu
            </Button>
          ) : null
        }
      />

      {error ? (
        <div className="mb-4">
          <Alert tone="danger" onDismiss={() => setError(null)}>
            {error}
          </Alert>
        </div>
      ) : null}

      {notifications.loading && !data ? (
        <SkeletonList rows={5} />
      ) : notifications.error ? (
        <LoadError message={notifications.error} onRetry={notifications.reload} />
      ) : data && data.content.length > 0 ? (
        <>
          <ul className="space-y-2">
            {data.content.map((notification) => (
              <li key={notification.id}>
                <button
                  type="button"
                  onClick={() => void markOne(notification)}
                  disabled={notification.read}
                  className={cn(
                    "w-full rounded-card p-4 text-left ring-1 transition-colors",
                    notification.read
                      ? "bg-surface ring-line"
                      : "bg-kola-50/60 ring-kola-200 hover:bg-kola-50"
                  )}
                >
                  <div className="flex items-start gap-3">
                    {/* La pastille est la seule marque du non-lu qui reste
                        lisible en un coup d'œil sur une longue liste. */}
                    <span
                      className={cn(
                        "mt-1.5 size-2 shrink-0 rounded-full",
                        notification.read ? "bg-transparent" : "bg-kola-600"
                      )}
                      aria-hidden
                    />
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <p
                          className={cn(
                            "text-sm",
                            notification.read
                              ? "font-medium text-ink-700"
                              : "font-semibold text-ink-950"
                          )}
                        >
                          {notification.title}
                        </p>
                        <Badge tone={NOTIFICATION_TYPE_TONE[notification.type]}>
                          {NOTIFICATION_TYPE_LABEL[notification.type]}
                        </Badge>
                      </div>
                      <p className="mt-0.5 text-sm text-ink-600">{notification.body}</p>
                      <p className="mt-1 text-xs text-ink-400">
                        {formatRelative(notification.createdAt)}
                        {notification.read ? "" : " · non lue, cliquez pour marquer comme lue"}
                      </p>
                    </div>
                  </div>
                </button>
              </li>
            ))}
          </ul>

          {data.totalPages > 1 ? (
            <nav className="mt-6 flex items-center justify-between gap-3" aria-label="Pagination">
              <Button
                variant="secondary"
                size="sm"
                disabled={data.number === 0 || notifications.loading}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Précédent
              </Button>
              <span className="text-sm text-ink-500">
                Page {data.number + 1} sur {data.totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={data.last || notifications.loading}
                onClick={() => setPage((current) => current + 1)}
              >
                Suivant
              </Button>
            </nav>
          ) : null}
        </>
      ) : (
        <EmptyState
          icon={<BellIcon />}
          title="Aucune notification"
          description="Les mouvements sur votre compte et les alertes de sécurité apparaîtront ici."
        />
      )}
    </>
  );
}
