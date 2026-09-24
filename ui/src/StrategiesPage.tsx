import { useEffect, useState } from "react";
import type { StrategyDetail, StrategyRule, StrategySummary } from "./api/types";
import { IconChevronLeft, IconChevronRight, IconClose } from "./icons/IconSet";
import { Badge } from "./ui/Badge";
import { Button } from "./ui/Button";
import { ConfirmDialog } from "./ui/ConfirmDialog";
import { EmptyState } from "./ui/EmptyState";
import { IconButton } from "./ui/IconButton";
import { InputField } from "./ui/InputField";
import { PageHeader } from "./ui/PageHeader";

type PanelMode = "closed" | "create" | "edit";

type StrategiesPageProps = {
  strategies: StrategySummary[];
  selected: StrategyDetail | null;
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
  onCreate: (name: string) => Promise<void>;
  onEdit: (strategyId: string) => Promise<void>;
  onSave: (strategyId: string, rules: StrategyRule[]) => Promise<void>;
  onDelete: (strategyId: string) => Promise<void>;
  onCloseEditor: () => void;
  onError: (error: unknown) => void;
};

export function StrategiesPage(props: StrategiesPageProps) {
  const [panel, setPanel] = useState<PanelMode>("closed");
  const [name, setName] = useState("");
  const [threshold, setThreshold] = useState("3000");
  const [busyId, setBusyId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<StrategySummary | null>(null);

  const totalPages = Math.max(1, Math.ceil(props.total / props.pageSize) || 1);
  const canPrev = props.page > 0;
  const canNext = (props.page + 1) * props.pageSize < props.total;
  const panelOpen = panel !== "closed";

  useEffect(() => {
    if (props.selected) {
      setPanel("edit");
      setThreshold(props.selected.rules[0]?.threshold ?? "3000");
    }
  }, [props.selected]);

  const closePanel = () => {
    setPanel("closed");
    setName("");
    props.onCloseEditor();
  };

  const openCreate = () => {
    props.onCloseEditor();
    setPanel("create");
    setName("");
  };

  const runRow = async (id: string, action: () => Promise<void>) => {
    setBusyId(id);
    try {
      await action();
    } catch (err) {
      props.onError(err);
    } finally {
      setBusyId(null);
    }
  };

  const submitCreate = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      props.onError(new Error("Strategy name is required"));
      return;
    }
    setCreating(true);
    try {
      await props.onCreate(trimmed);
      setName("");
      setPanel("closed");
    } catch (err) {
      props.onError(err);
    } finally {
      setCreating(false);
    }
  };

  const saveSelected = async () => {
    if (!props.selected) {
      return;
    }
    const rules = props.selected.rules.map((rule, index) =>
      index === 0 ? { ...rule, threshold } : rule,
    );
    await props.onSave(props.selected.strategyId, rules);
    closePanel();
  };

  const confirmDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    const target = pendingDelete;
    setPendingDelete(null);
    await runRow(target.strategyId, () => props.onDelete(target.strategyId));
  };

  return (
    <div className={`strategies-workspace${panelOpen ? " with-panel" : ""}`}>
      {panelOpen ? (
        <section className="panel side-panel" aria-label="Strategy form">
          <div className="panel-header">
            <h2 className="text-h3">{panel === "create" ? "New strategy" : "Edit strategy"}</h2>
            <IconButton label="Close" disabled={creating} onClick={closePanel}>
              <IconClose />
            </IconButton>
          </div>

          {panel === "create" ? (
            <>
              <InputField
                id="new-strategy-name"
                label="Name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                aria-label="New strategy name"
                disabled={creating}
              />
              <div className="row">
                <Button variant="primary" disabled={creating} onClick={() => void submitCreate()}>
                  Save strategy
                </Button>
              </div>
            </>
          ) : null}

          {panel === "edit" && props.selected ? (
            <>
              <p className="muted-id">{props.selected.name}</p>
              <InputField
                id="strategy-threshold"
                label="Threshold"
                value={threshold}
                onChange={(event) => setThreshold(event.target.value)}
                disabled={busyId === props.selected.strategyId}
              />
              <div className="row">
                <Button
                  variant="primary"
                  disabled={busyId === props.selected.strategyId}
                  onClick={() => {
                    const selected = props.selected;
                    if (!selected) {
                      return;
                    }
                    void runRow(selected.strategyId, saveSelected);
                  }}
                >
                  Save changes
                </Button>
              </div>
            </>
          ) : null}
        </section>
      ) : null}

      <section className="panel table-panel" aria-label="Strategies">
        <PageHeader
          title="Strategies"
          description="Your strategies for simulation and arena runs."
          actions={
            <Button variant="primary" disabled={creating} onClick={openCreate}>
              New strategy
            </Button>
          }
        />

        {props.strategies.length === 0 ? (
          <EmptyState
            title="No strategies yet"
            description="Create your first strategy to start running simulations. Use New strategy above to begin."
          />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Privacy</th>
                  <th scope="col">Version</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {props.strategies.map((strategy) => {
                  const rowBusy = busyId === strategy.strategyId;
                  return (
                    <tr key={strategy.strategyId}>
                      <td>{strategy.name}</td>
                      <td>
                        <Badge tone="neutral">{strategy.privacy}</Badge>
                      </td>
                      <td>{strategy.versionNumber}</td>
                      <td>
                        <div className="row">
                          <Button
                            variant="secondary"
                            disabled={rowBusy || creating}
                            onClick={() =>
                              void runRow(strategy.strategyId, () =>
                                props.onEdit(strategy.strategyId),
                              )
                            }
                          >
                            Edit
                          </Button>
                          <Button
                            variant="danger"
                            disabled={rowBusy || creating}
                            onClick={() => setPendingDelete(strategy)}
                          >
                            Delete
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div className="pagination row">
          <Button
            variant="secondary"
            disabled={!canPrev || creating}
            onClick={() => props.onPageChange(props.page - 1)}
            aria-label="Previous page"
          >
            <IconChevronLeft />
            Previous
          </Button>
          <span className="text-small">
            {props.page + 1} / {totalPages}
          </span>
          <Button
            variant="secondary"
            disabled={!canNext || creating}
            onClick={() => props.onPageChange(props.page + 1)}
            aria-label="Next page"
          >
            Next
            <IconChevronRight />
          </Button>
        </div>
      </section>

      {pendingDelete ? (
        <ConfirmDialog
          title="Delete strategy?"
          description={`This will permanently remove "${pendingDelete.name}" and its configuration.`}
          confirmLabel="Delete strategy"
          danger
          busy={busyId === pendingDelete.strategyId}
          onCancel={() => setPendingDelete(null)}
          onConfirm={() => void confirmDelete()}
        />
      ) : null}
    </div>
  );
}
