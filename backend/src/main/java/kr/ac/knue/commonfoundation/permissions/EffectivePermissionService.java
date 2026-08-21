package kr.ac.knue.commonfoundation.permissions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EffectivePermissionService {
    private final PermissionMapper permissionMapper;

    public EffectivePermissionService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public boolean canAccess(Long userId, List<String> roles, String path) {
        if (permissionMapper == null || roles == null || roles.isEmpty()) {
            return false;
        }
        if (path != null && path.startsWith("/admin/") && permissionMapper.countVisibleMenuForPath(path) == 0) {
            return false;
        }
        if (roles.contains("R09")) {
            return true;
        }
        return resolveAllowed(permissionMapper.findRulesForPath(userId, path, roleCsv(roles)));
    }

    public boolean resolveAllowed(List<PermissionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        List<PermissionRule> userRules = rules.stream().filter(rule -> "USER".equals(rule.targetType())).toList();
        if (!userRules.isEmpty()) {
            return allowedAtPriority(userRules);
        }
        List<PermissionRule> organizationRules = rules.stream().filter(rule -> "ORGANIZATION".equals(rule.targetType())).toList();
        if (!organizationRules.isEmpty()) {
            return allowedAtPriority(organizationRules);
        }
        List<PermissionRule> roleRules = rules.stream().filter(rule -> "ROLE".equals(rule.targetType())).toList();
        return allowedAtPriority(roleRules);
    }

    private boolean allowedAtPriority(List<PermissionRule> rules) {
        return rules.stream().noneMatch(rule -> "DENY".equals(rule.accessAllowed()))
                && rules.stream().anyMatch(rule -> "ALLOW".equals(rule.accessAllowed()));
    }

    public List<MenuItem> visibleMenus(Long userId, List<String> roles) {
        if (permissionMapper == null) {
            return List.of();
        }
        List<MenuRow> allMenus = permissionMapper.findActiveMenus();
        Set<Long> visibleIds = allMenus.stream()
                .filter(row -> row.url() == null || (roles != null && roles.contains("R09")) || resolveAllowed(permissionMapper.findRulesForMenu(userId, row.menuId(), roleCsv(roles))))
                .map(MenuRow::menuId)
                .collect(Collectors.toSet());
        Map<Long, MenuItem> byId = new LinkedHashMap<>();
        for (MenuRow row : allMenus) {
            if (row.url() == null || visibleIds.contains(row.menuId())) {
                byId.put(row.menuId(), MenuItem.leaf(row));
            }
        }
        List<MenuItem> roots = new ArrayList<>();
        for (MenuItem item : byId.values()) {
            if (item.parentMenuId() != null && byId.containsKey(item.parentMenuId())) {
                byId.get(item.parentMenuId()).children().add(item);
            } else {
                roots.add(item);
            }
        }
        return prune(roots).stream().sorted(Comparator.comparingInt(MenuItem::displayOrder)).toList();
    }

    private List<MenuItem> prune(List<MenuItem> items) {
        return items.stream()
                .map(item -> item.withChildren(prune(item.children())))
                .filter(item -> item.url() != null || !item.children().isEmpty())
                .sorted(Comparator.comparingInt(MenuItem::displayOrder))
                .toList();
    }

    private String roleCsv(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "''";
        }
        return roles.stream().map(role -> "'" + role.replace("'", "''") + "'").collect(Collectors.joining(","));
    }
}
