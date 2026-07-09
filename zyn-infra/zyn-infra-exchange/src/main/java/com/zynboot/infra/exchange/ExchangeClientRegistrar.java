package com.zynboot.infra.exchange;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 扫描所有 {@link ExchangeClient @ExchangeClient} 标记的接口，
 * 自动注册为 HttpExchange 代理 Bean。
 * <p>
 * 扫描包可通过 {@code zyn.exchange.scan-base-packages} 配置（逗号分隔），默认 {@code com.zynboot}。
 */
@Slf4j
public class ExchangeClientRegistrar implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                        return true;
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(ExchangeClient.class));

        // 从 Environment 读取扫描包配置（简单字符串，Environment 可直接读取）
        // 注意：不能调用 getBean(ExchangeProperties.class)，否则 ExchangeProperties 会在此阶段提前实例化，
        // 此时 ConfigurationPropertiesBindingPostProcessor 尚未注册，services Map 不会被绑定。
        // services 配置由 ExchangeClientFactoryBean.afterPropertiesSet 在 Bean 初始化阶段读取（此时已绑定）。
        Environment env = applicationContext.getEnvironment();
        String scanPackages = env.getProperty("zyn.exchange.scan-base-packages", "com.zynboot");
        List<String> basePackages = Arrays.stream(scanPackages.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            log.debug("Scanning @ExchangeClient in package '{}': {} found", basePackage, candidates.size());
            for (BeanDefinition bd : candidates) {
                String className = bd.getBeanClassName();
                if (className == null) continue;

                try {
                    Class<?> clientType = Class.forName(className);
                    ExchangeClient anno = clientType.getAnnotation(ExchangeClient.class);
                    if (anno == null) continue;

                    String serviceName = anno.value();
                    String beanName = className + "Proxy";

                    if (registry.containsBeanDefinition(beanName)) continue;

                    AbstractBeanDefinition proxyBd = BeanDefinitionBuilder
                            .genericBeanDefinition(ExchangeClientFactoryBean.class)
                            .addConstructorArgValue(clientType)
                            .addConstructorArgValue(serviceName)
                            .setScope(BeanDefinition.SCOPE_SINGLETON)
                            .getBeanDefinition();
                    proxyBd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

                    registry.registerBeanDefinition(beanName, proxyBd);

                    log.info("Discovered service client: {} -> {}", clientType.getSimpleName(), serviceName);
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to load service client class: {}", className, e);
                }
            }
        }
    }
}
