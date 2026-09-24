import type { ButtonHTMLAttributes, ReactNode } from "react";

type IconButtonProps = {
  label: string;
  children: ReactNode;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children" | "aria-label">;

export function IconButton(props: IconButtonProps) {
  const { label, className, type, children, ...rest } = props;
  return (
    <button
      type={type ?? "button"}
      className={`btn btn-icon ${className ?? ""}`.trim()}
      aria-label={label}
      title={label}
      {...rest}
    >
      {children}
    </button>
  );
}
