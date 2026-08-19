package kr.ac.knue.commonfoundation.menus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuStructureManagementService {
    private final MenuStructureMapper mapper;

    public MenuStructureManagementService(MenuStructureMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MenuTreeNode> getMenuTree(String filter) {
        return buildTree(mapper.findMenusForTree(blankToNull(filter)));
    }

    @Transactional
    public MenuTreeNode updateMenuParent(Long menuId, MenuParentUpdateRequest request, Long currentUserId) {
        validateParentChange(menuId, request.parentMenuId());
        mapper.updateParent(menuId, request.parentMenuId(), currentUserId, request.changeReason());
        MenuTreeRow row = mapper.findMenu(menuId);
        if (row == null) {
            throw validation("메뉴를 찾을 수 없습니다.", "menuId", "존재하지 않는 메뉴입니다.");
        }
        return MenuTreeNode.from(row);
    }

    @Transactional
    public List<MenuTreeNode> reorderMenus(MenuReorderRequest request, Long currentUserId) {
        List<ValidationError> fields = validateReorder(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("메뉴 표시순서 재정렬 요청이 올바르지 않습니다.", fields);
        }
        int order = 1;
        for (Long menuId : request.orderedMenuIds()) {
            mapper.updateDisplayOrder(menuId, request.parentMenuId(), order, currentUserId, request.changeReason());
            order += 1;
        }
        return mapper.findMenusByParent(request.parentMenuId()).stream()
                .map(MenuTreeNode::from)
                .sorted(Comparator.comparingInt(MenuTreeNode::displayOrder).thenComparing(MenuTreeNode::menuId))
                .toList();
    }

    private void validateParentChange(Long menuId, Long parentMenuId) {
        List<ValidationError> fields = new ArrayList<>();
        if (mapper.existsMenu(menuId) == 0) {
            fields.add(new ValidationError("menuId", "존재하지 않는 메뉴입니다."));
        }
        if (parentMenuId != null && mapper.existsMenu(parentMenuId) == 0) {
            fields.add(new ValidationError("parentMenuId", "존재하지 않는 부모 메뉴입니다."));
        }
        if (parentMenuId != null && menuId.equals(parentMenuId)) {
            fields.add(new ValidationError("parentMenuId", "메뉴 자기 자신은 부모 메뉴가 될 수 없습니다."));
        }
        if (parentMenuId != null && mapper.isDescendant(menuId, parentMenuId) > 0) {
            fields.add(new ValidationError("parentMenuId", "하위 메뉴를 부모로 지정할 수 없어 순환 구조가 차단되었습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("메뉴 부모 변경 요청이 올바르지 않아 순환 또는 자기부모 구조가 차단되었습니다.", fields);
        }
    }

    private List<ValidationError> validateReorder(MenuReorderRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        Set<Long> uniqueMenuIds = new HashSet<>(request.orderedMenuIds());
        if (uniqueMenuIds.size() != request.orderedMenuIds().size()) {
            fields.add(new ValidationError("orderedMenuIds", "중복된 메뉴 ID는 정렬할 수 없습니다."));
            return fields;
        }
        if (request.parentMenuId() != null && mapper.existsMenu(request.parentMenuId()) == 0) {
            fields.add(new ValidationError("parentMenuId", "존재하지 않는 부모 메뉴입니다."));
        }
        int siblingCount = mapper.countSiblingsUnderParent(request.parentMenuId(), request.orderedMenuIds());
        if (siblingCount != request.orderedMenuIds().size()) {
            fields.add(new ValidationError("orderedMenuIds", "선택한 부모 메뉴의 직계 하위 메뉴만 재정렬할 수 있습니다."));
        }
        return fields;
    }

    private List<MenuTreeNode> buildTree(List<MenuTreeRow> rows) {
        Map<Long, MenuTreeNode> byId = new LinkedHashMap<>();
        for (MenuTreeRow row : rows) {
            byId.put(row.menuId(), MenuTreeNode.from(row));
        }
        List<MenuTreeNode> roots = new ArrayList<>();
        for (MenuTreeNode node : byId.values()) {
            if (node.parentMenuId() != null && byId.containsKey(node.parentMenuId())) {
                byId.get(node.parentMenuId()).children().add(node);
            } else {
                roots.add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<MenuTreeNode> nodes) {
        nodes.sort(Comparator.comparingInt(MenuTreeNode::displayOrder).thenComparing(MenuTreeNode::menuId));
        for (MenuTreeNode node : nodes) {
            sortTree(node.children());
        }
    }

    private BusinessValidationException validation(String message, String field, String fieldMessage) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, fieldMessage)));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
