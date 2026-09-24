import type { ReactNode } from "react";

type PageHeaderProps = {
  title: string;
  description?: string;
  actions?: ReactNode;
};

export function PageHeader(props: PageHeaderProps) {
  return (
    <div className="page-header">
      <div className="page-header-copy">
        <h1 className="text-h1">{props.title}</h1>
        {props.description ? <p className="page-header-desc">{props.description}</p> : null}
      </div>
      {props.actions ? <div className="page-header-actions">{props.actions}</div> : null}
    </div>
  );
}
