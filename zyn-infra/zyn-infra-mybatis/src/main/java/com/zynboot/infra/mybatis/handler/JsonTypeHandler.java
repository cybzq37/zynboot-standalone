package com.zynboot.infra.mybatis.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JSON 字段类型处理器（VARCHAR 存储）。
 * <p>
 * 使用 Jackson 将 Java 对象与数据库 {@code varchar}/{@code text} 列中存储的 JSON 字符串相互映射。
 */
@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class JsonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    public JsonTypeHandler(Class<T> clazz) {
        super(clazz);
    }

    /** 自 3.5.6 版本开始支持泛型，需要加上此构造。 */
    public JsonTypeHandler(Class<?> type, Field field, Class<T> clazz) {
        super(type, field);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, toJson(parameter));
    }

    @Override
    public T parse(String json) {
        return JacksonHandlerUtils.parse(json, getFieldType(), log);
    }

    @Override
    public String toJson(Object obj) {
        return JacksonHandlerUtils.toJson(obj, log);
    }
}
