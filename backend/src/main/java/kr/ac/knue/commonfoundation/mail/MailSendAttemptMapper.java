package kr.ac.knue.commonfoundation.mail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MailSendAttemptMapper {
    long insertRequested(@Param("userId") Long userId,
                         @Param("email") String email,
                         @Param("mailType") String mailType,
                         @Param("requestId") String requestId);

    int markSent(@Param("attemptId") long attemptId);

    int markFailed(@Param("attemptId") long attemptId, @Param("failureReason") String failureReason);
}
