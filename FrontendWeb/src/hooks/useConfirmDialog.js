import { useCallback, useState } from 'react';

/**
 * Gates a mutating action behind a confirm dialog. `request(config, run)`
 * stages the action; the caller renders <ConfirmDialog {...dialogProps} />
 * once, and `run` only fires if the user confirms.
 */
export function useConfirmDialog() {
  const [pendingRequest, setPendingRequest] = useState(null);
  const [busy, setBusy] = useState(false);

  const request = useCallback((config, run) => {
    setPendingRequest({ ...config, run });
  }, []);

  const cancel = useCallback(() => {
    if (busy) return;
    setPendingRequest(null);
  }, [busy]);

  const confirm = useCallback(async () => {
    if (!pendingRequest) return;
    setBusy(true);
    try {
      await pendingRequest.run();
    } catch {
      // The action's own error state (actionError) surfaces the failure —
      // just close the dialog either way.
    } finally {
      setBusy(false);
      setPendingRequest(null);
    }
  }, [pendingRequest]);

  return {
    request,
    dialogProps: {
      open: Boolean(pendingRequest),
      pending: busy,
      title: pendingRequest?.title,
      message: pendingRequest?.message,
      danger: pendingRequest?.danger,
      confirmLabel: pendingRequest?.confirmLabel,
      onConfirm: confirm,
      onCancel: cancel,
    },
  };
}
