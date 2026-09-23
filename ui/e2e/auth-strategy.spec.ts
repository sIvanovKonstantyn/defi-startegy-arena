import { expect, test } from "@playwright/test";

const TOKEN = "test-token";
const CREATE_CORRELATION = "corr-create-1";
const LIST_CORRELATION = "corr-list-1";

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

  await page.route("**/strategies**", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 202,
        contentType: "application/json",
        body: JSON.stringify({ status: 202, correlationId: CREATE_CORRELATION }),
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
  await page.getByRole("button", { name: "Log in", exact: true }).click();
  await expect(page.getByRole("button", { name: "Add strategy" })).toBeEnabled();
  await expect(page.getByRole("link", { name: "Home" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Strategies" })).toBeVisible();

  await page.evaluate((correlationId) => {
    (window as unknown as { __dsaEmit: (payload: unknown) => void }).__dsaEmit({
      correlationId,
      type: "strategy.list",
      status: "completed",
      payload: { items: [], total: 0 },
    });
  }, LIST_CORRELATION);

  await page.getByRole("button", { name: "Add strategy" }).click();
  await page.getByLabel("New strategy name").fill("ws-alpha");

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
  await page.evaluate(() => {
    (window as unknown as { __dsaEmit: (payload: unknown) => void }).__dsaEmit({
      correlationId: "corr-list-1",
      type: "strategy.list",
      status: "completed",
      payload: {
        items: [
          {
            strategyId: "strategy-1",
            name: "ws-alpha",
            privacy: "PRIVATE",
            versionNumber: 1,
          },
        ],
        total: 1,
      },
    });
  });
  await createClick;

  await expect(page.getByRole("cell", { name: "ws-alpha" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Edit strategy" })).toBeEnabled();
  await expect(page.getByRole("button", { name: "Remove strategy" })).toBeEnabled();
});
