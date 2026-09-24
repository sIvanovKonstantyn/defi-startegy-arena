import type { ReactNode } from "react";

type EmptyStateProps = {
  title: string;
  description: string;
  action?: ReactNode;
};

export function EmptyState(props: EmptyStateProps) {
  return (
    <div className="empty-state">
      <h2 className="text-h3">{props.title}</h2>
      <p className="empty-state-desc">{props.description}</p>
      {props.action ? <div className="empty-state-action">{props.action}</div> : null}
    </div>
  );
}
