package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Decodes this feature's attachment reference list without registering a global List handler. */
public class CourseOperationAttachmentIdsTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType type)
            throws SQLException {
        try {
            statement.setString(index, JSON.writeValueAsString(value));
        } catch (Exception exception) {
            throw new SQLException("Invalid attachment reference list", exception);
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
    public List<String> getNullableResult(CallableStatement result, int column) throws SQLException {
        return decode(result.getString(column));
    }

    private List<String> decode(String value) throws SQLException {
        if (value == null || value.isBlank()) return java.util.Collections.emptyList();
        try {
            return JSON.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new SQLException("Invalid persisted attachment reference list", exception);
        }
    }
}
