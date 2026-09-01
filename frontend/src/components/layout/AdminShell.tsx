import type React from "react";
import { useMemo, useState } from "react";
import {
  Bell,
  ChevronDown,
  LogOut,
  Menu,
  Moon,
  Search,
  Settings,
  Shield,
  Sun,
  UserCircle,
  UserCog,
  X,
} from "lucide-react";
import { useAuth } from "../../app/AuthProvider";
import type { MenuItem } from "../../api/apiClient";

export function AdminShell({ children }: { children: React.ReactNode }) {
  const auth = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [miniSidebar, setMiniSidebar] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [activeHeaderMenuId, setActiveHeaderMenuId] = useState<number | null>(
    null,
  );
  const [searchQuery, setSearchQuery] = useState("");
  const [darkMode, setDarkMode] = useState(() =>
    typeof document !== "undefined"
      ? document.documentElement.classList.contains("dark")
      : false,
  );
  const currentPath =
    typeof window === "undefined" ? "/" : window.location.pathname;
  const visibleMenus = useMemo(
    () => auth.user?.menus ?? [],
    [auth.user?.menus],
  );
  const activeHeaderMenu =
    visibleMenus.find((menu) => menu.menuId === activeHeaderMenuId) ?? null;
  const searchResults = useMemo(() => {
    const keyword = searchQuery.trim().toLocaleLowerCase("ko-KR");
    if (!keyword) return flattenMenuLeaves(visibleMenus).slice(0, 8);
    return flattenMenuLeaves(visibleMenus)
      .filter((item) => item.searchText.includes(keyword))
      .slice(0, 12);
  }, [searchQuery, visibleMenus]);

  const toggleTheme = () => {
    const next = !darkMode;
    setDarkMode(next);
    if (typeof document !== "undefined") {
      document.documentElement.classList.toggle("dark", next);
    }
  };

  const closeMobile = () => setSidebarOpen(false);

  return (
    <div className="flex min-h-screen w-full bg-lightgray text-link dark:bg-dark dark:text-white/90">
      {sidebarOpen ? (
        <button
          type="button"
          className="fixed inset-0 z-[2] bg-dark/55 backdrop-blur-sm xl:hidden"
          aria-label="모바일 메뉴 닫기"
          onClick={closeMobile}
        />
      ) : null}

      <aside
        aria-hidden={!sidebarOpen}
        className={`menu-sidebar fixed left-0 top-0 z-[3] h-full w-[270px] flex-shrink-0 border-r border-ld bg-white shadow-sm transition-all duration-200 ease-in dark:border-white/10 dark:bg-dark ${
          sidebarOpen ? "block" : "hidden"
        }`}
        onMouseEnter={() => miniSidebar && setMiniSidebar(false)}
      >
        <div
          className={`flex min-h-[70px] items-center overflow-hidden px-6 ${miniSidebar ? "xl:justify-center xl:px-4" : ""}`}
        >
          <a
            href="/admin/users"
            className="flex items-center gap-3"
            onClick={(event) =>
              navigateInsideApp(event, "/admin/users", closeMobile)
            }
          >
            <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl bg-primary text-sm font-bold text-white shadow-btn-shadow">
              KN
            </div>
            <div className={`min-w-0 ${miniSidebar ? "xl:hidden" : ""}`}>
              <p className="truncate text-sm font-bold text-dark dark:text-white">
                Common Foundation
              </p>
              <p className="truncate text-xs text-muted dark:text-white/50">
                시스템 관리 콘솔
              </p>
            </div>
          </a>
          <button
            type="button"
            className="ml-auto inline-flex h-9 w-9 items-center justify-center rounded-full text-lightmuted transition-colors hover:bg-lightprimary hover:text-primary xl:hidden"
            aria-label="닫기"
            onClick={closeMobile}
          >
            <X size={18} />
          </button>
        </div>

        <nav className="h-[calc(100vh_-_180px)] overflow-y-auto px-6 pb-4 no-scrollbar">
          <p
            className={`mt-6 py-1 text-xs font-bold uppercase leading-[26px] text-link dark:text-white/60 ${miniSidebar ? "xl:text-center" : ""}`}
          >
            {miniSidebar ? "•••" : "SYSTEM"}
          </p>
          {visibleMenus.length > 0 ? (
            visibleMenus.map((menu) => (
              <MenuNode
                key={menu.menuId}
                menu={menu}
                currentPath={currentPath}
                miniSidebar={miniSidebar}
                onNavigate={closeMobile}
              />
            ))
          ) : (
            <div className="mt-4 rounded-2xl border border-dashed border-ld bg-lightgray p-4 text-xs text-muted dark:border-white/10 dark:bg-white/5 dark:text-white/60">
              표시 가능한 메뉴가 없습니다.
            </div>
          )}
        </nav>

        <div
          className={`mx-6 my-4 overflow-hidden rounded-2xl bg-lightsecondary px-4 py-4 dark:bg-white/5 ${miniSidebar ? "xl:mx-4 xl:px-2" : ""}`}
        >
          <div className="flex items-center justify-between gap-3">
            <div className={`min-w-0 ${miniSidebar ? "xl:hidden" : ""}`}>
              <p className="truncate text-base font-semibold text-dark dark:text-white">
                {auth.user?.name ?? "Guest"}
              </p>
              <p className="truncate text-xs font-normal text-muted dark:text-white/55">
                {auth.user?.roles.includes("R09")
                  ? "R09 시스템관리자"
                  : (auth.user?.roles.join(", ") ?? "anonymous")}
              </p>
            </div>
            <button
              className="inline-flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-lightprimary text-primary transition-colors hover:bg-primary hover:text-white"
              type="button"
              aria-label="로그아웃"
              onClick={() => void auth.logout()}
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      <div className="page-wrapper flex w-full transition-all duration-200 ease-in">
        <div className="body-wrapper w-full bg-lightgray dark:bg-dark">
          <header className="sticky top-0 z-[2] border-b border-ld bg-white/95 px-5 py-4 shadow-sm backdrop-blur md:px-[30px] dark:border-white/10 dark:bg-dark/95">
            <div className="mx-auto flex items-center justify-between gap-4">
              <div className="flex min-w-0 items-center gap-2 text-link dark:text-white/80">
                <button
                  className="relative inline-flex h-10 w-10 items-center justify-center rounded-full text-link transition-colors after:absolute after:h-10 after:w-10 after:rounded-full after:bg-transparent hover:text-primary hover:after:bg-lightprimary dark:text-white/80 xl:hidden"
                  type="button"
                  aria-label="모바일 메뉴"
                  onClick={() => setSidebarOpen(true)}
                >
                  <Menu size={20} />
                </button>
                <button
                  className="relative hidden h-10 w-10 items-center justify-center rounded-full text-link transition-colors after:absolute after:h-10 after:w-10 after:rounded-full after:bg-transparent hover:text-primary hover:after:bg-lightprimary dark:text-white/80 xl:inline-flex"
                  type="button"
                  aria-label={miniSidebar ? "메뉴 펼치기" : "메뉴 접기"}
                  onClick={() => setMiniSidebar((value) => !value)}
                >
                  <Menu size={20} />
                </button>
                <nav
                  className="hidden min-w-0 items-center gap-1 lg:flex"
                  data-testid="header-menu-bar"
                  aria-label="헤더 주요 메뉴"
                  onMouseLeave={() => setActiveHeaderMenuId(null)}
                >
                  {visibleMenus.map((menu) => (
                    <div className="relative" key={menu.menuId}>
                      <button
                        type="button"
                        className={`inline-flex h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold ${
                          activeHeaderMenuId === menu.menuId
                            ? "bg-lightprimary text-primary"
                            : "text-link hover:bg-lightprimary hover:text-primary dark:text-white/80"
                        }`}
                        onMouseEnter={() => setActiveHeaderMenuId(menu.menuId)}
                        onFocus={() => setActiveHeaderMenuId(menu.menuId)}
                      >
                        {menu.menuName}
                        {menu.children?.length ? (
                          <ChevronDown size={14} />
                        ) : null}
                      </button>
                    </div>
                  ))}
                </nav>
                {activeHeaderMenu ? (
                  <div
                    className="absolute left-5 top-[68px] z-[4] hidden w-[min(900px,calc(100vw-2.5rem))] rounded-2xl border border-ld bg-white p-4 shadow-md lg:block dark:border-white/10 dark:bg-dark"
                    onMouseEnter={() =>
                      setActiveHeaderMenuId(activeHeaderMenu.menuId)
                    }
                    onMouseLeave={() => setActiveHeaderMenuId(null)}
                  >
                    <HeaderMegaMenu
                      menu={activeHeaderMenu}
                      onNavigate={() => setActiveHeaderMenuId(null)}
                    />
                  </div>
                ) : null}
                <button
                  className="hidden h-10 min-w-[240px] items-center justify-between rounded-xl border border-ld bg-white px-3 text-sm text-muted transition-colors hover:border-primary hover:text-primary md:inline-flex dark:border-white/10 dark:bg-white/5 dark:text-white/60"
                  type="button"
                  onClick={() => setSearchOpen((value) => !value)}
                >
                  <span className="inline-flex items-center gap-2">
                    <Search size={16} /> 메뉴 또는 화면 검색
                  </span>
                  <kbd className="rounded-md bg-lightgray px-2 py-1 text-[11px] text-lightmuted dark:bg-white/10 dark:text-white/60">
                    Ctrl K
                  </kbd>
                </button>
              </div>

              <div className="flex items-center gap-2">
                <IconCircleButton
                  label={darkMode ? "라이트 모드" : "다크 모드"}
                  onClick={toggleTheme}
                >
                  {darkMode ? <Sun size={18} /> : <Moon size={18} />}
                </IconCircleButton>
                <IconCircleButton label="알림">
                  <Bell size={18} />
                  <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-error" />
                </IconCircleButton>
                <div className="relative">
                  <button
                    className="inline-flex h-10 items-center gap-2 rounded-full px-2 text-sm font-semibold text-link transition-colors hover:bg-lightprimary hover:text-primary dark:text-white/80"
                    type="button"
                    onClick={() => setProfileOpen((value) => !value)}
                  >
                    <span className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-primary text-xs font-bold text-white">
                      {(auth.user?.name ?? "G").slice(0, 1)}
                    </span>
                    <span className="hidden max-w-[120px] truncate md:inline">
                      {auth.user?.name ?? "Guest"}
                    </span>
                    <ChevronDown size={16} />
                  </button>
                  {profileOpen ? (
                    <div className="absolute right-0 mt-3 w-64 overflow-hidden rounded-2xl border border-ld bg-white p-2 shadow-md dark:border-white/10 dark:bg-dark">
                      <div className="border-b border-ld p-3 dark:border-white/10">
                        <p className="font-semibold text-dark dark:text-white">
                          {auth.user?.name ?? "Guest"}
                        </p>
                        <p className="mt-1 text-xs text-muted dark:text-white/55">
                          {auth.user?.loginId ?? "anonymous"} ·{" "}
                          {auth.user?.roles.join(", ") ?? "-"}
                        </p>
                      </div>
                      <button
                        className="mt-2 flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-link transition-colors hover:bg-lightprimary hover:text-primary dark:text-white/80"
                        type="button"
                        onClick={() => void auth.logout()}
                      >
                        <LogOut size={16} /> 로그아웃
                      </button>
                    </div>
                  ) : null}
                </div>
              </div>
            </div>
            {searchOpen ? (
              <div className="mt-4 rounded-2xl border border-ld bg-white p-3 shadow-md dark:border-white/10 dark:bg-dark">
                <label className="flex items-center gap-3 rounded-xl bg-lightgray px-4 py-3 text-sm text-muted dark:bg-white/5 dark:text-white/60">
                  <Search size={18} />
                  <span className="sr-only">메뉴 검색어</span>
                  <input
                    data-testid="menu-search-input"
                    className="w-full border-0 bg-transparent p-0 text-sm text-link shadow-none outline-none placeholder:text-muted focus-visible:ring-0 dark:text-white/90"
                    value={searchQuery}
                    onChange={(event) => setSearchQuery(event.target.value)}
                    placeholder="메뉴명, 화면ID, 경로를 검색하세요"
                    autoFocus
                  />
                </label>
                <div className="mt-3 grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                  {searchResults.length > 0 ? (
                    searchResults.map((result) => (
                      <a
                        key={`${result.menu.menuId}-${result.menu.url}`}
                        href={result.menu.url ?? "#"}
                        className="rounded-xl border border-ld bg-white px-4 py-3 text-sm hover:border-primary hover:bg-lightprimary hover:text-primary dark:border-white/10 dark:bg-white/5 dark:text-white/80"
                        onClick={(event) => {
                          if (result.menu.url) {
                            navigateInsideApp(event, result.menu.url, () => {
                              setSearchOpen(false);
                              setSearchQuery("");
                            });
                          }
                        }}
                      >
                        <span className="block font-semibold">
                          {result.menu.menuName}
                        </span>
                        <span className="mt-1 block truncate text-xs text-muted dark:text-white/55">
                          {result.path.join(" > ")}
                        </span>
                      </a>
                    ))
                  ) : (
                    <p className="rounded-xl border border-dashed border-ld px-4 py-3 text-sm text-muted dark:border-white/10 dark:text-white/60">
                      검색 결과가 없습니다.
                    </p>
                  )}
                </div>
              </div>
            ) : null}
          </header>
          <main className="w-full px-5 py-[30px] md:px-[30px]">{children}</main>
        </div>
      </div>
    </div>
  );
}

function HeaderMegaMenu({
  menu,
  onNavigate,
}: {
  menu: MenuItem;
  onNavigate: () => void;
}) {
  const columns = menu.children?.length ? menu.children : [menu];
  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      {columns.map((column) => (
        <div key={column.menuId} className="min-w-0">
          <p className="px-2 text-xs font-bold uppercase tracking-wide text-muted dark:text-white/50">
            {column.menuName}
          </p>
          <div className="mt-2 space-y-1">
            {flattenMenuLeaves([column]).map((item) => (
              <a
                key={item.menu.menuId}
                href={item.menu.url ?? "#"}
                className="block rounded-xl px-3 py-2 text-sm font-semibold text-link hover:bg-lightprimary hover:text-primary dark:text-white/80"
                onClick={(event) => {
                  if (item.menu.url)
                    navigateInsideApp(event, item.menu.url, onNavigate);
                }}
              >
                <span className="block truncate">{item.menu.menuName}</span>
                <span className="mt-1 block truncate text-xs font-normal text-muted dark:text-white/50">
                  {item.menu.screenId ?? item.menu.url}
                </span>
              </a>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

type FlatMenuItem = {
  menu: MenuItem;
  path: string[];
  searchText: string;
};

function flattenMenuLeaves(
  menus: MenuItem[],
  parents: string[] = [],
): FlatMenuItem[] {
  return menus.flatMap((menu) => {
    const path = [...parents, menu.menuName];
    const current =
      menu.url != null
        ? [
            {
              menu,
              path,
              searchText: [
                menu.menuName,
                menu.screenId ?? "",
                menu.url ?? "",
                path.join(" "),
              ]
                .join(" ")
                .toLocaleLowerCase("ko-KR"),
            },
          ]
        : [];
    return [...current, ...flattenMenuLeaves(menu.children ?? [], path)];
  });
}

function IconCircleButton({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick?: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="relative inline-flex h-10 w-10 items-center justify-center rounded-full text-link transition-colors hover:bg-lightprimary hover:text-primary dark:text-white/80"
    >
      {children}
    </button>
  );
}

function MenuNode({
  menu,
  currentPath,
  miniSidebar,
  onNavigate,
}: {
  menu: MenuItem;
  currentPath: string;
  miniSidebar: boolean;
  onNavigate: () => void;
}) {
  const childActive = menu.children?.some((child) =>
    hasActivePath(child, currentPath),
  );
  const isActive = menu.url === currentPath;
  const hasChildren = Boolean(menu.children?.length);
  const [open, setOpen] = useState(childActive || !menu.parentMenuId);
  const activeClasses = isActive
    ? "bg-primary text-white shadow-btn-shadow hover:bg-primary hover:text-white dark:text-white"
    : childActive
      ? "bg-lightprimary text-primary dark:bg-primary/15 dark:text-primary"
      : "text-link hover:bg-lightprimary hover:text-primary dark:text-white/75 dark:hover:bg-white/5";

  const icon = renderMenuIcon(menu.icon);

  return (
    <div className="mt-1">
      <a
        className={`group flex w-full min-w-0 items-center gap-3 rounded-xl p-3 text-sm font-semibold transition-all duration-200 ease-in-out hover:translate-x-1 ${activeClasses}`}
        href={menu.url ?? "#"}
        onClick={(event) => {
          if (hasChildren && !menu.url) {
            event.preventDefault();
            setOpen((value) => !value);
            return;
          }
          if (menu.url) {
            navigateInsideApp(event, menu.url, onNavigate);
          }
        }}
        title={miniSidebar ? menu.menuName : undefined}
      >
        {icon}
        <span
          className={`max-w-36 flex-1 truncate leading-normal ${miniSidebar ? "xl:hidden" : ""}`}
        >
          {menu.menuName}
        </span>
        {hasChildren ? (
          <ChevronDown
            className={`transition-transform duration-200 ${open ? "rotate-180" : "rotate-0"} ${miniSidebar ? "xl:hidden" : ""}`}
            size={16}
          />
        ) : null}
      </a>
      {hasChildren && open && !miniSidebar ? (
        <div className="ml-4 flex flex-col border-l border-ld py-2 pl-3 dark:border-white/10">
          {menu.children.map((child) => (
            <MenuNode
              key={child.menuId}
              menu={child}
              currentPath={currentPath}
              miniSidebar={miniSidebar}
              onNavigate={onNavigate}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function renderMenuIcon(icon?: string | null) {
  const className = "flex-shrink-0 transition-colors duration-200";
  if (icon === "shield" || icon === "lock")
    return <Shield className={className} size={18} />;
  if (icon === "user" || icon === "users")
    return <UserCog className={className} size={18} />;
  if (icon === "profile" || icon === "account")
    return <UserCircle className={className} size={18} />;
  return <Settings className={className} size={18} />;
}

function hasActivePath(menu: MenuItem, currentPath: string): boolean {
  return (
    menu.url === currentPath ||
    menu.children.some((child) => hasActivePath(child, currentPath))
  );
}

function navigateInsideApp(
  event: React.MouseEvent<HTMLAnchorElement>,
  url: string,
  afterNavigate?: () => void,
) {
  if (!url.startsWith("/")) return;
  event.preventDefault();
  if (typeof window !== "undefined") {
    window.history.pushState({}, "", url);
    window.dispatchEvent(new PopStateEvent("popstate"));
  }
  afterNavigate?.();
}
