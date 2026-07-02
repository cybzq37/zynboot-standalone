package com.zynboot.kit.util;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * spring工具类
 *
 * @author lichunqing
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    private static volatile ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }

    private static ListableBeanFactory getBeanFactory() {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext has not been initialized");
        }
        return applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static boolean isContextReady() {
        return applicationContext != null;
    }

    public static <T> T getBean(Class<T> clazz) {
        return getBeanFactory().getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        return getBeanFactory().getBean(name, clazz);
    }

    public static Object getBean(String name) {
        return getBeanFactory().getBean(name);
    }

    public static <T> Optional<T> getBeanIfPresent(Class<T> clazz) {
        if (!isContextReady()) {
            return Optional.empty();
        }
        try {
            return Optional.of(getBean(clazz));
        } catch (BeansException ex) {
            return Optional.empty();
        }
    }

    public static <T> Optional<T> getBeanIfPresent(String name, Class<T> clazz) {
        if (!isContextReady()) {
            return Optional.empty();
        }
        try {
            return Optional.of(getBean(name, clazz));
        } catch (BeansException ex) {
            return Optional.empty();
        }
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        if (!isContextReady()) {
            return Collections.emptyMap();
        }
        return getBeanFactory().getBeansOfType(clazz);
    }

    public static boolean containsBean(String name) {
        return getBeanFactory().containsBean(name);
    }

    public static boolean isSingleton(String name) {
        return getBeanFactory().isSingleton(name);
    }

    public static Class<?> getType(String name) {
        return getBeanFactory().getType(name);
    }

    public static String[] getAliases(String name) {
        return getBeanFactory().getAliases(name);
    }

    public static String getProperty(String key) {
        if (!isContextReady()) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    public static boolean isActiveProfile(String profile) {
        if (!isContextReady() || profile == null || profile.isBlank()) {
            return false;
        }
        for (String activeProfile : applicationContext.getEnvironment().getActiveProfiles()) {
            if (profile.equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }
        return false;
    }

    public static void publishEvent(ApplicationEvent event) {
        if (event == null) {
            return;
        }
        getApplicationContext().publishEvent(event);
    }

    public static void publishEvent(Object event) {
        if (event == null) {
            return;
        }
        getApplicationContext().publishEvent(event);
    }

    /**
     * 获取 aop 代理对象。
     * 需要 {@code @EnableAspectJAutoProxy(exposeProxy = true)} 配合使用。
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(T invoker) {
        return (T) AopContext.currentProxy();
    }

}
