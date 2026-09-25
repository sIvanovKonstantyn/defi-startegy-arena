import { expect, test } from "@playwright/test";

const TOKEN = "test-token";
const CREATE_CORRELATION = "corr-create-1";
const LIST_CORRELATION = "corr-list-1";
const GET_CORRELATION = "corr-get-1";
const STRATEGY_ID = "strategy-1";
const DESCRIPTION = "Buys ETH when the trend holds";

type CreateBody = {
  name: string;
  description: string;
  rules: {
    id: string;
    when: {
      type: string;
      children?: { type: string; indicator?: string; parameters?: Record<string, string> }[];
    };
    then: { type: string };
  }[];
};

test("login create strategy and receive ws envelope", async ({ page }) => {
  await page.addInitScript(() => {
    type Listener = (event: Event) => void;

    class FakeWebSocket {
      static CONNECTING = 0;
      static OPEN = 1;
      static CLOSING = 2;
      static CLOSED = 3;
      readyState = FakeWebSocket.CONNECTING;
      url: string;
      private readonly listeners = new Map<string, Listener[]>();

      constructor(url: string) {
        this.url = url;
        (window as unknown as { __dsaSocket: FakeWebSocket }).__dsaSocket = this;
        queueMicrotask(() => {
          this.readyState = FakeWebSocket.OPEN;
          this.dispatch("open", new Event("open"));
        });
      }

      addEventListener(type: string, listener: Listener) {
        const bucket = this.listeners.get(type) ?? [];
        bucket.push(listener);
        this.listeners.set(type, bucket);
      }

      send(data: string) {
        const parsed = JSON.parse(data) as { type?: string };
        if (parsed.type === "auth") {
          this.dispatch(
            "message",
            new MessageEvent("message", {
              data: JSON.stringify({ type: "auth", status: "ok" }),
            }),
          );
        }
      }

      close() {
        this.readyState = FakeWebSocket.CLOSED;
        this.dispatch("close", new Event("close"));
      }

      dispatch(type: string, event: Event) {
        for (const listener of this.listeners.get(type) ?? []) {
          listener(event);
        }
      }
    }

    (window as unknown as { WebSocket: typeof FakeWebSocket }).WebSocket = FakeWebSocket;
    (window as unknown as { __dsaEmit: (payload: unknown) => void }).__dsaEmit = (payload) => {
      const socket = (window as unknown as { __dsaSocket?: FakeWebSocket }).__dsaSocket;
      socket?.dispatch("message", new MessageEvent("message", { data: JSON.stringify(payload) }));
    };
  });

  await page.route("**/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ status: 200, accessToken: TOKEN }),
    });
  });

  let createBody: CreateBody | null = null;

  await page.route("**/strategies**", async (route) => {
    const request = route.request();
    if (request.method() === "POST") {
      createBody = request.postDataJSON() as CreateBody;
      await route.fulfill({
        status: 202,
        contentType: "application/json",
        body: JSON.stringify({ status: 202, correlationId: CREATE_CORRELATION }),
      });
      return;
    }
    if (request.method() === "GET" && request.url().includes(`/strategies/${STRATEGY_ID}`)) {
      await route.fulfill({
        status: 202,
        contentType: "application/json",
        body: JSON.stringify({ status: 202, correlationId: GET_CORRELATION }),
      });
      return;
    }
    await route.fulfill({
      status: 202,
      contentType: "application/json",
      body: JSON.stringify({ status: 202, correlationId: LIST_CORRELATION }),
    });
  });

  await page.goto("/");
  await page.getByLabel("Email").fill("player@arena.test");
  await page.getByLabel("Password").fill("secret-value");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.getByRole("button", { name: "New strategy" })).toBeEnabled();
  await expect(page.getByRole("link", { name: "Home" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Strategies", exact: true })).toBeVisible();

  await page.evaluate((correlationId) => {
    (window as unknown as { __dsaEmit: (payload: unknown) => void }).__dsaEmit({
      correlationId,
      type: "strategy.list",
      status: "completed",
      payload: { items: [], total: 0 },
    });
  }, LIST_CORRELATION);

  await page.getByRole("button", { name: "New strategy" }).click();
  await page.getByLabel("Name").fill("ws-alpha");
  await page.getByLabel("Description").fill(DESCRIPTION);

  const rule = page.getByRole("group", { name: "Rule 1" });
  await expect(rule.getByLabel("Instrument")).toHaveValue("ETH-USD");
  await expect(rule.getByLabel("Threshold")).toHaveValue("3000");
  await rule.getByLabel("Condition type").selectOption("and");
  await rule.getByRole("button", { name: "Add indicator condition" }).click();
  await expect(
    page.getByRole("group", { name: "Condition 2" }).getByLabel("Indicator"),
  ).toHaveValue("sma");
  await expect(rule.getByLabel("Action type")).toHaveValue("hold");

  const createClick = page.getByRole("button", { name: "Save strategy" }).click();
  await page.waitForResponse(
    (response) => response.request().method() === "POST" && response.url().includes("/strategies"),
  );
  await page.evaluate((correlationId) => {
    (window as unknown as { __dsaEmit: (payload: unknown) => void }).__dsaEmit({
      correlationId,
      type: "strategy.create",
      status: "completed",
      payload: { strategyId: "strategy-1" },
    });
  }, CREATE_CORRELATION);
  await page.waitForResponse(
    (response) => response.request().method() === "GET" && response.url().includes("/strategies"),
  );
  await page.evaluate(
    (input) => {
      (window as unknown as { __dsaEmit: (payload: unknown) => void }).__dsaEmit({
        correlationId: input.correlationId,
        type: "strategy.list",
        status: "completed",
        payload: {
          items: [
            {
              strategyId: "strategy-1",
              name: "ws-alpha",
              description: input.description,
              privacy: "PRIVATE",
              versionNumber: 1,
            },
          ],
          total: 1,
        },
      });
    },
    { correlationId: LIST_CORRELATION, description: DESCRIPTION },
  );
  await createClick;

  await expect(page.getByRole("cell", { name: "ws-alpha" })).toBeVisible();
  await expect(page.getByRole("cell", { name: DESCRIPTION })).toBeVisible();
  await expect(page.getByRole("button", { name: "Edit" })).toBeEnabled();
  await expect(page.getByRole("button", { name: "Delete" })).toBeEnabled();

  const sent = createBody as CreateBody | null;
  expect(sent?.description).toBe(DESCRIPTION);
  expect(sent?.rules[0].when.type).toBe("and");
  expect(sent?.rules[0].when.children?.[0].type).toBe("price_compare");
  expect(sent?.rules[0].when.children?.[1].indicator).toBe("sma");
  expect(sent?.rules[0].then.type).toBe("hold");

  await page.getByRole("button", { name: "Edit" }).click();
  await page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().includes(`/strategies/${STRATEGY_ID}`),
  );
  await page.evaluate(
    (input) => {
      (window as unknown as { __dsaEmit: (payload: unknown) => void }).__dsaEmit({
        correlationId: input.correlationId,
        type: "strategy.get",
        status: "completed",
        payload: {
          strategyId: "strategy-1",
          name: "ws-alpha",
          description: input.description,
          privacy: "PRIVATE",
          versionNumber: 1,
          rules: [
            {
              id: "r1",
              when: {
                type: "and",
                children: [
                  {
                    type: "price_compare",
                    instrument: "ETH-USD",
                    operator: "gt",
                    threshold: "3000",
                  },
                  {
                    type: "indicator_compare",
                    indicator: "sma",
                    parameters: { period: "14" },
                    operator: "gt",
                    threshold: "3000",
                  },
                ],
              },
              // biome-ignore lint/suspicious/noThenProperty: `then` is the rule wire field name
              then: { type: "hold" },
            },
          ],
          pnl: "",
          drawdown: "",
        },
      });
    },
    { correlationId: GET_CORRELATION, description: DESCRIPTION },
  );

  await expect(page.getByRole("heading", { name: "Edit strategy" })).toBeVisible();
  await expect(page.getByLabel("Description")).toHaveValue(DESCRIPTION);
  await expect(page.getByText("PnL")).toBeVisible();
  await expect(page.getByText("Max drawdown")).toBeVisible();
  await expect(page.getByText("Pending").first()).toBeVisible();
});
