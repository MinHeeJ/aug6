package kr.ac.knue.commonfoundation.educationachievement;

import kr.ac.knue.commonfoundation.common.api.ConflictException;

/**
 * Signals that a final-evaluation-confirmed achievement cannot be changed by any role.
 */
public class ConfirmedDataLockedException extends ConflictException {
    public ConfirmedDataLockedException() {
        super("평가확정된 실적은 변경할 수 없습니다.");
    }
}
