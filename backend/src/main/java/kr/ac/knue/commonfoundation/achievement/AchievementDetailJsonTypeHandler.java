package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Converts the schema's jsonb achievement_detail column without exposing database-specific values to the API. */
public class AchievementDetailJsonTypeHandler extends BaseTypeHandler<Map<String, Object>> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Map<String, Object> value, JdbcType jdbcType) throws SQLException {
        try { statement.setString(index, OBJECT_MAPPER.writeValueAsString(value)); }
        catch (Exception exception) { throw new SQLException("업적 상세 JSON을 저장할 수 없습니다.", exception); }
    }
    @Override public Map<String, Object> getNullableResult(ResultSet resultSet, String columnName) throws SQLException { return read(resultSet.getString(columnName)); }
    @Override public Map<String, Object> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException { return read(resultSet.getString(columnIndex)); }
    @Override public Map<String, Object> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException { return read(statement.getString(columnIndex)); }
    private Map<String, Object> read(String value) throws SQLException {
        if (value == null) return Map.of();
        try { return OBJECT_MAPPER.readValue(value, MAP_TYPE); }
        catch (Exception exception) { throw new SQLException("업적 상세 JSON을 읽을 수 없습니다.", exception); }
    }
}
