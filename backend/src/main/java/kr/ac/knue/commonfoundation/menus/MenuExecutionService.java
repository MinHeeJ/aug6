package kr.ac.knue.commonfoundation.menus;

import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuExecutionService {
    private final MenuExecutionMapper mapper;

    public MenuExecutionService(MenuExecutionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public MenuExecutionRow getMenuExecution(Long menuId) {
        MenuExecutionRow row = mapper.findExecution(menuId);
        if (row == null) {
            throw new BusinessValidationException("메뉴 실행정보 조회 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("menuId", "존재하지 않는 메뉴입니다.")));
        }
        return row;
    }

    @Transactional
    public MenuExecutionRow updateMenuExecution(Long menuId, MenuExecutionRequest request, Long adminUserId) {
        List<ValidationError> fields = validate(menuId, request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("메뉴 실행정보 저장 요청이 올바르지 않습니다.", fields);
        }
        mapper.updateMenuExecutionFields(menuId, request.menuName(), request.screenId(), request.url(), request.icon(),
                request.businessCategory(), request.description(), adminUserId, request.changeReason());
        mapper.upsertMenuExecutionInfo(menuId, request.screenId(), request.url(), request.icon(), request.businessCategory(),
                request.description(), adminUserId);
        return mapper.findExecution(menuId);
    }

    private List<ValidationError> validate(Long menuId, MenuExecutionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (mapper.existsActiveMenu(menuId) == 0) {
            fields.add(new ValidationError("menuId", "존재하지 않는 메뉴입니다."));
        }
        if (hasText(request.url()) && (!request.url().startsWith("/") || request.url().startsWith("//"))) {
            fields.add(new ValidationError("url", "실행 URL은 / 로 시작하는 상대경로여야 합니다."));
        }
        if (hasText(request.screenId()) && mapper.countActiveScreenIdExceptMenu(request.screenId(), menuId) > 0) {
            fields.add(new ValidationError("screenId", "다른 메뉴에서 사용 중인 화면ID입니다."));
        }
        return fields;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
