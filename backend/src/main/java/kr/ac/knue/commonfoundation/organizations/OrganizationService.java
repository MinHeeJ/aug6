package kr.ac.knue.commonfoundation.organizations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    public List<OrganizationRow> searchOrganizations(String organizationCodeFilter, int page, int size) {
        return organizationMapper.searchOrganizations(blankToNull(organizationCodeFilter), Math.max(page, 0) * normalizeSize(size), normalizeSize(size));
    }

    public List<OrganizationTreeNode> getOrganizationTree() {
        List<OrganizationRow> rows = organizationMapper.findOrganizationsForTree();
        Map<String, OrganizationTreeNode> byCode = new LinkedHashMap<>();
        for (OrganizationRow row : rows) {
            byCode.put(row.organizationCode(), OrganizationTreeNode.from(row));
        }
        List<OrganizationTreeNode> roots = new ArrayList<>();
        for (OrganizationTreeNode node : byCode.values()) {
            if (node.parentOrganizationCode() != null && byCode.containsKey(node.parentOrganizationCode())) {
                byCode.get(node.parentOrganizationCode()).children().add(node);
            } else {
                roots.add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    @Transactional
    public OrganizationRow saveParentRelation(String organizationCode, OrganizationParentRelationRequest request, Long currentUserId) {
        validateDates(request);
        if (organizationCode.equals(request.parentOrganizationCode())) {
            throw validation("자기 자신을 상위조직으로 지정할 수 없습니다.", "parentOrganizationCode", "자기 자신은 상위조직이 될 수 없습니다.");
        }
        OrganizationRow child = organizationMapper.findOrganization(organizationCode);
        if (child == null) {
            throw validation("조직을 찾을 수 없습니다.", "organizationCode", "존재하지 않는 조직입니다.");
        }
        OrganizationRow parent = organizationMapper.findOrganization(request.parentOrganizationCode());
        if (parent == null) {
            throw validation("상위조직을 찾을 수 없습니다.", "parentOrganizationCode", "존재하지 않는 상위조직입니다.");
        }
        OrganizationRelationRow current = organizationMapper.findCurrentRelation(organizationCode);
        Long excludeRelationId = current == null ? null : current.relationId();
        OrganizationRelationRow overlap = organizationMapper.findOverlappingRelation(
                organizationCode,
                request.effectiveStartDate(),
                request.effectiveEndDate(),
                excludeRelationId);
        if (overlap != null) {
            throw validation("동일 조직의 상위관계 적용기간이 중복됩니다.", "effectiveStartDate", "기존 관계 적용기간과 중복됩니다.");
        }
        OrganizationRelationRow before = current == null
                ? new OrganizationRelationRow(null, organizationCode, null, null, null, null, null)
                : current;
        if (current != null) {
            LocalDate endDate = request.effectiveStartDate().minusDays(1);
            if (endDate.isBefore(current.effectiveStartDate())) {
                endDate = current.effectiveStartDate();
            }
            organizationMapper.endRelation(current.relationId(), endDate, currentUserId, request.changeReason());
        }
        organizationMapper.insertRelation(
                organizationCode,
                request.parentOrganizationCode(),
                request.effectiveStartDate(),
                request.effectiveEndDate(),
                currentUserId,
                request.changeReason());
        OrganizationRelationRow latest = organizationMapper.findLatestRelation(organizationCode);
        organizationMapper.insertHistory(before, latest, organizationCode, currentUserId, request.changeReason());
        return withRelation(child, latest);
    }

    public List<OrganizationRelationHistoryRow> listHistory(String organizationCode, int page, int size) {
        return organizationMapper.findRelationHistory(organizationCode, Math.max(page, 0) * normalizeSize(size), normalizeSize(size));
    }

    private void validateDates(OrganizationParentRelationRequest request) {
        if (request.effectiveEndDate() != null && request.effectiveEndDate().isBefore(request.effectiveStartDate())) {
            throw validation("적용 종료일은 시작일보다 빠를 수 없습니다.", "effectiveEndDate", "적용 종료일은 시작일 이후여야 합니다.");
        }
    }

    private OrganizationRow withRelation(OrganizationRow organization, OrganizationRelationRow relation) {
        if (relation == null) {
            return organization;
        }
        return new OrganizationRow(
                organization.organizationCode(),
                organization.organizationName(),
                organization.organizationType(),
                organization.systemUseYn(),
                organization.status(),
                relation.parentOrganizationCode(),
                relation.effectiveStartDate(),
                relation.effectiveEndDate(),
                organization.updatedAt());
    }

    private BusinessValidationException validation(String message, String field, String fieldMessage) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, fieldMessage)));
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void sortTree(List<OrganizationTreeNode> nodes) {
        nodes.sort(Comparator.comparing(OrganizationTreeNode::organizationCode));
        for (OrganizationTreeNode node : nodes) {
            sortTree(node.children());
        }
    }
}
