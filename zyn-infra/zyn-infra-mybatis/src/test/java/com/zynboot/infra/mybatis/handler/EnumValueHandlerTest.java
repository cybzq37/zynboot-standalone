package com.zynboot.infra.mybatis.handler;

import com.zynboot.kit.enums.IEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EnumValueHandlerTest {

    enum Gender implements IEnum<Integer> {
        MALE(1, "男"), FEMALE(0, "女");

        private final Integer code;
        private final String desc;

        Gender(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() { return code; }
        public String getDesc() { return desc; }
    }

    enum Status implements IEnum<String> {
        ACTIVE("A", "启用"), DISABLED("D", "禁用");

        private final String code;
        private final String desc;

        Status(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() { return code; }
        public String getDesc() { return desc; }
    }

    @Test
    void shouldWriteCodeToDatabase() throws Exception {
        EnumValueHandler handler = new EnumValueHandler(Gender.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setNonNullParameter(ps, 1, Gender.MALE, null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), captor.capture());
        assertThat(captor.getValue()).isEqualTo(1);
    }

    @Test
    void shouldReadEnumByColumnName() throws Exception {
        EnumValueHandler handler = new EnumValueHandler(Gender.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("gender")).thenReturn(1);

        Gender result = (Gender) handler.getNullableResult(rs, "gender");

        assertThat(result).isEqualTo(Gender.MALE);
    }

    @Test
    void shouldReadEnumByColumnIndex() throws Exception {
        EnumValueHandler handler = new EnumValueHandler(Status.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("A");

        Status result = (Status) handler.getNullableResult(rs, 1);

        assertThat(result).isEqualTo(Status.ACTIVE);
    }

    @Test
    void shouldReadEnumFromCallableStatement() throws Exception {
        EnumValueHandler handler = new EnumValueHandler(Gender.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(cs.getObject(1)).thenReturn(0);

        Gender result = (Gender) handler.getNullableResult(cs, 1);

        assertThat(result).isEqualTo(Gender.FEMALE);
    }

    @Test
    void shouldReturnNullWhenDbValueIsNull() throws Exception {
        EnumValueHandler handler = new EnumValueHandler(Gender.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("gender")).thenReturn(null);

        Object result = handler.getNullableResult(rs, "gender");

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenNoMatch() throws Exception {
        EnumValueHandler handler = new EnumValueHandler(Gender.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("gender")).thenReturn(99);

        Object result = handler.getNullableResult(rs, "gender");

        assertThat(result).isNull();
    }

    @Test
    void shouldResolveDifferentEnumTypesIndependently() throws Exception {
        EnumValueHandler genderHandler = new EnumValueHandler(Gender.class);
        EnumValueHandler statusHandler = new EnumValueHandler(Status.class);

        ResultSet rs1 = mock(ResultSet.class);
        when(rs1.getObject(1)).thenReturn(1);
        ResultSet rs2 = mock(ResultSet.class);
        when(rs2.getObject(1)).thenReturn("D");

        Gender gender = (Gender) genderHandler.getNullableResult(rs1, 1);
        Status status = (Status) statusHandler.getNullableResult(rs2, 1);

        assertThat(gender).isEqualTo(Gender.MALE);
        assertThat(status).isEqualTo(Status.DISABLED);
    }

    @Test
    void shouldMatchByStringRepresentation() throws Exception {
        EnumValueHandler handler = new EnumValueHandler(Gender.class);
        ResultSet rs = mock(ResultSet.class);
        // 数据库返回字符串 "1"，枚举 code 是 Integer 1
        when(rs.getObject("gender")).thenReturn("1");

        Gender result = (Gender) handler.getNullableResult(rs, "gender");

        assertThat(result).isEqualTo(Gender.MALE);
    }

    @Test
    void shouldWorkWithAnnotationStyleInstantiation() {
        // 模拟 MyBatis-Plus 的实例化方式：new EnumValueHandler(FieldType.class)
        EnumValueHandler handler = new EnumValueHandler(Gender.class);
        assertThat(handler).isInstanceOf(BaseTypeHandler.class);
    }
}
