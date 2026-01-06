package com.iflytek.astron.workflow.engine.integration.plugins.aitools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Tool {
    private final String appId;
    private final String toolId;
    private final String operationId;
    private final Map<String, Object> methodSchema;
    private final Map<String, Object> parameters;
    private final String getUrl;
    private final String runUrl;
    private final String version;
    
    private final OkHttpClient httpClient;
    
    public Tool(String appId, String toolId, String operationId, 
                Map<String, Object> methodSchema, Map<String, Object> parameters,
                String getUrl, String runUrl, String version) {
        this.appId = appId;
        this.toolId = toolId;
        this.operationId = operationId;
        this.methodSchema = methodSchema != null ? methodSchema : new HashMap<>();
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.getUrl = getUrl;
        this.runUrl = runUrl;
        this.version = version;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();
    }
    
    public String getOperationId() {
        return operationId;
    }
    
    /**
     * Assemble HTTP parameters from action and business inputs.
     */
    public Map<String, Object>[] assembleParameters(Map<String, Object> actionInput, 
                                                   Map<String, Object> businessInput) {
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> path = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parametersSchema = (List<Map<String, Object>>) methodSchema.get("parameters");
        if (parametersSchema != null) {
            for (Map<String, Object> parameter : parametersSchema) {
                String in = (String) parameter.get("in");
                if ("header".equals(in)) {
                    updateParams(header, parameter, actionInput, businessInput);
                } else if ("query".equals(in)) {
                    updateParams(query, parameter, actionInput, businessInput);
                } else if ("path".equals(in)) {
                    updateParams(path, parameter, actionInput, businessInput);
                }
            }
        }
        
        return new Map[]{header, query, path};
    }
    
    /**
     * Update parameter dictionary with values from appropriate input source.
     */
    private void updateParams(Map<String, Object> params, Map<String, Object> headerParameter,
                             Map<String, Object> actionInput, Map<String, Object> businessInput) {
        Object xFromObj = getNestedValue(headerParameter, "schema", "x-from");
        String name = (String) headerParameter.get("name");
        Object defaultValue = getNestedValue(headerParameter, "schema", "default");
        
        Integer xFrom = xFromObj instanceof Number ? ((Number) xFromObj).intValue() : null;
        Object value;
        
        if (xFrom != null && xFrom == 0) {  // Model recognition source
            value = actionInput != null ? actionInput.getOrDefault(name, defaultValue) : defaultValue;
        } else if (xFrom != null && xFrom == 1) {  // Business passthrough source
            value = businessInput != null ? businessInput.getOrDefault(name, defaultValue) : defaultValue;
        } else {
            // Default to action input, fallback to default value
            value = actionInput != null ? actionInput.getOrDefault(name, defaultValue) : defaultValue;
        }
        if (name != null) {
            params.put(name, value);
        }
    }
    
    /**
     * Assemble request body from schema and input parameters.
     */
    public Map<String, Object> assembleBody(Map<String, Object> bodySchema, 
                                           Map<String, Object> actionInput, 
                                           Map<String, Object> businessInput) {
        Map<String, Object> properties = new HashMap<>();
        if (bodySchema == null) {
            return properties;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> bodyProperties = (Map<String, Object>) bodySchema.get("properties");
        
        if (bodyProperties != null) {
            for (Map.Entry<String, Object> entry : bodyProperties.entrySet()) {
                String parameterName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> parameterDetail = (Map<String, Object>) entry.getValue();
                
                String parameterType = (String) parameterDetail.get("type");
                
                if ("object".equals(parameterType)) {
                    // Recursively process nested objects
                    Map<String, Object> nestedActionInput = new HashMap<>();
                    if (actionInput != null) {
                        Object nestedValue = actionInput.getOrDefault(parameterName, new HashMap<>());
                        if (nestedValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nestedMap = (Map<String, Object>) nestedValue;
                            nestedActionInput = nestedMap;
                        }
                    }
                    Map<String, Object> nestedProperties = assembleBody(parameterDetail, nestedActionInput, businessInput);
                    properties.put(parameterName, nestedProperties);
                } else {
                    Object xFromObj = parameterDetail.get("x-from");
                    Object defaultValue = parameterDetail.get("default");
                    
                    Integer xFrom = xFromObj instanceof Number ? ((Number) xFromObj).intValue() : null;
                    Object value;
                    
                    if (xFrom != null && xFrom == 0) {
                        value = actionInput != null ? actionInput.get(parameterName) : null;
                    } else if (xFrom != null && xFrom == 1) {
                        value = businessInput != null ? businessInput.get(parameterName) : null;
                    } else {
                        value = actionInput != null ? actionInput.get(parameterName) : null;
                    }
                    
                    // Use default value if no value found
                    if (value == null) {
                        if (defaultValue == null) {
                            continue;
                        }
                        value = defaultValue;
                    }
                    properties.put(parameterName, value);
                }
            }
        }
        return properties;
    }
    
    /**
     * Encode payload as base64-encoded JSON string.
     */
    private String dumps(Map<String, Object> payload) {
        if (payload != null && !payload.isEmpty()) {
            return Base64.getEncoder().encodeToString(JSON.toJSONString(payload).getBytes());
        }
        return null;
    }
    
    /**
     * Execute the tool operation through the Link system.
     */
    public Map<String, Object> run(Map<String, Object> actionInput, 
                                  Map<String, Object> businessInput) throws IOException {
        // Extract request body schema from method schema
        Map<String, Object> requestBody = getNestedMap(methodSchema, "requestBody");
        Map<String, Object> content = getNestedMap(requestBody, "content");
        Map<String, Object> applicationJson = getNestedMap(content, "application/json");
        Map<String, Object> bodySchema = getNestedMap(applicationJson, "schema");
        
        // Assemble HTTP parameters and request body
        Map<String, Object>[] params = assembleParameters(actionInput, businessInput);
        Map<String, Object> header = params[0];
        Map<String, Object> query = params[1];
        Map<String, Object> path = params[2];
        Map<String, Object> body = assembleBody(bodySchema, actionInput, businessInput);
        
        // Construct Link system request payload
        Map<String, Object> runLinkPayload = new HashMap<>();
        Map<String, Object> runHeader = new HashMap<>();
        Map<String, Object> runParameter = new HashMap<>();
        Map<String, Object> runPayload = new HashMap<>();
        Map<String, Object> runMessage = new HashMap<>();
        
        runHeader.put("app_id", appId);
        runParameter.put("tool_id", toolId);
        runParameter.put("operation_id", operationId);
        runParameter.put("version", version);
        
        // Encode parameters for transmission
        Map<String, Object> callbackPayload = new HashMap<>();
        String headerStr = dumps(header);
        String queryStr = dumps(query);
        String pathStr = dumps(path);
        String bodyStr = dumps(body);
        
        // Add encoded parameters to payload if they exist
        if (headerStr != null) {
            runMessage.put("header", headerStr);
            callbackPayload.put("header", header);
        }
        if (queryStr != null) {
            runMessage.put("query", queryStr);
            callbackPayload.put("query", query);
        }
        if (bodyStr != null) {
            runMessage.put("body", bodyStr);
            callbackPayload.put("body", body);
        }
        if (pathStr != null) {
            runMessage.put("path", pathStr);
            callbackPayload.put("path", path);
        }
        
        runPayload.put("message", runMessage);
        runLinkPayload.put("header", runHeader);
        runLinkPayload.put("parameter", runParameter);
        runLinkPayload.put("payload", runPayload);
        
        log.info("Calling link service: toolId={}, operationId={}", toolId, operationId);
        
        // Make HTTP request to Link system
        String jsonBody = JSON.toJSONString(runLinkPayload);
        RequestBody requestBodyObj = RequestBody.create(
                jsonBody, 
                MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(runUrl)
                .post(requestBodyObj)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Link service call failed: " + response.code() + ", " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("Link service returned empty response");
            }
            
            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            // Check if response has header
            if (!jsonResponse.containsKey("header")) {
                throw new IOException("Invalid response format, missing 'header': " + responseBody);
            }
            
            // Process response and handle errors
            JSONObject headerObj = jsonResponse.getJSONObject("header");
            int code = headerObj.getIntValue("code");
            String message = headerObj.getString("message");
            
            if (code != 0) {
                // Handle tool execution errors
                throw new IOException("Tool execution failed: " + code + ", " + message);
            } else {
                // Extract and parse successful response
                JSONObject payloadObj = jsonResponse.getJSONObject("payload");
                if (payloadObj == null || !payloadObj.containsKey("text")) {
                    throw new IOException("Invalid response format, missing 'payload.text': " + responseBody);
                }
                JSONObject textObj = payloadObj.getJSONObject("text");
                String toolResponseText = textObj.getString("text");
                if (toolResponseText == null || toolResponseText.isEmpty()) {
                    throw new IOException("Invalid response format, missing 'text.text': " + responseBody);
                }
                return JSON.parseObject(toolResponseText, Map.class);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw e;
            } else {
                throw new IOException("Failed to parse response: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Helper method to get nested map value
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        if (map == null) {
            return new HashMap<>();
        }
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }
    
    /**
     * Helper method to get nested value
     */
    private Object getNestedValue(Map<String, Object> map, String... keys) {
        if (map == null || keys.length == 0) {
            return null;
        }
        
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(key);
            } else {
                return null;
            }
        }
        return current;
    }
}