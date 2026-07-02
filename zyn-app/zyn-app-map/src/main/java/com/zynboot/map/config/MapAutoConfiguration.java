package com.zynboot.map.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.zynboot.infra.mybatis.config.MybatisAutoConfiguration;
import com.zynboot.infra.satoken.config.SaTokenConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@AutoConfiguration(after = {SaTokenConfig.class, MybatisAutoConfiguration.class})
@MapperScan("com.zynboot.map.infrastructure.mapper")
public class MapAutoConfiguration {

    @Bean
    public BeanPostProcessor optimisticLockerProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof MybatisPlusInterceptor interceptor) {
                    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
                }
                return bean;
            }
        };
    }

    @Bean("mapTileExecutor")
    public ExecutorService mapTileExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        AtomicInteger counter = new AtomicInteger(1);
        return Executors.newFixedThreadPool(Math.max(2, cores), task -> {
            Thread thread = new Thread(task);
            thread.setName("map-tile-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }
}
