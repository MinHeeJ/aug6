package kr.ac.knue.commonfoundation.messages;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageManagementMapper {
    List<MessageCodeRow> listMessages(@Param("messageType") String messageType, @Param("messageCode") String messageCode,
            @Param("limit") int limit, @Param("offset") int offset);
    long countMessages(@Param("messageType") String messageType, @Param("messageCode") String messageCode);
    MessageCodeRow findMessage(@Param("messageCode") String messageCode);
    MessageTextResponse findMessageText(@Param("messageCode") String messageCode);
    void upsertMessage(@Param("messageCode") String messageCode, @Param("messageType") String messageType,
            @Param("userMessage") String userMessage, @Param("userId") Long userId, @Param("changeReason") String changeReason);
}
