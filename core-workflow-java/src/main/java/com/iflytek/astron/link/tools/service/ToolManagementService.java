package com.iflytek.astron.link.tools.service;

import cn.hutool.core.codec.Base64;
import com.iflytek.astron.link.constant.LinkErrorCode;
import com.iflytek.astron.link.controller.vo.req.ToolManagerRequest;
import com.iflytek.astron.link.controller.vo.res.ToolManagerResponse;
import com.iflytek.astron.link.tools.entity.ToolEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@Service
public class ToolManagementService {

    @Autowired
    private ToolCrudService toolCrudService;

    private static final Pattern TOOL_ID_PATTERN = Pattern.compile("^tool@[0-9a-f]+$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)\\.(\\d+)$");

    private String getLatestVersion(String appId, String toolId) {
        List<ToolEntity> existingTools = toolCrudService.getToolsByToolId(appId, toolId);
        if (existingTools.isEmpty()) {
            return null;
        }
        
        return existingTools.stream()
                .map(ToolEntity::getVersion)
                .max((v1, v2) -> compareVersions(v1, v2))
                .orElse(null);
    }

    private String incrementVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isEmpty()) {
            return "V1.0";
        }
        
        Matcher matcher = VERSION_PATTERN.matcher(currentVersion);
        if (!matcher.matches()) {
            log.warn("Invalid version format: {}, defaulting to V1.0", currentVersion);
            return "V1.0";
        }
        
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        
        major++;
        
        return String.format("V%d.%d", major, minor);
    }

    private int compareVersions(String v1, String v2) {
        Matcher m1 = VERSION_PATTERN.matcher(v1);
        Matcher m2 = VERSION_PATTERN.matcher(v2);
        
        if (!m1.matches() || !m2.matches()) {
            return v1.compareTo(v2);
        }
        
        int major1 = Integer.parseInt(m1.group(1));
        int minor1 = Integer.parseInt(m1.group(2));
        int major2 = Integer.parseInt(m2.group(1));
        int minor2 = Integer.parseInt(m2.group(2));
        
        if (major1 != major2) {
            return Integer.compare(major1, major2);
        }
        return Integer.compare(minor1, minor2);
    }

    /**
     * 创建工具版本
     *
     * @param toolsInfo
     * @return
     */
    public ToolManagerResponse createVersion(ToolManagerRequest toolsInfo) {
        log.info("Creating tool version, appId: {}, toolCount: {}", 
                toolsInfo.getHeader().appId(), toolsInfo.getPayload().tools().size());
        try {
            List<ToolManagerRequest.SchemaInfo> tools = toolsInfo.getPayload().tools();
            List<ToolEntity> toolList = new ArrayList<>(tools.size());

            for (ToolManagerRequest.SchemaInfo toolInfo : tools) {
                ToolEntity tool = new ToolEntity();
                tool.setAppId(toolsInfo.getHeader().appId());
                String toolId = "tool@" + Long.toHexString(System.nanoTime());
                tool.setToolId(toolId);
                tool.setName(toolInfo.getName());
                tool.setDescription(toolInfo.getDescription());
                tool.setOpenApiSchema(Base64.decodeStr(toolInfo.getOpenapiSchema()));
                tool.setVersion("V1.0");
                tool.setIsDeleted(0);
                toolList.add(tool);
                log.info("Generated toolId: {} for tool: {}", toolId, toolInfo.getName());
            }

            toolCrudService.addTools(toolList);
            log.info("Successfully created {} tools", toolList.size());

            List<Map<String, String>> responseTools = new ArrayList<>();
            for (ToolEntity tool : toolList) {
                Map<String, String> responseTool = new HashMap<>();
                responseTool.put("name", tool.getName());
                responseTool.put("id", tool.getToolId());
                responseTool.put("version", tool.getVersion());
                responseTools.add(responseTool);
            }

            return ToolManagerResponse.success(Map.of("tools", responseTools));
        } catch (Exception e) {
            log.error("Error creating tools: ", e);
            return ToolManagerResponse.error(LinkErrorCode.UNKNOWN_ERR, e.getMessage());
        }
    }

    /**
     * 删除指定工具版本
     *
     * @param appId
     * @param toolIds
     * @param versions
     * @return
     */
    public ToolManagerResponse deleteVersion(String appId, String[] toolIds, String[] versions) {
        log.info("Deleting tool version, appId: {}, toolIds: {}, versions: {}", 
                appId, toolIds, versions);
        if (toolIds.length == 0) {
            log.warn("No tool IDs provided for deletion");
            return ToolManagerResponse.error(LinkErrorCode.NO_TOOL_ID_PROVIDER);
        }

        for (String toolId : toolIds) {
            if (!TOOL_ID_PATTERN.matcher(toolId).matches()) {
                log.warn("Invalid tool ID format: {}", toolId);
                return ToolManagerResponse.error(LinkErrorCode.TOOL_NOT_EXIST_ERR, toolId);
            }
        }

        try {
            List<ToolEntity> toolsToDelete = new ArrayList<>();
            IntStream.range(0, toolIds.length).forEach(i -> {
                ToolEntity tool = new ToolEntity();
                tool.setAppId(appId);
                tool.setToolId(toolIds[i]);
                if (versions != null && i < versions.length) {
                    tool.setVersion(versions[i]);
                }
                tool.setIsDeleted(1);
                toolsToDelete.add(tool);
            });

            toolCrudService.deleteTools(toolsToDelete);
            log.info("Successfully deleted {} tools", toolsToDelete.size());

            return ToolManagerResponse.success(Map.of("message", "Tools deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting tools: ", e);
            return ToolManagerResponse.error(LinkErrorCode.UNKNOWN_ERR, e.getMessage());
        }
    }

    /**
     * 实现更新工具版本逻辑
     *
     * @param toolsInfo
     * @return
     */
    public ToolManagerResponse updateVersion(ToolManagerRequest toolsInfo) {
        log.info("Updating tool version, appId: {}, toolCount: {}", 
                toolsInfo.getHeader().appId(), toolsInfo.getPayload().tools().size());
        try {
            List<ToolManagerRequest.SchemaInfo> tools = toolsInfo.getPayload().tools();
            List<ToolEntity> toolList = new ArrayList<>();

            for (ToolManagerRequest.SchemaInfo toolInfo : tools) {
                String latestVersion = getLatestVersion(toolsInfo.getHeader().appId(), toolInfo.getId());
                String newVersion = incrementVersion(latestVersion);
                
                ToolEntity tool = new ToolEntity();
                tool.setAppId(toolsInfo.getHeader().appId());
                tool.setToolId(toolInfo.getId());
                tool.setName(toolInfo.getName());
                tool.setDescription(toolInfo.getDescription());
                tool.setOpenApiSchema(Base64.decodeStr(toolInfo.getOpenapiSchema()));
                tool.setVersion(newVersion);
                tool.setIsDeleted(0);
                toolList.add(tool);
                log.info("Updating toolId: {}, oldVersion: {}, newVersion: {}", 
                        toolInfo.getId(), latestVersion, newVersion);
            }

            toolCrudService.addToolVersion(toolList);
            log.info("Successfully updated {} tools", toolList.size());
            return ToolManagerResponse.success(Map.of("message", "Tools updated successfully"));

        } catch (Exception e) {
            log.error("Error updating tools: ", e);
            return ToolManagerResponse.error(LinkErrorCode.UNKNOWN_ERR, e.getMessage());
        }
    }

    /**
     * 实现读取工具版本逻辑
     *
     * @param appId
     * @param toolIds
     * @param versions
     * @return
     */
    public ToolManagerResponse readVersion(String appId, String[] toolIds, String[] versions) {
        log.info("Reading tool version, appId: {}, toolIds: {}, versions: {}", 
                appId, toolIds, versions);
        if (toolIds.length == 0 || versions.length == 0 || toolIds.length != versions.length) {
            log.warn("Invalid parameters: toolIds length={}, versions length={}", 
                    toolIds.length, versions.length);
            return ToolManagerResponse.error(LinkErrorCode.NO_TOOL_ID_PROVIDER);
        }

        for (String toolId : toolIds) {
            if (!TOOL_ID_PATTERN.matcher(toolId).matches()) {
                log.warn("Invalid tool ID format: {}", toolId);
                return ToolManagerResponse.error(LinkErrorCode.INVALID_TOOL_ID_FORMAT, toolId);
            }
        }

        try {
            List<ToolEntity> toolsToQuery = new ArrayList<>();
            IntStream.range(0, toolIds.length).forEachOrdered(i -> {
                ToolEntity tool = new ToolEntity();
                tool.setAppId(appId);
                tool.setToolId(toolIds[i]);
                tool.setVersion(versions[i]);
                tool.setIsDeleted(0);
                toolsToQuery.add(tool);
            });

            List<ToolEntity> queriedTools = toolCrudService.getTools(toolsToQuery);
            log.info("Successfully queried {} tools", queriedTools.size());

            List<Map<String, String>> responseTools = new ArrayList<>();
            for (ToolEntity tool : queriedTools) {
                Map<String, String> responseTool = new HashMap<>();
                responseTool.put("name", tool.getName());
                responseTool.put("description", tool.getDescription());
                responseTool.put("id", tool.getToolId());
                responseTool.put("schema", tool.getOpenApiSchema());
                responseTool.put("version", tool.getVersion());
                responseTools.add(responseTool);
            }

            return ToolManagerResponse.success(Map.of("tools", responseTools));
        } catch (Exception e) {
            log.error("Error reading tools: ", e);
            return ToolManagerResponse.error(LinkErrorCode.UNKNOWN_ERR, e.getMessage());
        }
    }
}