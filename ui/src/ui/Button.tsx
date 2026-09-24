import type { ButtonHTMLAttributes, ReactNode } from "react";

type ButtonVariant = "primary" | "secondary" | "tertiary" | "danger";

type ButtonProps = {
  variant?: ButtonVariant;
  children: ReactNode;
} & ButtonHTMLAttributes<HTMLButtonElement>;

export function Button(props: ButtonProps) {
  const { variant = "primary", className, type, children, ...rest } = props;
  return (
    <button
      type={type ?? "button"}
      className={`btn btn-${variant} ${className ?? ""}`.trim()}
      {...rest}
    >
      {children}
    </button>
  );
}
