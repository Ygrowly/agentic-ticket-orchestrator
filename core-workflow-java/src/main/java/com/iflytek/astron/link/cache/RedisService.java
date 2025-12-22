package com.iflytek.astron.link.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private static final String CONFIG_KEY_PREFIX = "spark_bot:tool_config:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 从Redis中获取工具配置信息
     *
     * @param toolId 工具ID
     * @return 工具配置信息
     */
    public Map<String, Object> getToolConfig(String toolId, String version) {
        try {
            String key = CONFIG_KEY_PREFIX.concat(toolId).concat(":").concat(version);
            return (Map<String, Object>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            // 记录日志但不抛出异常，让调用者决定如何处理
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将工具配置信息存储到Redis中
     *
     * @param toolId 工具ID
     * @param config 工具配置信息
     * @param expireTime 过期时间（秒）
     */
    public void setToolConfig(String toolId, Map<String, Object> config, long expireTime) {
        try {
            String key = "spark_bot:tool_config:" + toolId;
            redisTemplate.opsForValue().set(key, config, expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 记录日志但不抛出异常
            e.printStackTrace();
        }
    }

    /**
     * 检查Redis中是否存在指定的工具配置
     *
     * @param toolId 工具ID
     * @return 是否存在
     */
    public boolean hasToolConfig(String toolId) {
        try {
            String key = "spark_bot:tool_config:" + toolId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            // 记录日志但不抛出异常
            e.printStackTrace();
            return false;
        }
    }
}