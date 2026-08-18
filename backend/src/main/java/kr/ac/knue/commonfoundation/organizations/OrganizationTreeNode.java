package kr.ac.knue.commonfoundation.organizations;

import java.util.ArrayList;
import java.util.List;

public record OrganizationTreeNode(
        String organizationCode,
        String organizationName,
        String organizationType,
        String systemUseYn,
        String status,
        String parentOrganizationCode,
        List<OrganizationTreeNode> children) {
    public static OrganizationTreeNode from(OrganizationRow row) {
        return new OrganizationTreeNode(
                row.organizationCode(),
                row.organizationName(),
                row.organizationType(),
                row.systemUseYn(),
                row.status(),
                row.parentOrganizationCode(),
                new ArrayList<>());
    }
}
