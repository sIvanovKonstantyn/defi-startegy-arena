import type { ReactNode, SelectHTMLAttributes } from "react";

export type SelectOption = {
  value: string;
  label: string;
};

type SelectFieldProps = {
  label: string;
  hint?: ReactNode;
  options: SelectOption[];
} & Omit<SelectHTMLAttributes<HTMLSelectElement>, "id" | "children"> & {
    id: string;
  };

export function SelectField(props: SelectFieldProps) {
  const { label, hint, options, id, className, ...rest } = props;
  return (
    <div className="field">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <select
        id={id}
        className={`field-input field-select ${className ?? ""}`.trim()}
        aria-describedby={hint ? `${id}-hint` : undefined}
        {...rest}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {hint ? (
        <p id={`${id}-hint`} className="field-message">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
