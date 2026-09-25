import type { CompareOperator, ConditionType, ConditionWire } from "../api/types";
import { INDICATOR_CATALOG, indicatorParameters } from "../indicators/catalog";
import { Button } from "../ui/Button";
import { InputField } from "../ui/InputField";
import { SelectField, type SelectOption } from "../ui/SelectField";
import {
  CONDITION_TYPE_OPTIONS,
  childrenOf,
  conditionOfType,
  isGroup,
  type NodePath,
  newGroup,
  newIndicatorCompare,
  newPriceCompare,
  OPERATOR_OPTIONS,
  withIndicator,
  withParameter,
} from "./ruleModel";

export type ConditionChange = {
  path: NodePath;
  node: ConditionWire;
};

type ConditionEditorProps = {
  node: ConditionWire;
  path: NodePath;
  idPrefix: string;
  disabled: boolean;
  onChange: (change: ConditionChange) => void;
  onAdd: (change: ConditionChange) => void;
  onRemove: (path: NodePath) => void;
};

const INDICATOR_OPTIONS: SelectOption[] = INDICATOR_CATALOG.map((indicator) => ({
  value: indicator.id,
  label: indicator.displayName,
}));

const PATH_OFFSET = 1;

function nodeTitle(path: NodePath): string {
  if (path.length === 0) {
    return "Condition";
  }
  return `Condition ${path.map((step) => step + PATH_OFFSET).join(".")}`;
}

export function ConditionEditor(props: ConditionEditorProps) {
  const { node, path, idPrefix, disabled } = props;
  const nodeId = path.length === 0 ? `${idPrefix}-root` : `${idPrefix}-${path.join("-")}`;
  const title = nodeTitle(path);

  const changeType = (next: ConditionType) => {
    const keepsChildren = isGroup(node) && (next === "and" || next === "or");
    props.onChange({ path, node: keepsChildren ? { ...node, type: next } : conditionOfType(next) });
  };

  const patch = (next: ConditionWire) => props.onChange({ path, node: next });

  return (
    <fieldset className="condition-node">
      <legend className="text-caption condition-node-title">{title}</legend>
      <div className="condition-node-controls">
        <SelectField
          id={`${nodeId}-type`}
          label="Condition type"
          value={node.type}
          disabled={disabled}
          options={CONDITION_TYPE_OPTIONS}
          onChange={(event) => changeType(event.target.value as ConditionType)}
        />
        {path.length > 0 ? (
          <Button variant="danger" disabled={disabled} onClick={() => props.onRemove(path)}>
            Remove condition
          </Button>
        ) : null}
      </div>

      {node.type === "price_compare" ? (
        <div className="field-grid">
          <InputField
            id={`${nodeId}-instrument`}
            label="Instrument"
            value={node.instrument ?? ""}
            disabled={disabled}
            onChange={(event) => patch({ ...node, instrument: event.target.value })}
          />
          <OperatorField
            id={`${nodeId}-operator`}
            value={node.operator}
            disabled={disabled}
            onSelect={(operator) => patch({ ...node, operator })}
          />
          <InputField
            id={`${nodeId}-threshold`}
            label="Threshold"
            value={node.threshold ?? ""}
            disabled={disabled}
            onChange={(event) => patch({ ...node, threshold: event.target.value })}
          />
        </div>
      ) : null}

      {node.type === "indicator_compare" ? (
        <div className="field-grid">
          <SelectField
            id={`${nodeId}-indicator`}
            label="Indicator"
            value={node.indicator ?? ""}
            disabled={disabled}
            options={INDICATOR_OPTIONS}
            onChange={(event) => patch(withIndicator(node, event.target.value))}
          />
          {indicatorParameters(node.indicator ?? "").map((parameter) => (
            <InputField
              key={parameter.name}
              id={`${nodeId}-param-${parameter.name}`}
              label={parameter.label}
              value={node.parameters?.[parameter.name] ?? ""}
              disabled={disabled}
              onChange={(event) =>
                patch(withParameter(node, { name: parameter.name, value: event.target.value }))
              }
            />
          ))}
          <OperatorField
            id={`${nodeId}-operator`}
            value={node.operator}
            disabled={disabled}
            onSelect={(operator) => patch({ ...node, operator })}
          />
          <InputField
            id={`${nodeId}-threshold`}
            label="Threshold"
            value={node.threshold ?? ""}
            disabled={disabled}
            onChange={(event) => patch({ ...node, threshold: event.target.value })}
          />
        </div>
      ) : null}

      {isGroup(node) ? (
        <div className="condition-children">
          {childrenOf(node).map((child, index) => {
            const childPath = [...path, index];
            const childKey = `${nodeId}-child-${childPath.join("-")}`;
            return (
              <ConditionEditor
                key={childKey}
                node={child}
                path={childPath}
                idPrefix={idPrefix}
                disabled={disabled}
                onChange={props.onChange}
                onAdd={props.onAdd}
                onRemove={props.onRemove}
              />
            );
          })}
          <div className="row">
            <Button
              variant="secondary"
              disabled={disabled}
              onClick={() => props.onAdd({ path, node: newPriceCompare() })}
            >
              Add price condition
            </Button>
            <Button
              variant="secondary"
              disabled={disabled}
              onClick={() => props.onAdd({ path, node: newIndicatorCompare() })}
            >
              Add indicator condition
            </Button>
            <Button
              variant="tertiary"
              disabled={disabled}
              onClick={() => props.onAdd({ path, node: newGroup("and") })}
            >
              Add nested group
            </Button>
          </div>
        </div>
      ) : null}
    </fieldset>
  );
}

type OperatorFieldProps = {
  id: string;
  value: CompareOperator | undefined;
  disabled: boolean;
  onSelect: (operator: CompareOperator) => void;
};

function OperatorField(props: OperatorFieldProps) {
  return (
    <SelectField
      id={props.id}
      label="Operator"
      value={props.value ?? OPERATOR_OPTIONS[0].value}
      disabled={props.disabled}
      options={OPERATOR_OPTIONS}
      onChange={(event) => props.onSelect(event.target.value as CompareOperator)}
    />
  );
}
