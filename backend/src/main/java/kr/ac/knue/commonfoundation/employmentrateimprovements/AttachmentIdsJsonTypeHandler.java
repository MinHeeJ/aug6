package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Materializes the JSONB attachment-id array into the read model's typed string list.
 *
 * <p>Malformed legacy values and non-array values are treated as an empty attachment list so a
 * single corrupt value cannot prevent an achievement list or detail response from loading.
 */
public class AttachmentIdsJsonTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            List<String> attachmentIds,
            JdbcType jdbcType) throws SQLException {
        try {
            statement.setObject(index, OBJECT_MAPPER.writeValueAsString(attachmentIds), Types.OTHER);
        } catch (JsonProcessingException exception) {
            throw new SQLException("첨부 식별자 목록을 JSON으로 변환할 수 없습니다.", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return deserialize(resultSet.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return deserialize(resultSet.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return deserialize(statement.getString(columnIndex));
    }

    private List<String> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode values = OBJECT_MAPPER.readTree(json);
            if (values == null || !values.isArray()) {
                return List.of();
            }
            List<String> attachmentIds = new ArrayList<>();
            for (JsonNode value : values) {
                if (value.isTextual() && !value.textValue().isBlank()) {
                    attachmentIds.add(value.textValue());
                }
            }
            return List.copyOf(attachmentIds);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
