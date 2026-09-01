package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record ResearcherProfileTabSaveRequest(List<ResearcherProfileTabItem> items, String changeReason) {
}
