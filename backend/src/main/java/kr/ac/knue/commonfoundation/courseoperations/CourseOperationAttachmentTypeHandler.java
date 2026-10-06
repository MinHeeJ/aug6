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

/** Explicitly materializes the JSONB attachment array without relying on record generic inference. */
public class CourseOperationAttachmentTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, List<String> value, JdbcType type)
            throws SQLException {
        try {
            statement.setString(index, JSON.writeValueAsString(value));
        } catch (Exception exception) {
            throw new SQLException("첨부 참조를 변환하지 못했습니다.", exception);
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
        try {
            return JSON.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new SQLException("첨부 참조를 읽지 못했습니다.", exception);
        }
    }
}
