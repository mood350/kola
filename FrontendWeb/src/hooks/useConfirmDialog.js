import { useCallback, useState } from 'react';

/**
 * Gates a mutating action behind a confirm dialog. `request(config, run)`
 * stages the action; the caller renders <ConfirmDialog {...dialogProps} />
 * once, and `run` only fires if the user confirms.
 */
export function useConfirmDialog() {
  const [pendingRequest, setPendingRequest] = useState(null);
  const [busy, setBusy] = useState(false);
  const [inputValue, setInputValue] = useState('');

  const request = useCallback((config, run) => {
    setInputValue('');
    setPendingRequest({ ...config, run });
  }, []);

  const cancel = useCallback(() => {
    if (busy) return;
    setPendingRequest(null);
  }, [busy]);

  const confirm = useCallback(async () => {
    if (!pendingRequest) return;
    // A required field guards the action here rather than in each caller: a rejection with no
    // reason is refused by the server anyway, and failing at the dialog keeps the text typed.
    if (pendingRequest.input?.required && !inputValue.trim()) return;
    setBusy(true);
    try {
      await pendingRequest.run(inputValue.trim());
    } catch {
      // The action's own error state (actionError) surfaces the failure —
      // just close the dialog either way.
    } finally {
      setBusy(false);
      setPendingRequest(null);
    }
  }, [pendingRequest, inputValue]);

  return {
    request,
    dialogProps: {
      open: Boolean(pendingRequest),
      pending: busy,
      title: pendingRequest?.title,
      message: pendingRequest?.message,
      danger: pendingRequest?.danger,
      confirmLabel: pendingRequest?.confirmLabel,
      input: pendingRequest?.input,
      inputValue,
      onInputChange: setInputValue,
      onConfirm: confirm,
      onCancel: cancel,
    },
  };
}
