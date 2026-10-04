import { describe, expect, it } from "vitest";
import {
  BASIC83_ROUTES,
  canAccessBasic83Route,
  findBasic83Route,
} from "./basic83RouteAccess";

describe("BASIC-83 route access", () => {
  it("restricts individual achievement screens to the approved read roles", () => {
    const route = findBasic83Route(
      "/faculty/education/employment-rate-improvements",
    );

    expect(route?.screenId).toBe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS");
    expect(canAccessBasic83Route(route!, ["R01"])).toBe(true);
    expect(canAccessBasic83Route(route!, ["R02"])).toBe(true);
    expect(canAccessBasic83Route(route!, ["R04"])).toBe(true);
    expect(canAccessBasic83Route(route!, ["R07"])).toBe(false);
  });

  it("allows the R07 operational route while retaining the four approved routes", () => {
    const route = findBasic83Route(
      "/faculty/education/employment-rate-achievements",
    );

    expect(BASIC83_ROUTES).toHaveLength(4);
    expect(route?.screenId).toBe("SCR-EMPLOYMENT-RATE-ACHIEVEMENTS");
    expect(canAccessBasic83Route(route!, ["R07"])).toBe(true);
    expect(canAccessBasic83Route(route!, ["R09"])).toBe(false);
  });
});
