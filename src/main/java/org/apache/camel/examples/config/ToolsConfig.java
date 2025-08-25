package org.apache.camel.examples.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具配置类，负责从Spring容器中获取所有工具类并注册为ToolCallbackProvider
 */
@Slf4j
@Component
public class ToolsConfig {
    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        List<Object> toolObjects = new ArrayList<>();
        
        // 获取所有可能包含工具方法的Bean
        collectTools(toolObjects);
        
        // 打印注册的工具信息
        log.info("已注册 {} 个工具类:", toolObjects.size());
        for (Object tool : toolObjects) {
            log.info("- {}", tool.getClass().getSimpleName());
        }
        
        return MethodToolCallbackProvider.builder()
            .toolObjects(toolObjects.toArray())
            .build();
    }
    
    private <A extends Annotation> void collectTools(List<Object> toolObjects) {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        
        for (Object bean : beans.values()) {
            if (hasToolMethod(bean) && !toolObjects.contains(bean)) {
                toolObjects.add(bean);
            }
        }
    }
    
    private boolean hasToolMethod(Object bean) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (AnnotationUtils.findAnnotation(method, Tool.class) != null) {
                return true;
            }
        }
        return false;
    }
}
