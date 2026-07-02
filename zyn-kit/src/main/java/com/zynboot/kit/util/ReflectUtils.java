package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 反射工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectUtils {

    // ==================== 字段操作 ====================

    /**
     * 获取字段值（支持 private）。
     */
    public static Object getFieldValue(Object target, String fieldName) {
        try {
            Field field = getField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new IllegalStateException("getFieldValue failed: " + fieldName, e);
        }
    }

    /**
     * 设置字段值（支持 private）。
     */
    public static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = getField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("setFieldValue failed: " + fieldName, e);
        }
    }

    /**
     * 获取字段（含父类）。
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalArgumentException("Field not found: " + fieldName + " in " + clazz.getName());
    }

    /**
     * 获取所有字段（含父类）。
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    // ==================== 方法调用 ====================

    /**
     * 调用无参方法。
     */
    public static Object invokeMethod(Object target, String methodName) {
        return invokeMethod(target, methodName, new Class<?>[0], new Object[0]);
    }

    /**
     * 调用方法（含参数）。
     */
    public static Object invokeMethod(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new IllegalStateException("invokeMethod failed: " + methodName, e);
        }
    }

    /**
     * 获取方法（含父类）。
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }

    // ==================== 注解 ====================

    /**
     * 获取类上的注解（含父类）。
     */
    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationType) {
        T annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) return annotation;
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && parent != Object.class) {
            return getAnnotation(parent, annotationType);
        }
        return null;
    }

    /**
     * 获取字段上的注解（含父类）。
     */
    public static <T extends Annotation> T getFieldAnnotation(Class<?> clazz, String fieldName, Class<T> annotationType) {
        Field field = getField(clazz, fieldName);
        return field.getAnnotation(annotationType);
    }

    // ==================== 泛型 ====================

    /**
     * 获取泛型父类的实际类型参数。
     *
     * {@code getGenericSuperclass(UserService.class, 0)} → User.class
     */
    public static Class<?> getGenericSuperclass(Class<?> clazz, int index) {
        Type type = clazz.getGenericSuperclass();
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (index < args.length && args[index] instanceof Class<?> c) {
                return c;
            }
        }
        return Object.class;
    }

    /**
     * 获取接口的泛型参数。
     */
    public static Class<?> getGenericInterface(Class<?> clazz, Class<?> interfaceClass, int index) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType pt && pt.getRawType().equals(interfaceClass)) {
                Type[] args = pt.getActualTypeArguments();
                if (index < args.length && args[index] instanceof Class<?> c) {
                    return c;
                }
            }
        }
        return Object.class;
    }

    // ==================== 实例化 ====================

    /**
     * 通过无参构造器创建实例。
     */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate: " + clazz.getName(), e);
        }
    }

    // ==================== 判断 ====================

    /**
     * 判断是否为 Spring 代理类。
     */
    public static boolean isProxy(Class<?> clazz) {
        return clazz.getName().contains("$$");
    }

    /**
     * 获取真实类（处理 CGLIB 代理）。
     */
    public static Class<?> getRealClass(Class<?> clazz) {
        if (clazz.getName().contains("$$")) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

    /**
     * 对象转 Map（字段名 → 值）。
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) return new LinkedHashMap<>();
        Map<String, Object> map = new LinkedHashMap<>();
        for (Field field : getAllFields(obj.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            try {
                map.put(field.getName(), field.get(obj));
            } catch (IllegalAccessException ignored) {
            }
        }
        return map;
    }
}
