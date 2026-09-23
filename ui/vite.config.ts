/// <reference types="vitest/config" />
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

const API_TARGET = process.env.VITE_API_PROXY ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/auth": API_TARGET,
      "/strategies": API_TARGET,
      "/ws": {
        target: API_TARGET,
        ws: true,
      },
    },
  },
  preview: {
    proxy: {
      "/auth": API_TARGET,
      "/strategies": API_TARGET,
      "/ws": {
        target: API_TARGET,
        ws: true,
      },
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
