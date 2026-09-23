import { useEffect, useState } from "react";
import type { StrategyDetail, StrategyRule, StrategySummary } from "./api/types";
import {
  IconAdd,
  IconButton,
  IconChevronLeft,
  IconChevronRight,
  IconClose,
  IconEdit,
  IconRemove,
  IconSave,
} from "./icons/IconSet";

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

  return (
    <div className={`strategies-workspace${panelOpen ? " with-panel" : ""}`}>
      {panelOpen ? (
        <section className="panel side-panel" aria-label="Strategy form">
          <div className="panel-header">
            <h2>{panel === "create" ? "New strategy" : "Edit strategy"}</h2>
            <IconButton
              label="Close"
              className="button-secondary"
              disabled={creating}
              onClick={closePanel}
            >
              <IconClose />
            </IconButton>
          </div>

          {panel === "create" ? (
            <>
              <label>
                Name
                <input
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  aria-label="New strategy name"
                  disabled={creating}
                />
              </label>
              <div className="row">
                <IconButton
                  label="Save strategy"
                  disabled={creating}
                  onClick={() => void submitCreate()}
                >
                  <IconSave />
                </IconButton>
              </div>
            </>
          ) : null}

          {panel === "edit" && props.selected ? (
            <>
              <p className="muted-id">{props.selected.name}</p>
              <label>
                Threshold
                <input
                  value={threshold}
                  onChange={(event) => setThreshold(event.target.value)}
                  disabled={busyId === props.selected.strategyId}
                />
              </label>
              <div className="row">
                <IconButton
                  label="Save changes"
                  disabled={busyId === props.selected.strategyId}
                  onClick={() => {
                    const selected = props.selected;
                    if (!selected) {
                      return;
                    }
                    void runRow(selected.strategyId, saveSelected);
                  }}
                >
                  <IconSave />
                </IconButton>
              </div>
            </>
          ) : null}
        </section>
      ) : null}

      <section className="panel table-panel">
        <div className="panel-header">
          <h2>Strategies</h2>
          <IconButton
            label="Add strategy"
            disabled={creating}
            onClick={() => {
              props.onCloseEditor();
              setPanel("create");
              setName("");
            }}
          >
            <IconAdd />
          </IconButton>
        </div>

        <div className="table-wrap">
          <table className="strategy-table">
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col">Privacy</th>
                <th scope="col">Version</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {props.strategies.length === 0 ? (
                <tr>
                  <td colSpan={4}>No strategies yet.</td>
                </tr>
              ) : (
                props.strategies.map((strategy) => {
                  const rowBusy = busyId === strategy.strategyId;
                  return (
                    <tr key={strategy.strategyId}>
                      <td>{strategy.name}</td>
                      <td>{strategy.privacy}</td>
                      <td>{strategy.versionNumber}</td>
                      <td>
                        <div className="row">
                          <IconButton
                            label="Edit strategy"
                            className="button-secondary"
                            disabled={rowBusy || creating}
                            onClick={() =>
                              void runRow(strategy.strategyId, () =>
                                props.onEdit(strategy.strategyId),
                              )
                            }
                          >
                            <IconEdit />
                          </IconButton>
                          <IconButton
                            label="Remove strategy"
                            className="button-danger"
                            disabled={rowBusy || creating}
                            onClick={() =>
                              void runRow(strategy.strategyId, () =>
                                props.onDelete(strategy.strategyId),
                              )
                            }
                          >
                            <IconRemove />
                          </IconButton>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="pagination row">
          <IconButton
            label="Previous page"
            className="button-secondary"
            disabled={!canPrev || creating}
            onClick={() => props.onPageChange(props.page - 1)}
          >
            <IconChevronLeft />
          </IconButton>
          <span>
            {props.page + 1}/{totalPages}
          </span>
          <IconButton
            label="Next page"
            className="button-secondary"
            disabled={!canNext || creating}
            onClick={() => props.onPageChange(props.page + 1)}
          >
            <IconChevronRight />
          </IconButton>
        </div>
      </section>
    </div>
  );
}
