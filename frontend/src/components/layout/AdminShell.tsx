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
  const [searchKeyword, setSearchKeyword] = useState("");
  const [activeHeaderMenuId, setActiveHeaderMenuId] = useState<number | null>(
    null,
  );
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

  const toggleTheme = () => {
    const next = !darkMode;
    setDarkMode(next);
    if (typeof document !== "undefined") {
      document.documentElement.classList.toggle("dark", next);
    }
  };

  const closeMobile = () => setSidebarOpen(false);
  const closeHeaderNavigation = () => setActiveHeaderMenuId(null);
  const closeMenuSearch = () => {
    setSearchOpen(false);
    setSearchKeyword("");
  };

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
        className={`menu-sidebar fixed left-0 top-0 z-[3] h-full flex-shrink-0 border-r border-ld bg-white shadow-sm transition-all duration-200 ease-in dark:border-white/10 dark:bg-dark xl:block ${
          sidebarOpen ? "block" : "hidden"
        } ${miniSidebar ? "xl:w-[90px]" : "xl:w-[270px]"} w-[270px]`}
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

      <div
        className={`page-wrapper flex w-full transition-all duration-200 ease-in ${
          miniSidebar ? "xl:ml-[90px]" : "xl:ml-[270px]"
        }`}
      >
        <div className="body-wrapper w-full bg-lightgray dark:bg-dark">
          <header className="sticky top-0 z-[2] border-b border-ld bg-white/95 px-5 py-4 shadow-sm backdrop-blur md:px-[30px] dark:border-white/10 dark:bg-dark/95">
            <div className="mx-auto flex items-center justify-between gap-4">
              <div className="flex min-w-0 flex-1 items-center gap-2 text-link dark:text-white/80">
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
                <button
                  className="hidden h-10 min-w-[240px] items-center justify-between rounded-xl border border-ld bg-white px-3 text-sm text-muted transition-colors hover:border-primary hover:text-primary md:inline-flex dark:border-white/10 dark:bg-white/5 dark:text-white/60"
                  type="button"
                  onClick={() => setSearchOpen((value) => !value)}
                  data-testid="header-menu-search-toggle"
                >
                  <span className="inline-flex items-center gap-2">
                    <Search size={16} /> 메뉴 또는 화면 검색
                  </span>
                  <kbd className="rounded-md bg-lightgray px-2 py-1 text-[11px] text-lightmuted dark:bg-white/10 dark:text-white/60">
                    Ctrl K
                  </kbd>
                </button>
                <HeaderNavigation
                  menus={visibleMenus}
                  activeMenuId={activeHeaderMenuId}
                  currentPath={currentPath}
                  onOpen={setActiveHeaderMenuId}
                  onClose={closeHeaderNavigation}
                />
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
              <MenuSearchPanel
                menus={visibleMenus}
                keyword={searchKeyword}
                onKeywordChange={setSearchKeyword}
                onNavigate={closeMenuSearch}
              />
            ) : null}
          </header>
          <main className="w-full px-5 py-[30px] md:px-[30px]">{children}</main>
        </div>
      </div>
    </div>
  );
}

type MenuSearchResult = {
  menu: MenuItem;
  menuPath: string;
};

function MenuSearchPanel({
  menus,
  keyword,
  onKeywordChange,
  onNavigate,
}: {
  menus: MenuItem[];
  keyword: string;
  onKeywordChange: (keyword: string) => void;
  onNavigate: () => void;
}) {
  const normalizedKeyword = keyword.trim().toLocaleLowerCase("ko-KR");
  const results = useMemo(() => {
    if (!normalizedKeyword) {
      return [];
    }
    return collectSearchableMenus(menus)
      .filter(({ menu, menuPath }) =>
        [menu.menuName, menuPath, menu.screenId ?? "", menu.url ?? ""]
          .join(" ")
          .toLocaleLowerCase("ko-KR")
          .includes(normalizedKeyword),
      )
      .slice(0, 12);
  }, [menus, normalizedKeyword]);

  return (
    <section
      className="mt-4 rounded-2xl border border-ld bg-white p-3 shadow-md dark:border-white/10 dark:bg-dark"
      data-testid="common-menu-search"
      aria-label="메뉴 검색"
    >
      <label className="flex items-center gap-3 rounded-xl border border-ld bg-lightgray px-4 py-3 text-sm text-muted focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
        <Search size={18} />
        <span className="sr-only">메뉴 검색어</span>
        <input
          type="search"
          value={keyword}
          onChange={(event) => onKeywordChange(event.target.value)}
          className="min-w-0 flex-1 border-0 bg-transparent p-0 text-sm font-semibold text-link outline-none placeholder:text-muted focus:ring-0 dark:text-white/90 dark:placeholder:text-white/35"
          placeholder="메뉴명 또는 경로 입력"
          data-testid="header-menu-search-input"
          autoFocus
        />
      </label>

      {normalizedKeyword ? (
        <div
          className="mt-3 overflow-hidden rounded-xl border border-ld bg-white dark:border-white/10 dark:bg-white/[0.03]"
          data-testid="header-menu-search-results"
          role="listbox"
          aria-label="메뉴 검색 결과"
        >
          {results.length > 0 ? (
            results.map(({ menu, menuPath }) => (
              <a
                key={menu.menuId}
                href={menu.url ?? "#"}
                className="flex items-center justify-between gap-3 border-b border-ld px-4 py-3 text-sm last:border-b-0 hover:bg-lightprimary hover:text-primary dark:border-white/10 dark:text-white/75 dark:hover:bg-primary/10"
                data-testid={`header-menu-search-result-${menu.menuId}`}
                onClick={(event) => {
                  if (menu.url) {
                    navigateInsideApp(event, menu.url, onNavigate);
                  }
                }}
              >
                <span className="min-w-0">
                  <span className="block font-bold text-dark dark:text-white">
                    {menu.menuName}
                  </span>
                  <span className="mt-1 block truncate text-xs text-muted dark:text-white/55">
                    {menuPath}
                  </span>
                </span>
                {menu.screenId ? (
                  <span className="flex-shrink-0 rounded-full bg-lightsecondary px-2 py-1 text-[10px] font-bold text-lightmuted dark:bg-white/5 dark:text-white/45">
                    {menu.screenId}
                  </span>
                ) : null}
              </a>
            ))
          ) : (
            <p className="px-4 py-3 text-sm text-muted dark:text-white/60">
              접근 가능한 메뉴 검색 결과가 없습니다.
            </p>
          )}
        </div>
      ) : (
        <p className="mt-3 rounded-xl bg-lightgray px-4 py-3 text-sm text-muted dark:bg-white/5 dark:text-white/60">
          검색어를 입력하면 접근 가능한 실제 메뉴만 표시됩니다.
        </p>
      )}
    </section>
  );
}

function HeaderNavigation({
  menus,
  activeMenuId,
  currentPath,
  onOpen,
  onClose,
}: {
  menus: MenuItem[];
  activeMenuId: number | null;
  currentPath: string;
  onOpen: (menuId: number) => void;
  onClose: () => void;
}) {
  const orderedMenus = useMemo(() => sortMenus(menus), [menus]);
  const activeMenu =
    orderedMenus.find((menu) => menu.menuId === activeMenuId) ?? null;
  const panelGroups = activeMenu
    ? activeMenu.children.length > 0
      ? sortMenus(activeMenu.children)
      : [activeMenu]
    : [];

  if (orderedMenus.length === 0) {
    return null;
  }

  return (
    <nav
      className="relative hidden min-w-0 flex-1 items-center xl:flex"
      aria-label="헤더 메뉴"
      data-testid="common-header-nav"
      onMouseLeave={onClose}
    >
      <div className="flex min-w-0 items-center gap-1 overflow-x-auto no-scrollbar px-2">
        {orderedMenus.map((menu) => {
          const active = hasActivePath(menu, currentPath);
          return (
            <button
              key={menu.menuId}
              type="button"
              className={`inline-flex h-10 flex-shrink-0 items-center gap-2 rounded-xl px-3 text-sm font-semibold ${
                active || activeMenuId === menu.menuId
                  ? "bg-lightprimary text-primary dark:bg-primary/15 dark:text-primary"
                  : "text-link hover:bg-lightprimary hover:text-primary dark:text-white/75 dark:hover:bg-white/5"
              }`}
              data-testid={`header-nav-top-${menu.menuId}`}
              aria-expanded={activeMenuId === menu.menuId}
              aria-haspopup="menu"
              onMouseEnter={() => onOpen(menu.menuId)}
              onFocus={() => onOpen(menu.menuId)}
            >
              <span className="max-w-[150px] truncate">{menu.menuName}</span>
              <ChevronDown size={14} />
            </button>
          );
        })}
      </div>
      {activeMenu ? (
        <div
          className="absolute left-2 top-full mt-3 w-[min(760px,calc(100vw_-_360px))] rounded-2xl border border-ld bg-white p-4 shadow-md dark:border-white/10 dark:bg-dark"
          data-testid="header-nav-panel"
          role="menu"
          onMouseEnter={() => onOpen(activeMenu.menuId)}
        >
          <div className="mb-3 flex items-center justify-between border-b border-ld pb-3 dark:border-white/10">
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-primary">
                Header Navigation
              </p>
              <h2 className="mt-1 text-base font-bold text-dark dark:text-white">
                {activeMenu.menuName}
              </h2>
            </div>
            <span className="rounded-full bg-lightsecondary px-3 py-1 text-xs font-semibold text-muted dark:bg-white/5 dark:text-white/55">
              hover 메뉴
            </span>
          </div>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {panelGroups.map((middle) => (
              <div
                key={middle.menuId}
                className="rounded-xl border border-ld bg-lightgray/60 p-3 dark:border-white/10 dark:bg-white/5"
                data-testid={`header-nav-middle-${middle.menuId}`}
              >
                {middle.url ? (
                  <HeaderLeafLink menu={middle} onNavigate={onClose} />
                ) : (
                  <p className="text-sm font-bold text-dark dark:text-white">
                    {middle.menuName}
                  </p>
                )}
                {middle.children.length > 0 ? (
                  <div className="mt-3 space-y-1">
                    {sortMenus(middle.children)
                      .flatMap((child) => collectLeafMenus(child))
                      .map((leaf) => (
                        <HeaderLeafLink
                          key={leaf.menuId}
                          menu={leaf}
                          onNavigate={onClose}
                        />
                      ))}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </nav>
  );
}

function HeaderLeafLink({
  menu,
  onNavigate,
}: {
  menu: MenuItem;
  onNavigate: () => void;
}) {
  return (
    <a
      href={menu.url ?? "#"}
      className="flex items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm font-semibold text-link hover:bg-white hover:text-primary dark:text-white/75 dark:hover:bg-dark"
      data-testid={`header-nav-leaf-${menu.menuId}`}
      aria-label={menu.menuName}
      onClick={(event) => {
        if (menu.url) {
          navigateInsideApp(event, menu.url, onNavigate);
        }
      }}
    >
      <span className="truncate">{menu.menuName}</span>
      {menu.screenId ? (
        <span className="rounded-full bg-white px-2 py-0.5 text-[10px] font-bold text-lightmuted dark:bg-dark dark:text-white/45">
          {menu.screenId}
        </span>
      ) : null}
    </a>
  );
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

function sortMenus(menus: MenuItem[]): MenuItem[] {
  return [...menus].sort(
    (left, right) => left.displayOrder - right.displayOrder,
  );
}

function collectLeafMenus(menu: MenuItem): MenuItem[] {
  const childLeaves = sortMenus(menu.children).flatMap((child) =>
    collectLeafMenus(child),
  );
  return menu.url ? [menu, ...childLeaves] : childLeaves;
}

function collectSearchableMenus(
  menus: MenuItem[],
  ancestorNames: string[] = [],
): MenuSearchResult[] {
  return sortMenus(menus).flatMap((menu) => {
    const menuPath = [...ancestorNames, menu.menuName].join(" > ");
    const current = menu.url ? [{ menu, menuPath }] : [];
    return [
      ...current,
      ...collectSearchableMenus(menu.children, [
        ...ancestorNames,
        menu.menuName,
      ]),
    ];
  });
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
