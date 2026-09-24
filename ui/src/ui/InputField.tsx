import type { InputHTMLAttributes, ReactNode } from "react";

type InputFieldProps = {
  label: string;
  hint?: ReactNode;
  error?: string;
} & Omit<InputHTMLAttributes<HTMLInputElement>, "id"> & {
    id: string;
  };

export function InputField(props: InputFieldProps) {
  const { label, hint, error, id, className, ...rest } = props;
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
  return (
    <div className={`field ${error ? "field-error" : ""}`.trim()}>
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        className={`field-input ${className ?? ""}`.trim()}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        {...rest}
      />
      {error ? (
        <p id={`${id}-error`} className="field-message field-message-error">
          {error}
        </p>
      ) : null}
      {!error && hint ? (
        <p id={`${id}-hint`} className="field-message">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
