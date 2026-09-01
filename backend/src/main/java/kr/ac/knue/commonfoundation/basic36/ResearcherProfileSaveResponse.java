package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record ResearcherProfileSaveResponse(ResearcherProfileDetail profile, List<String> warnings) {
}
