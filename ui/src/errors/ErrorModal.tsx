import { useEffect } from "react";
import { IconAlertError, IconButton, IconClose } from "../icons/IconSet";

type ErrorModalProps = {
  message: string;
  onClose: () => void;
};

export function ErrorModal(props: ErrorModalProps) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        props.onClose();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [props]);

  return (
    <div className="error-modal-backdrop">
      <button
        type="button"
        className="error-modal-scrim"
        aria-label="Dismiss error"
        onClick={props.onClose}
      />
      <div
        className="error-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="error-modal-title"
        aria-describedby="error-modal-desc"
      >
        <div className="error-modal-header">
          <IconAlertError className="error-modal-icon" />
          <h2 id="error-modal-title">Something went wrong</h2>
          <IconButton label="Close" className="button-secondary" onClick={props.onClose}>
            <IconClose />
          </IconButton>
        </div>
        <p id="error-modal-desc" className="error-modal-body">
          {props.message}
        </p>
        <div className="error-modal-actions">
          <IconButton label="Dismiss" onClick={props.onClose}>
            <IconClose />
          </IconButton>
        </div>
      </div>
    </div>
  );
}
