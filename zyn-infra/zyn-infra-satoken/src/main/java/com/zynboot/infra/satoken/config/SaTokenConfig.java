package com.zynboot.infra.satoken.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.zynboot.infra.redis.RedisClient;
import com.zynboot.infra.satoken.dao.RedisSaTokenDao;
import com.zynboot.infra.satoken.service.SaPermissionImpl;
import com.zynboot.infra.satoken.utils.LoginHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 自动配置
 * <p>
 * 通过 {@code zyn.infra.satoken.enabled=true} 启用（默认开启）
 *
 * @author lichunqing
 */
@AutoConfiguration
@ConditionalOnClass(StpLogic.class)
@ConditionalOnProperty(prefix = "zyn.satoken", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SaTokenProperties.class)
public class SaTokenConfig {

    @Bean
    @ConditionalOnMissingBean(StpLogic.class)
    public StpLogic stpLogic(SaTokenProperties properties) {
        return new StpLogic(properties.getLoginType());
    }

    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface stpInterface() {
        return new SaPermissionImpl();
    }

    @Bean
    @ConditionalOnBean(RedisClient.class)
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao saTokenDao(RedisClient redisTemplate) {
        return new RedisSaTokenDao(redisTemplate);
    }

    @Bean
    LoginHelper loginHelperInitializer(SaTokenProperties properties) {
        LoginHelper.setProperties(properties);
        return new LoginHelper();
    }

    /**
     * 全局鉴权拦截器：对所有请求默认 checkLogin（白名单除外），并处理 @SaCheckLogin/@SaCheckPermission/@SaCheckRole 等注解。
     * <p>
     * 通过 {@code zyn.satoken.interceptor-enabled=false} 可关闭（如纯展示型应用）。
     */
    @Configuration
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "zyn.satoken", name = "interceptor-enabled", havingValue = "true", matchIfMissing = true)
    static class SaTokenWebMvcConfigurer implements WebMvcConfigurer {

        private final SaTokenProperties properties;

        SaTokenWebMvcConfigurer(SaTokenProperties properties) {
            this.properties = properties;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                    .addPathPatterns("/**")
                    .excludePathPatterns(properties.getExcludePaths());
        }
    }
}
