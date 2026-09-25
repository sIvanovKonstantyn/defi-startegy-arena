import type { ActionType, ActionWire } from "../api/types";
import { InputField } from "../ui/InputField";
import { SelectField } from "../ui/SelectField";
import { ACTION_TYPE_OPTIONS, newAction } from "./ruleModel";

type ActionEditorProps = {
  action: ActionWire;
  idPrefix: string;
  disabled: boolean;
  onChange: (action: ActionWire) => void;
};

export function ActionEditor(props: ActionEditorProps) {
  const { action, idPrefix, disabled } = props;
  const tradesInstrument = action.type === "buy" || action.type === "sell";

  return (
    <fieldset className="action-node">
      <legend className="text-caption condition-node-title">Action</legend>
      <div className="field-grid">
        <SelectField
          id={`${idPrefix}-action-type`}
          label="Action type"
          value={action.type}
          disabled={disabled}
          options={ACTION_TYPE_OPTIONS}
          onChange={(event) => props.onChange(newAction(event.target.value as ActionType))}
        />
        {tradesInstrument ? (
          <InputField
            id={`${idPrefix}-action-instrument`}
            label="Instrument"
            value={action.instrument ?? ""}
            disabled={disabled}
            onChange={(event) => props.onChange({ ...action, instrument: event.target.value })}
          />
        ) : null}
        {action.type === "open_lp" ? (
          <InputField
            id={`${idPrefix}-action-pair`}
            label="Instrument pair"
            value={action.instrumentPair ?? ""}
            disabled={disabled}
            onChange={(event) => props.onChange({ ...action, instrumentPair: event.target.value })}
          />
        ) : null}
        {tradesInstrument || action.type === "open_lp" ? (
          <InputField
            id={`${idPrefix}-action-allocation`}
            label="Allocation percent"
            value={action.allocationPercent ?? ""}
            disabled={disabled}
            onChange={(event) =>
              props.onChange({ ...action, allocationPercent: event.target.value })
            }
          />
        ) : null}
        {action.type === "open_lp" ? (
          <InputField
            id={`${idPrefix}-action-fee`}
            label="Yearly fee percent"
            value={action.yearlyFeePercent ?? ""}
            disabled={disabled}
            onChange={(event) =>
              props.onChange({ ...action, yearlyFeePercent: event.target.value })
            }
          />
        ) : null}
      </div>
      {action.type === "hold" ? (
        <p className="field-message">Hold keeps the current position when the condition matches.</p>
      ) : null}
    </fieldset>
  );
}
