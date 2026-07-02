package com.zynboot.infra.mybatis.handler;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL JSONB 字段类型处理器。
 * <p>
 * 使用 Jackson 将 Java 对象与 PostgreSQL {@code jsonb} 列相互映射，
 * 写入时通过 {@link PGobject} 设置类型为 {@code jsonb}。
 */
@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class PgJsonbTypeHandler<T> extends BaseTypeHandler<T> {

    private static final Log log = LogFactory.getLog(PgJsonbTypeHandler.class);

    private final Class<?> type;
    private final Type genericType;

    public PgJsonbTypeHandler(Class<?> clazz) {
        this.type = clazz;
        this.genericType = clazz;
    }

    /** 自 3.5.6 版本开始支持泛型，需要加上此构造。 */
    public PgJsonbTypeHandler(Class<?> type, Field field) {
        this.type = type;
        this.genericType = field.getGenericType();
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        PGobject jsonbObject = new PGobject();
        jsonbObject.setType("jsonb");
        jsonbObject.setValue(JacksonHandlerUtils.toJson(parameter, log));
        ps.setObject(i, jsonbObject);
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return json == null || json.isBlank() ? null : parse(json);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return json == null || json.isBlank() ? null : parse(json);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return json == null || json.isBlank() ? null : parse(json);
    }

    private T parse(String json) {
        return JacksonHandlerUtils.parse(json, genericType, log);
    }
}
