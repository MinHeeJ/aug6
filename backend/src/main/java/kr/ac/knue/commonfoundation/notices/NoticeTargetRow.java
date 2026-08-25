package kr.ac.knue.commonfoundation.notices;

public record NoticeTargetRow(Long targetId, Long noticeId, String targetType, String targetIdValue, String targetName) {
}
