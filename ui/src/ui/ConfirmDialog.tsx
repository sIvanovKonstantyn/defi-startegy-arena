import { useEffect } from "react";
import { IconClose } from "../icons/IconSet";
import { Button } from "./Button";
import { IconButton } from "./IconButton";

type ConfirmDialogProps = {
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel?: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmDialog(props: ConfirmDialogProps) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !props.busy) {
        props.onCancel();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [props]);

  return (
    <div className="dialog-backdrop">
      <button
        type="button"
        className="dialog-scrim"
        aria-label="Dismiss"
        disabled={props.busy}
        onClick={props.onCancel}
      />
      <div
        className="dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-desc"
      >
        <div className="dialog-header">
          <h2 id="confirm-dialog-title" className="text-h3">
            {props.title}
          </h2>
          <IconButton label="Close" disabled={props.busy} onClick={props.onCancel}>
            <IconClose />
          </IconButton>
        </div>
        <p id="confirm-dialog-desc" className="dialog-body">
          {props.description}
        </p>
        <div className="dialog-actions">
          <Button variant="secondary" disabled={props.busy} onClick={props.onCancel}>
            {props.cancelLabel ?? "Cancel"}
          </Button>
          <Button
            variant={props.danger ? "danger" : "primary"}
            disabled={props.busy}
            onClick={props.onConfirm}
          >
            {props.confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
