package com.iflytek.astron.link.cache;

import com.iflytek.astron.link.tools.entity.ToolEntity;
import com.iflytek.astron.link.tools.service.ToolCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {
    private static final String CONFIG_KEY_PREFIX = "spark_bot:tool_config:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ToolCrudService toolCrudService;

    public Map<String, Object> getToolConfig(String toolId, String version, String appId) {
        String key = CONFIG_KEY_PREFIX + toolId + ":" + version;
        log.info("Getting tool config from Redis - key: {}", key);
        
        try {
            Map<String, Object> config = (Map<String, Object>) redisTemplate.opsForValue().get(key);
            if (config == null) {
                log.warn("Tool config not found in Redis - toolId: {}, version: {}, fallback to database", toolId, version);
                config = getToolConfigFromDatabase(toolId, version, appId);
                if (config != null) {
                    log.info("Tool config retrieved from database, caching to Redis - toolId: {}, version: {}", toolId, version);
                    setToolConfig(toolId, version, config, 3600);
                }
            } else {
                log.info("Tool config retrieved from Redis - toolId: {}, version: {}, keys: {}", 
                        toolId, version, config.keySet());
            }
            return config;
        } catch (Exception e) {
            log.error("Failed to get tool config from Redis - toolId: {}, version: {}, error: {}", 
                    toolId, version, e.getMessage(), e);
            throw new RuntimeException("Redis get tool config failed", e);
        }
    }
    
    private Map<String, Object> getToolConfigFromDatabase(String toolId, String version, String appId) {
        try {
            List<ToolEntity> tools = toolCrudService.getToolsByToolId(appId, toolId);
            if (tools == null || tools.isEmpty()) {
                log.warn("Tool not found in database - toolId: {}, appId: {}", toolId, appId);
                return null;
            }
            
            ToolEntity tool = tools.stream()
                    .filter(t -> version.equals(t.getVersion()))
                    .findFirst()
                    .orElse(null);
                    
            if (tool == null) {
                log.warn("Tool version not found in database - toolId: {}, version: {}", toolId, version);
                return null;
            }
            
            Map<String, Object> config = new HashMap<>();
            // TODO: Parse authentication info from tool.getAuthInfo() if exists
            log.info("Tool config retrieved from database - toolId: {}, version: {}", toolId, version);
            return config;
        } catch (Exception e) {
            log.error("Failed to get tool config from database - toolId: {}, version: {}, error: {}", 
                    toolId, version, e.getMessage(), e);
            return null;
        }
    }

    public void setToolConfig(String toolId, String version, Map<String, Object> config, long expireTime) {
        String key = CONFIG_KEY_PREFIX + toolId + ":" + version;
        log.info("Setting tool config to Redis - key: {}, expireTime: {}s", key, expireTime);
        
        try {
            redisTemplate.opsForValue().set(key, config, expireTime, TimeUnit.SECONDS);
            log.info("Tool config set successfully - toolId: {}, version: {}", toolId, version);
        } catch (Exception e) {
            log.error("Failed to set tool config to Redis - toolId: {}, version: {}, error: {}", 
                    toolId, version, e.getMessage(), e);
            throw new RuntimeException("Redis set tool config failed", e);
        }
    }

    public boolean hasToolConfig(String toolId, String version) {
        String key = CONFIG_KEY_PREFIX + toolId + ":" + version;
        log.debug("Checking tool config existence in Redis - key: {}", key);
        
        try {
            Boolean exists = redisTemplate.hasKey(key);
            boolean result = Boolean.TRUE.equals(exists);
            log.debug("Tool config exists: {} - toolId: {}, version: {}", result, toolId, version);
            return result;
        } catch (Exception e) {
            log.error("Failed to check tool config in Redis - toolId: {}, version: {}, error: {}", 
                    toolId, version, e.getMessage(), e);
            throw new RuntimeException("Redis check tool config failed", e);
        }
    }
}