package com.iflytek.astron.link.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.astron.link.cache.RedisService;
import com.iflytek.astron.link.constant.LinkConstants;
import com.iflytek.astron.link.constant.LinkErrorCode;
import com.iflytek.astron.link.controller.vo.req.HttpToolRunRequest;
import com.iflytek.astron.link.controller.vo.req.ToolDebugRequest;
import com.iflytek.astron.link.controller.vo.res.HttpToolRunResponse;
import com.iflytek.astron.link.controller.vo.res.ToolDebugResponse;
import com.iflytek.astron.link.tools.entity.ToolEntity;
import com.iflytek.astron.link.tools.service.ToolCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ToolExecutionService {

    @Autowired
    private HttpExecutor httpExecutor;

    @Autowired
    private ToolCrudService toolCrudService;

    @Autowired
    private RedisService redisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpToolRunResponse httpRun(HttpToolRunRequest runParams) {
        // 实现 HTTP 工具执行逻辑
        // 这里迁移了 Python 版本中的 http_run 函数逻辑
        HttpToolRunResponse response = new HttpToolRunResponse();
        HttpToolRunResponse.HttpRunResponseHeader header = new HttpToolRunResponse.HttpRunResponseHeader();

        // 提取请求参数
        String appId = extractAppId(runParams);
        String uid = extractUid(runParams);
        String sid = extractSid(runParams);

        // 设置响应头
        header.setCode(LinkErrorCode.SUCCESSES.getCode());
        header.setMessage(LinkErrorCode.SUCCESSES.getMessage());
        header.setSid(sid != null ? sid : "sid-" + System.currentTimeMillis());
        response.setHeader(header);

        try {
            // 验证请求参数
            String validateErr = validateRequest(runParams);
            if (validateErr != null) {
                return handleValidationError(validateErr, sid);
            }

            // 提取工具参数
            String toolId = runParams.getParameter().toolId();
            String operationId = runParams.getParameter().operationId();
            String version = runParams.getParameter().version();
            if (version == null || version.isEmpty()) {
                version = LinkConstants.DEF_VER;
            }

            // 获取工具schema
            ToolSchemaResult schemaResult = getToolSchema(appId, toolId, operationId, version);
            if (schemaResult.errorResponse != null) {
                return schemaResult.errorResponse;
            }

            if (schemaResult.operationIdSchema == null) {
                String message = schemaResult.operationIdSchema == null ?
                        toolId + " does not exist" :
                        "operation_id: " + operationId + " does not exist";
                LinkErrorCode errorCode = schemaResult.operationIdSchema == null ?
                        LinkErrorCode.TOOL_NOT_EXIST_ERR :
                        LinkErrorCode.OPERATION_ID_NOT_EXIST_ERR;
                return handleCustomError(errorCode, message, sid, toolId, schemaResult.toolType);
            }

            // 处理请求执行
            return handleRequestExecution(
                    schemaResult.operationIdSchema,
                    schemaResult.toolType,
                    schemaResult.openApiSchema,
                    runParams,
                    toolId,
                    operationId,
                    version,
                    sid
            );
        } catch (Exception e) {
            return handleGeneralException(e, sid);
        }
    }


    public ToolDebugResponse toolDebug(ToolDebugRequest toolDebugParams) {
        // 实现工具调试逻辑
        // 这里迁移了 Python 版本中的 tool_debug 函数逻辑
        ToolDebugResponse response = new ToolDebugResponse();
        ToolDebugResponse.ToolDebugResponseHeader header = new ToolDebugResponse.ToolDebugResponseHeader();
        String sid = "sid-" + System.currentTimeMillis();
        header.setCode(LinkErrorCode.SUCCESSES.getCode());
        header.setMessage(LinkErrorCode.SUCCESSES.getMessage());
        header.setSid(sid);
        response.setHeader(header);

        try {
            // 执行调试请求
            String result = httpExecutor.doCall(
                    toolDebugParams.getServer(),
                    toolDebugParams.getMethod(),
                    toolDebugParams.getPath(),
                    toolDebugParams.getQuery(),
                    toolDebugParams.getHeader(),
                    toolDebugParams.getBody()
            );

            // 设置响应数据
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> text = new HashMap<>();
            text.put("text", result);
            payload.put("text", text);
            response.setPayload(payload);
        } catch (Exception e) {
            header.setCode(LinkErrorCode.COMMON_ERR.getCode());
            header.setMessage("Error: " + e.getMessage());
            response.setHeader(header);
            response.setPayload(new HashMap<>());
        }

        return response;
    }

    // 辅助方法

    private String extractAppId(HttpToolRunRequest runParams) {
        if (runParams.getHeader() != null && runParams.getHeader().appId() != null) {
            return runParams.getHeader().appId();
        }
        return System.getenv(LinkConstants.DEFAULT_APPID_KEY);
    }

    private String extractUid(HttpToolRunRequest runParams) {
        if (runParams.getHeader() != null && runParams.getHeader().uid() != null) {
            return runParams.getHeader().uid();
        }
        return UUID.randomUUID().toString();
    }

    private String extractSid(HttpToolRunRequest runParams) {
        if (runParams.getHeader() != null) {
            return runParams.getHeader().uid();
        }
        return null;
    }

    private String validateRequest(HttpToolRunRequest runParams) {
        // 简化的验证逻辑，实际应该根据JSON schema进行验证
        if (runParams.getParameter() == null) {
            return "Parameter is required";
        }
        if (runParams.getParameter().toolId() == null) {
            return "Tool ID is required";
        }
        if (runParams.getParameter().operationId() == null) {
            return "Operation ID is required";
        }
        return null;
    }

    private HttpToolRunResponse handleValidationError(String validateErr, String sid) {
        HttpToolRunResponse response = new HttpToolRunResponse();
        HttpToolRunResponse.HttpRunResponseHeader header = new HttpToolRunResponse.HttpRunResponseHeader();
        header.setCode(LinkErrorCode.JSON_PROTOCOL_PARSER_ERR.getCode());
        header.setMessage(validateErr);
        header.setSid(sid != null ? sid : "sid-" + System.currentTimeMillis());
        response.setHeader(header);
        response.setPayload(new HashMap<>());
        return response;
    }

    private HttpToolRunResponse handleCustomError(LinkErrorCode errorCode, String message, String sid, String toolId, String toolType) {
        HttpToolRunResponse response = new HttpToolRunResponse();
        HttpToolRunResponse.HttpRunResponseHeader header = new HttpToolRunResponse.HttpRunResponseHeader();
        header.setCode(errorCode.getCode());
        header.setMessage(message);
        header.setSid(sid != null ? sid : "sid-" + System.currentTimeMillis());
        response.setHeader(header);
        response.setPayload(new HashMap<>());
        return response;
    }

    private HttpToolRunResponse handleGeneralException(Exception e, String sid) {
        HttpToolRunResponse response = new HttpToolRunResponse();
        HttpToolRunResponse.HttpRunResponseHeader header = new HttpToolRunResponse.HttpRunResponseHeader();
        header.setCode(LinkErrorCode.COMMON_ERR.getCode());
        header.setMessage(LinkErrorCode.COMMON_ERR.getMessage() + ": " + e.getMessage());
        header.setSid(sid != null ? sid : "sid-" + System.currentTimeMillis());
        response.setHeader(header);
        response.setPayload(new HashMap<>());
        return response;
    }

    private ToolSchemaResult getToolSchema(String appId, String toolId, String operationId, String version) {
        try {
            // 构造查询条件
            List<ToolEntity> toolList = new ArrayList<>();
            ToolEntity queryTool = new ToolEntity();
            queryTool.setAppId(appId);
            queryTool.setToolId(toolId);
            queryTool.setVersion(version);
            queryTool.setIsDeleted(LinkConstants.DEF_DEL);
            toolList.add(queryTool);

            // 查询工具
            List<ToolEntity> tools = toolCrudService.getTools(toolList);

            if (tools.isEmpty()) {
                return new ToolSchemaResult(null, null, null, null);
            }

            ToolEntity tool = tools.get(0);
            Map<String, Object> openApiSchema = objectMapper.readValue(tool.getOpenApiSchema(), Map.class);

            // 根据OpenAPI schema中的信息确定toolType
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) openApiSchema.get("info");
            boolean isOfficial = info != null && "true".equals(String.valueOf(info.get("x-is-official")));
            String toolType = isOfficial ? System.getenv(LinkConstants.OFFICIAL_TOOL_KEY) : System.getenv(LinkConstants.THIRD_TOOL_KEY);
            if (toolType == null) {
                toolType = isOfficial ? "official" : "third"; // 默认值
            }

            // 解析OpenAPI schema以获取operation schema
            Map<String, Object> operationIdSchema = parseOpenApiSchema(openApiSchema, operationId);

            return new ToolSchemaResult(operationIdSchema, toolType, openApiSchema, null);
        } catch (Exception e) {
            HttpToolRunResponse errorResponse = handleGeneralException(e, null);
            return new ToolSchemaResult(null, null, null, errorResponse);
        }
    }

    /**
     * 处理请求认证
     *
     * @param operationIdSchema 操作ID模式
     * @param messageHeader     消息头部
     * @param messageQuery      消息查询参数
     * @param toolId            工具ID
     */
    private void processAuthentication(
            Map<String, Object> openApiSchema,
            Map<String, Object> operationIdSchema,
            Map<String, Object> messageHeader,
            Map<String, Object> messageQuery,
            String toolId,
            String version) throws Exception {
        // 检查是否有安全配置
        Object securityObj = operationIdSchema.get("security");
        if (securityObj == null) {
            return;
        }

        // 获取安全类型
        String securityType = (String) operationIdSchema.get("security_type");
        if (securityType == null || securityType.isEmpty()) {
            return;
        }

        // 从Redis获取工具配置
        Map<String, Object> redisCache = redisService.getToolConfig(toolId, version);
        if (redisCache == null) {
            throw new Exception("security: get redis_cache is none!");
        }

        // 获取认证信息
        @SuppressWarnings("unchecked")
        Map<String, Object> authInfo = (Map<String, Object>) redisCache.get("authentication");
        if (authInfo == null) {
            throw new Exception("security: redis_cache get authentication is none!");
        }

        // 根据安全类型处理认证
        @SuppressWarnings("unchecked")
        Map<String, Object> securityScheme = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) openApiSchema.get("components"))
                .get("securitySchemes")).get(securityType);

        if (securityScheme != null) {
            String type = (String) securityScheme.get("type");

            // API Key认证
            if ("apiKey".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> apiKeyDict = (Map<String, Object>) authInfo.get("apiKey");
                if (apiKeyDict != null) {
                    String inLocation = (String) securityScheme.get("in");
                    if ("header".equals(inLocation)) {
                        messageHeader.putAll(apiKeyDict);
                    } else if ("query".equals(inLocation)) {
                        messageQuery.putAll(apiKeyDict);
                    }
                }
            }
            // Bearer Token认证
            else if ("http".equals(type)) {
                String scheme = (String) securityScheme.get("scheme");
                if ("bearer".equals(scheme)) {
                    // 获取Bearer Token
                    String bearerToken = (String) authInfo.get("bearerToken");
                    if (bearerToken != null && !bearerToken.isEmpty()) {
                        messageHeader.put("Authorization", "Bearer " + bearerToken);
                    }
                }
            }
        }
    }

    private HttpToolRunResponse handleRequestExecution(
            Map<String, Object> operationIdSchema,
            String toolType,
            Map<String, Object> openApiSchema,
            HttpToolRunRequest runParams,
            String toolId,
            String operationId,
            String version,
            String sid) {
        try {
            // 获取消息参数
            Map<String, Object> message = runParams.getPayload().message();

            // 解码消息参数
            Map<String, Object> messageHeader = decodeBase64Json((String) message.get("header"));
            Map<String, Object> messageQuery = decodeBase64Json((String) message.get("query"));
            Map<String, Object> path = decodeBase64Json((String) message.get("path"));
            Map<String, Object> body = decodeBase64Json((String) message.get("body"));

            // 处理认证
            try {
                processAuthentication(openApiSchema, operationIdSchema, messageHeader, messageQuery, toolId, version);
            } catch (Exception authErr) {
                String errMsg = authErr.getMessage();
                if (errMsg != null && errMsg.contains("Security type")) {
                    return handleCustomError(LinkErrorCode.OPENAPI_AUTH_TYPE_ERR, LinkErrorCode.OPENAPI_AUTH_TYPE_ERR.getMessage(), sid, toolId, toolType);
                }
                throw authErr;
            }

            // 执行HTTP请求
            String serverUrl = (String) operationIdSchema.get("server_url");
            String method = (String) operationIdSchema.get("method");
            String queryPath = (String) operationIdSchema.get("path");

            String result = httpExecutor.doCall(serverUrl + queryPath, method, path, messageQuery, messageHeader, body);

            // 构造成功响应
            HttpToolRunResponse response = new HttpToolRunResponse();
            HttpToolRunResponse.HttpRunResponseHeader header = new HttpToolRunResponse.HttpRunResponseHeader();
            header.setCode(LinkErrorCode.SUCCESSES.getCode());
            header.setMessage(LinkErrorCode.SUCCESSES.getMessage());
            header.setSid(sid != null ? sid : "sid-" + System.currentTimeMillis());
            response.setHeader(header);

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> text = new HashMap<>();
            text.put("text", result);
            payload.put("text", text);
            response.setPayload(payload);

            return response;
        } catch (Exception e) {
            return handleGeneralException(e, sid);
        }
    }

    private Map<String, Object> decodeBase64Json(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            return new HashMap<>();
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String decodedString = new String(decodedBytes);
            // 使用Jackson解析JSON
            return objectMapper.readValue(decodedString, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 解析OpenAPI schema以获取特定操作的schema
     *
     * @param openApiSchema OpenAPI schema
     * @param operationId   操作ID
     * @return operation schema
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenApiSchema(Map<String, Object> openApiSchema, String operationId) {
        Map<String, Object> operationSchema = new HashMap<>();

        try {
            // 获取servers信息
            List<Map<String, Object>> servers = (List<Map<String, Object>>) openApiSchema.get("servers");
            if (servers != null && !servers.isEmpty()) {
                Map<String, Object> server = servers.get(0);
                operationSchema.put("server_url", server.get("url"));
            } else {
                operationSchema.put("server_url", "");
            }

            // 获取paths信息
            Map<String, Object> paths = (Map<String, Object>) openApiSchema.get("paths");
            if (paths != null) {
                // 遍历所有路径找到匹配的操作
                for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                    Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

                    // 遍历所有HTTP方法
                    for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                        String method = methodEntry.getKey().toLowerCase();
                        Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();

                        // 检查operationId是否匹配
                        if (operationId.equals(operation.get("operationId"))) {
                            operationSchema.put("method", method);
                            operationSchema.put("path", pathEntry.getKey());

                            // 获取安全信息
                            Object security = operation.get("security");
                            if (security != null) {
                                operationSchema.put("security", security);

                                // 获取安全类型
                                if (security instanceof List) {
                                    List<?> securityList = (List<?>) security;
                                    if (!securityList.isEmpty() && securityList.get(0) instanceof Map) {
                                        Map<?, ?> securityMap = (Map<?, ?>) securityList.get(0);
                                        if (!securityMap.isEmpty()) {
                                            // 获取第一个键作为安全类型
                                            String securityType = securityMap.keySet().iterator().next().toString();
                                            operationSchema.put("security_type", securityType);
                                        }
                                    }
                                }
                            }

                            // 获取其他相关信息
                            Object requestBody = operation.get("requestBody");
                            if (requestBody != null) {
                                operationSchema.put("requestBody", requestBody);
                            }

                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果解析失败，使用默认值
            operationSchema.put("server_url", "");
            operationSchema.put("method", "GET");
        }

        return operationSchema;
    }

    // 内部类用于封装工具schema查询结果
    private static class ToolSchemaResult {
        Map<String, Object> operationIdSchema;
        String toolType;
        Map<String, Object> openApiSchema;
        HttpToolRunResponse errorResponse;

        ToolSchemaResult(Map<String, Object> operationIdSchema, String toolType,
                         Map<String, Object> openApiSchema, HttpToolRunResponse errorResponse) {
            this.operationIdSchema = operationIdSchema;
            this.toolType = toolType;
            this.openApiSchema = openApiSchema;
            this.errorResponse = errorResponse;
        }
    }
}