export type Basic83Route = {
  path: string;
  screenId: string;
  title: string;
  allowedRoles: readonly string[];
};

/**
 * Defines the client-side entry guard for BASIC-83 screens. The backend remains
 * the authorization authority; this prevents an unauthorized route from
 * rendering mutation controls before the protected API reports its result.
 */
export const BASIC83_ROUTES: readonly Basic83Route[] = [
  {
    path: "/faculty/education/employment-rate-improvements",
    screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENTS",
    title: "취업률 제고 실적 관리",
    allowedRoles: ["R01", "R02", "R04"],
  },
  {
    path: "/faculty/education/course-operations",
    screenId: "SCR-COURSE-OPERATIONS",
    title: "강좌 개설·운영 실적 관리",
    allowedRoles: ["R01", "R02", "R04"],
  },
  {
    path: "/faculty/education/lecture-improvements",
    screenId: "SCR-LECTURE-IMPROVEMENTS",
    title: "강의개선 실적 관리",
    allowedRoles: ["R01", "R02", "R04"],
  },
  {
    path: "/faculty/education/employment-rate-achievements",
    screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
    title: "취업률 실적 관리",
    allowedRoles: ["R01", "R02", "R04", "R07"],
  },
];

export function findBasic83Route(path: string): Basic83Route | undefined {
  return BASIC83_ROUTES.find((route) => route.path === path);
}

export function canAccessBasic83Route(
  route: Basic83Route,
  roles: readonly string[] | null | undefined,
): boolean {
  return roles?.some((role) => route.allowedRoles.includes(role)) ?? false;
}
