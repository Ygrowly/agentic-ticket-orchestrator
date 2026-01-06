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
        HttpToolRunResponse response = new HttpToolRunResponse();
        HttpToolRunResponse.HttpRunResponseHeader header = new HttpToolRunResponse.HttpRunResponseHeader();

        String appId = extractAppId(runParams);
        String uid = extractUid(runParams);
        String sid = extractSid(runParams);
        
        log.info("httpRun started - appId: {}, uid: {}, sid: {}", appId, uid, sid);

        header.setCode(LinkErrorCode.SUCCESSES.getCode());
        header.setMessage(LinkErrorCode.SUCCESSES.getMessage());
        header.setSid(sid != null ? sid : "sid-" + System.currentTimeMillis());
        response.setHeader(header);

        try {
            String validateErr = validateRequest(runParams);
            if (validateErr != null) {
                log.warn("Validation failed: {}", validateErr);
                return handleValidationError(validateErr, sid);
            }

            String toolId = runParams.getParameter().toolId();
            String operationId = runParams.getParameter().operationId();
            String version = runParams.getParameter().version();
            if (version == null || version.isEmpty()) {
                version = LinkConstants.DEF_VER;
            }
            
            log.info("Executing tool - toolId: {}, operationId: {}, version: {}", toolId, operationId, version);

            ToolSchemaResult schemaResult = getToolSchema(appId, toolId, operationId, version);
            if (schemaResult.errorResponse != null) {
                log.error("Failed to get tool schema - toolId: {}", toolId);
                return schemaResult.errorResponse;
            }

            if (schemaResult.operationIdSchema == null) {
                String message = schemaResult.operationIdSchema == null ?
                        toolId + " does not exist" :
                        "operation_id: " + operationId + " does not exist";
                LinkErrorCode errorCode = schemaResult.operationIdSchema == null ?
                        LinkErrorCode.TOOL_NOT_EXIST_ERR :
                        LinkErrorCode.OPERATION_ID_NOT_EXIST_ERR;
                log.error("Tool or operation not found: {}", message);
                return handleCustomError(errorCode, message, sid, toolId, schemaResult.toolType);
            }

            HttpToolRunResponse result = handleRequestExecution(
                    schemaResult.operationIdSchema,
                    schemaResult.toolType,
                    schemaResult.openApiSchema,
                    runParams,
                    toolId,
                    operationId,
                    version,
                    sid
            );
            log.info("httpRun completed successfully - toolId: {}, operationId: {}", toolId, operationId);
            return result;
        } catch (Exception e) {
            log.error("httpRun failed - appId: {}, error: {}", appId, e.getMessage(), e);
            return handleGeneralException(e, sid);
        }
    }


    public ToolDebugResponse toolDebug(ToolDebugRequest toolDebugParams) {
        ToolDebugResponse response = new ToolDebugResponse();
        ToolDebugResponse.ToolDebugResponseHeader header = new ToolDebugResponse.ToolDebugResponseHeader();
        String sid = "sid-" + System.currentTimeMillis();
        
        log.info("toolDebug started - server: {}, method: {}, sid: {}", 
                toolDebugParams.getServer(), toolDebugParams.getMethod(), sid);
        
        header.setCode(LinkErrorCode.SUCCESSES.getCode());
        header.setMessage(LinkErrorCode.SUCCESSES.getMessage());
        header.setSid(sid);
        response.setHeader(header);

        try {
            String serverUrl = normalizeServerUrl(toolDebugParams.getServer());
            log.info("Normalized server URL: {} -> {}", toolDebugParams.getServer(), serverUrl);
            
            String result = httpExecutor.doCall(
                    serverUrl,
                    toolDebugParams.getMethod(),
                    toolDebugParams.getPath(),
                    toolDebugParams.getQuery(),
                    toolDebugParams.getHeader(),
                    toolDebugParams.getBody()
            );

            log.info("toolDebug call successful, response length: {}", result.length());
            
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> text = new HashMap<>();
            text.put("text", result);
            payload.put("text", text);
            response.setPayload(payload);
        } catch (Exception e) {
            log.error("toolDebug failed - server: {}, error: {}", toolDebugParams.getServer(), e.getMessage(), e);
            header.setCode(LinkErrorCode.COMMON_ERR.getCode());
            header.setMessage("Error: " + e.getMessage());
            response.setHeader(header);
            response.setPayload(new HashMap<>());
        }

        return response;
    }

    private String normalizeServerUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            return serverUrl;
        }
        
        // 将Python aitools服务地址(core-aitools:18668)转换为当前Java服务地址(localhost:7880)
        if (serverUrl.contains("core-aitools:18668")) {
            serverUrl = serverUrl.replace("core-aitools:18668", "localhost:7880");
            log.info("Normalized server URL from core-aitools:18668 to localhost:7880");
        }
        
        return serverUrl;
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
        log.info("Getting tool schema - appId: {}, toolId: {}, operationId: {}, version: {}", 
                appId, toolId, operationId, version);
        try {
            List<ToolEntity> toolList = new ArrayList<>();
            ToolEntity queryTool = new ToolEntity();
            queryTool.setAppId(appId);
            queryTool.setToolId(toolId);
            queryTool.setVersion(version);
            queryTool.setIsDeleted(LinkConstants.DEF_DEL);
            toolList.add(queryTool);

            List<ToolEntity> tools = toolCrudService.getTools(toolList);

            if (tools.isEmpty()) {
                log.warn("Tool not found - toolId: {}, version: {}", toolId, version);
                return new ToolSchemaResult(null, null, null, null);
            }

            ToolEntity tool = tools.get(0);
            Map<String, Object> openApiSchema = objectMapper.readValue(tool.getOpenApiSchema(), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) openApiSchema.get("info");
            boolean isOfficial = info != null && "true".equals(String.valueOf(info.get("x-is-official")));
            String toolType = isOfficial ? System.getenv(LinkConstants.OFFICIAL_TOOL_KEY) : System.getenv(LinkConstants.THIRD_TOOL_KEY);
            if (toolType == null) {
                toolType = isOfficial ? "official" : "third";
            }

            Map<String, Object> operationIdSchema = parseOpenApiSchema(openApiSchema, operationId);
            
            log.info("Tool schema retrieved - toolId: {}, toolType: {}, hasOperation: {}", 
                    toolId, toolType, operationIdSchema != null);

            return new ToolSchemaResult(operationIdSchema, toolType, openApiSchema, null);
        } catch (Exception e) {
            log.error("Failed to get tool schema - toolId: {}, error: {}", toolId, e.getMessage(), e);
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
            String version,
            String appId) throws Exception {
        Object securityObj = operationIdSchema.get("security");
        if (securityObj == null) {
            log.debug("No security configuration for toolId: {}", toolId);
            return;
        }

        String securityType = (String) operationIdSchema.get("security_type");
        if (securityType == null || securityType.isEmpty()) {
            log.debug("No security type specified for toolId: {}", toolId);
            return;
        }
        
        log.info("Processing authentication - toolId: {}, securityType: {}", toolId, securityType);

        Map<String, Object> redisCache = redisService.getToolConfig(toolId, version, appId);
        if (redisCache == null) {
            log.error("Config not found for toolId: {}, version: {}", toolId, version);
            throw new Exception("security: get tool config is none!");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> authInfo = (Map<String, Object>) redisCache.get("authentication");
        if (authInfo == null) {
            log.error("Authentication info not found in redis for toolId: {}", toolId);
            throw new Exception("security: redis_cache get authentication is none!");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> securityScheme = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) openApiSchema.get("components"))
                .get("securitySchemes")).get(securityType);

        if (securityScheme != null) {
            String type = (String) securityScheme.get("type");

            if ("apiKey".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> apiKeyDict = (Map<String, Object>) authInfo.get("apiKey");
                if (apiKeyDict != null) {
                    String inLocation = (String) securityScheme.get("in");
                    if ("header".equals(inLocation)) {
                        messageHeader.putAll(apiKeyDict);
                        log.info("Added API key to header for toolId: {}", toolId);
                    } else if ("query".equals(inLocation)) {
                        messageQuery.putAll(apiKeyDict);
                        log.info("Added API key to query for toolId: {}", toolId);
                    }
                }
            }
            else if ("http".equals(type)) {
                String scheme = (String) securityScheme.get("scheme");
                if ("bearer".equals(scheme)) {
                    String bearerToken = (String) authInfo.get("bearerToken");
                    if (bearerToken != null && !bearerToken.isEmpty()) {
                        messageHeader.put("Authorization", "Bearer " + bearerToken);
                        log.info("Added Bearer token for toolId: {}", toolId);
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
            Map<String, Object> message = runParams.getPayload().message();

            Map<String, Object> messageHeader = decodeBase64Json((String) message.get("header"));
            Map<String, Object> messageQuery = decodeBase64Json((String) message.get("query"));
            Map<String, Object> path = decodeBase64Json((String) message.get("path"));
            Map<String, Object> body = decodeBase64Json((String) message.get("body"));
            
            log.info("Decoded request params - header size: {}, query size: {}, body size: {}", 
                    messageHeader.size(), messageQuery.size(), body.size());

            try {
                String appId = extractAppId(runParams);
                processAuthentication(openApiSchema, operationIdSchema, messageHeader, messageQuery, toolId, version, appId);
                log.info("Authentication processed for toolId: {}", toolId);
            } catch (Exception authErr) {
                log.error("Authentication failed for toolId: {}, error: {}", toolId, authErr.getMessage());
                String errMsg = authErr.getMessage();
                if (errMsg != null && errMsg.contains("Security type")) {
                    return handleCustomError(LinkErrorCode.OPENAPI_AUTH_TYPE_ERR, LinkErrorCode.OPENAPI_AUTH_TYPE_ERR.getMessage(), sid, toolId, toolType);
                }
                throw authErr;
            }

            String serverUrl = (String) operationIdSchema.get("server_url");
            String method = (String) operationIdSchema.get("method");
            String queryPath = (String) operationIdSchema.get("path");
            
            log.info("Executing HTTP call - url: {}{}, method: {}", serverUrl, queryPath, method);

            String result = httpExecutor.doCall(serverUrl + queryPath, method, path, messageQuery, messageHeader, body);
            
            log.info("HTTP call successful - toolId: {}, operationId: {}, response length: {}", 
                    toolId, operationId, result.length());

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
            log.error("Request execution failed - toolId: {}, operationId: {}, error: {}", 
                    toolId, operationId, e.getMessage(), e);
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
        log.info("Parsing OpenAPI schema for operationId: {}", operationId);
        Map<String, Object> operationSchema = new HashMap<>();

        try {
            List<Map<String, Object>> servers = (List<Map<String, Object>>) openApiSchema.get("servers");
            if (servers != null && !servers.isEmpty()) {
                Map<String, Object> server = servers.get(0);
                String serverUrl = (String) server.get("url");
                operationSchema.put("server_url", serverUrl);
                log.debug("Server URL found: {}", serverUrl);
            } else {
                operationSchema.put("server_url", "");
                log.warn("No server URL found in OpenAPI schema");
            }

            Map<String, Object> paths = (Map<String, Object>) openApiSchema.get("paths");
            if (paths != null) {
                log.debug("Parsing {} paths to find operationId: {}", paths.size(), operationId);
                boolean found = false;
                
                for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                    Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

                    for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                        String method = methodEntry.getKey().toLowerCase();
                        Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();

                        if (operationId.equals(operation.get("operationId"))) {
                            operationSchema.put("method", method);
                            operationSchema.put("path", pathEntry.getKey());
                            found = true;
                            
                            log.info("Operation found - operationId: {}, method: {}, path: {}", 
                                    operationId, method, pathEntry.getKey());

                            Object security = operation.get("security");
                            if (security != null) {
                                operationSchema.put("security", security);

                                if (security instanceof List) {
                                    List<?> securityList = (List<?>) security;
                                    if (!securityList.isEmpty() && securityList.get(0) instanceof Map) {
                                        Map<?, ?> securityMap = (Map<?, ?>) securityList.get(0);
                                        if (!securityMap.isEmpty()) {
                                            String securityType = securityMap.keySet().iterator().next().toString();
                                            operationSchema.put("security_type", securityType);
                                            log.info("Security type found: {}", securityType);
                                        }
                                    }
                                }
                            }

                            Object requestBody = operation.get("requestBody");
                            if (requestBody != null) {
                                operationSchema.put("requestBody", requestBody);
                            }

                            break;
                        }
                    }
                    if (found) break;
                }
                
                if (!found) {
                    log.error("Operation not found in paths - operationId: {}", operationId);
                }
            } else {
                log.error("No paths found in OpenAPI schema");
            }
        } catch (Exception e) {
            log.error("Error parsing OpenAPI schema for operationId: {}, error: {}", 
                    operationId, e.getMessage(), e);
            operationSchema.put("server_url", "");
            operationSchema.put("method", "GET");
        }
        
        log.debug("Parsed operation schema: {}", operationSchema.keySet());
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