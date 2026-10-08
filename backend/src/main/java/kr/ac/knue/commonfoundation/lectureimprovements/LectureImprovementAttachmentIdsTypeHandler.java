package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Explicit JSON collection materialization for the lecture-improvement attachment references. */
public class LectureImprovementAttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> values, JdbcType type)
            throws SQLException {
        try {
            statement.setString(index, JSON.writeValueAsString(values));
        } catch (java.io.IOException exception) {
            throw new SQLException("첨부참조 직렬화 실패", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet result, String column) throws SQLException {
        return parse(result.getString(column));
    }

    @Override
    public List<String> getNullableResult(ResultSet result, int column) throws SQLException {
        return parse(result.getString(column));
    }

    @Override
    public List<String> getNullableResult(CallableStatement result, int column) throws SQLException {
        return parse(result.getString(column));
    }

    private List<String> parse(String value) throws SQLException {
        if (value == null) return List.of();
        try {
            return JSON.readValue(value, new TypeReference<List<String>>() {});
        } catch (java.io.IOException exception) {
            throw new SQLException("첨부참조 JSON 형식 오류", exception);
        }
    }
}
