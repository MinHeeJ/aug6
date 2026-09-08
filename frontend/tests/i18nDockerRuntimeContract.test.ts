import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { describe, expect, it } from "vitest";

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const dockerfile = readFileSync(
  resolve(currentDirectory, "../Dockerfile"),
  "utf8",
);
const packageJson = JSON.parse(
  readFileSync(resolve(currentDirectory, "../package.json"), "utf8"),
) as { dependencies?: Record<string, string> };

describe("BASIC-56 frontend Docker i18n runtime contract", () => {
  it("build stage installs i18next/react-i18next before compiling the static bundle", () => {
    expect(packageJson.dependencies?.i18next).toMatch(/^23\./);
    expect(packageJson.dependencies?.["react-i18next"]).toMatch(/^14\./);
    expect(dockerfile).toMatch(/FROM node:20\.11\.0-alpine AS build/);
    expect(dockerfile).toMatch(/COPY package\.json package-lock\.json \.\//);
    expect(dockerfile).toMatch(/RUN npm ci/);
    expect(dockerfile.indexOf("RUN npm ci")).toBeLessThan(
      dockerfile.indexOf("RUN npm run build"),
    );
  });

  it("runtime image serves prebuilt assets and does not run npm registry downloads", () => {
    const runtimeStage = dockerfile.split(
      /FROM nginx:1\.27\.3-alpine AS runtime/,
    )[1];

    expect(runtimeStage, "nginx runtime stage must exist").toBeTruthy();
    expect(runtimeStage).toContain(
      "COPY --from=build /app/dist /usr/share/nginx/html",
    );
    expect(runtimeStage).not.toMatch(
      /npm\s+(ci|install|add|update)|npx|yarn|pnpm/,
    );
    expect(runtimeStage).not.toMatch(/registry\.npmjs\.org|curl|wget.*npm/);
    expect(runtimeStage).toContain('CMD ["nginx", "-g", "daemon off;"]');
  });
});
