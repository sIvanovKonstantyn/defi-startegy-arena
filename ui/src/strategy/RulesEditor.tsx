import type { ActionWire, StrategyRule } from "../api/types";
import { Button } from "../ui/Button";
import { ActionEditor } from "./ActionEditor";
import { type ConditionChange, ConditionEditor } from "./ConditionEditor";
import {
  appendChild,
  type NodePath,
  newRule,
  nextRuleNumber,
  removeNode,
  replaceNode,
} from "./ruleModel";

type RulesEditorProps = {
  rules: StrategyRule[];
  disabled: boolean;
  onChange: (rules: StrategyRule[]) => void;
};

const RULE_OFFSET = 1;

export function RulesEditor(props: RulesEditorProps) {
  const replaceRule = (ruleId: string, next: StrategyRule) => {
    props.onChange(props.rules.map((rule) => (rule.id === ruleId ? next : rule)));
  };

  return (
    <div className="rules-editor">
      {props.rules.map((rule, index) => {
        const idPrefix = `rule-${rule.id}`;
        const onConditionChange = (change: ConditionChange) =>
          replaceRule(rule.id, { ...rule, when: replaceNode(rule.when, change.path, change.node) });
        const onConditionAdd = (change: ConditionChange) =>
          replaceRule(rule.id, { ...rule, when: appendChild(rule.when, change.path, change.node) });
        const onConditionRemove = (path: NodePath) =>
          replaceRule(rule.id, { ...rule, when: removeNode(rule.when, path) });
        const onActionChange = (action: ActionWire) =>
          // biome-ignore lint/suspicious/noThenProperty: `when` / `then` are the wire field names
          replaceRule(rule.id, { ...rule, then: action });

        return (
          <fieldset className="rule-card" key={rule.id}>
            <legend className="text-h3 rule-card-title">{`Rule ${index + RULE_OFFSET}`}</legend>
            <div className="rule-card-toolbar">
              <span className="text-small muted-id">Rule id: {rule.id}</span>
              <Button
                variant="danger"
                disabled={props.disabled}
                onClick={() =>
                  props.onChange(props.rules.filter((candidate) => candidate.id !== rule.id))
                }
              >
                Remove rule
              </Button>
            </div>
            <ConditionEditor
              node={rule.when}
              path={[]}
              idPrefix={idPrefix}
              disabled={props.disabled}
              onChange={onConditionChange}
              onAdd={onConditionAdd}
              onRemove={onConditionRemove}
            />
            <ActionEditor
              action={rule.then}
              idPrefix={idPrefix}
              disabled={props.disabled}
              onChange={onActionChange}
            />
          </fieldset>
        );
      })}
      <div className="row">
        <Button
          variant="secondary"
          disabled={props.disabled}
          onClick={() => props.onChange([...props.rules, newRule(nextRuleNumber(props.rules))])}
        >
          Add rule
        </Button>
      </div>
    </div>
  );
}
