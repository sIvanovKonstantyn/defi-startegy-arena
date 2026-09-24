import type { ReactNode } from "react";

type BadgeTone = "neutral" | "success" | "warning" | "danger" | "info";

type BadgeProps = {
  tone?: BadgeTone;
  children: ReactNode;
};

export function Badge(props: BadgeProps) {
  const tone = props.tone ?? "neutral";
  return <span className={`badge badge-${tone}`}>{props.children}</span>;
}
