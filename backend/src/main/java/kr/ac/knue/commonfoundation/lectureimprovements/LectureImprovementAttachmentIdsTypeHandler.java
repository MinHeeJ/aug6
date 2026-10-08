package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Converts the header JSON array without exposing JDBC JSON objects in API rows. */
public class LectureImprovementAttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType type)
            throws SQLException {
        try {
            statement.setString(index, JSON.writeValueAsString(value));
        } catch (Exception exception) {
            throw new SQLException("Invalid attachment references", exception);
        }
    }

    private List<String> decode(String value) throws SQLException {
        try {
            return value == null ? List.of() : JSON.readValue(value, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw new SQLException("Invalid attachment references", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet result, String column) throws SQLException {
        return decode(result.getString(column));
    }

    @Override
    public List<String> getNullableResult(ResultSet result, int column) throws SQLException {
        return decode(result.getString(column));
    }

    @Override
    public List<String> getNullableResult(CallableStatement statement, int column) throws SQLException {
        return decode(statement.getString(column));
    }
}
