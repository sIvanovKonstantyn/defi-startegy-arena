import type { ReactNode, TextareaHTMLAttributes } from "react";

type TextAreaFieldProps = {
  label: string;
  hint?: ReactNode;
} & Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, "id"> & {
    id: string;
  };

export function TextAreaField(props: TextAreaFieldProps) {
  const { label, hint, id, className, rows, ...rest } = props;
  return (
    <div className="field">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <textarea
        id={id}
        rows={rows ?? 3}
        className={`field-input field-textarea ${className ?? ""}`.trim()}
        aria-describedby={hint ? `${id}-hint` : undefined}
        {...rest}
      />
      {hint ? (
        <p id={`${id}-hint`} className="field-message">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
