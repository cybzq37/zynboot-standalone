package com.zynboot.infra.mybatis.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL geometry 字段类型处理器。
 * <p>
 * 将 Java {@link String}（WKT/EWKT 格式）与 PostgreSQL {@code geometry} 列相互映射，
 * 写入时通过 {@link org.postgresql.util.PGobject} 设置类型为 {@code geometry}。
 */
@MappedTypes(value = {String.class})
public class PgGeometryTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        PGobject geometry = new PGobject();
        geometry.setType("geometry");
        geometry.setValue(parameter);
        ps.setObject(i, geometry);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null || value.isBlank() ? null : value;
    }
}
