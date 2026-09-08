import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const readJson = <T>(path: string): T =>
  JSON.parse(readFileSync(resolve(process.cwd(), path), "utf8")) as T;

interface PackageJson {
  dependencies?: Record<string, string>;
}

interface PackageLock {
  packages?: Record<
    string,
    {
      version?: string;
      dependencies?: Record<string, string>;
    }
  >;
}

describe("frontend i18next dependency and Docker build contract", () => {
  it("declares i18next and react-i18next as exact production dependencies in package.json and package-lock.json", () => {
    const packageJson = readJson<PackageJson>("package.json");
    const packageLock = readJson<PackageLock>("package-lock.json");

    expect(packageJson.dependencies?.i18next).toBe("23.16.8");
    expect(packageJson.dependencies?.["react-i18next"]).toBe("14.1.3");

    expect(packageLock.packages?.[""]?.dependencies?.i18next).toBe("23.16.8");
    expect(packageLock.packages?.[""]?.dependencies?.["react-i18next"]).toBe(
      "14.1.3",
    );
    expect(packageLock.packages?.["node_modules/i18next"]?.version).toBe(
      "23.16.8",
    );
    expect(packageLock.packages?.["node_modules/react-i18next"]?.version).toBe(
      "14.1.3",
    );
  });

  it("installs dependencies in the frontend Docker build stage before the nginx runtime stage", () => {
    const dockerfile = readFileSync(
      resolve(process.cwd(), "Dockerfile"),
      "utf8",
    );
    const copyLockIndex = dockerfile.indexOf(
      "COPY package.json package-lock.json ./",
    );
    const npmCiIndex = dockerfile.indexOf("RUN npm ci");
    const buildStageIndex = dockerfile.indexOf(
      "FROM node:20.11.0-alpine AS build",
    );
    const runtimeStageIndex = dockerfile.indexOf(
      "FROM nginx:1.27.3-alpine AS runtime",
    );

    expect(buildStageIndex).toBeGreaterThanOrEqual(0);
    expect(copyLockIndex).toBeGreaterThan(buildStageIndex);
    expect(npmCiIndex).toBeGreaterThan(copyLockIndex);
    expect(runtimeStageIndex).toBeGreaterThan(npmCiIndex);
    expect(dockerfile.slice(runtimeStageIndex)).not.toMatch(
      /npm\s+(ci|install)/,
    );
  });
});
