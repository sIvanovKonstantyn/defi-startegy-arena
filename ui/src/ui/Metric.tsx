const PENDING_VALUE = "—";

type MetricProps = {
  label: string;
  value: string;
  pendingLabel?: string;
};

export function Metric(props: MetricProps) {
  const pending = props.value.trim().length === 0;
  return (
    <div className="metric">
      <span className="text-caption metric-label">{props.label}</span>
      <span className="text-h3 metric-value">{pending ? PENDING_VALUE : props.value}</span>
      {pending ? (
        <span className="text-caption metric-note">{props.pendingLabel ?? "Pending"}</span>
      ) : null}
    </div>
  );
}
