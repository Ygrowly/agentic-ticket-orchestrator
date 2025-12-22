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
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@Service
public class ToolManagementService {

    @Autowired
    private ToolCrudService toolCrudService;

    // 验证工具ID格式
    private static final Pattern TOOL_ID_PATTERN = Pattern.compile("^tool@[0-9a-f]+$");

    /**
     * 这里迁移了 Python 版本中的 create_version 函数逻辑
     *
     * @param toolsInfo
     * @return
     */
    public ToolManagerResponse createVersion(ToolManagerRequest toolsInfo) {
        try {
            // 处理工具创建逻辑
            List<ToolManagerRequest.SchemaInfo> tools = toolsInfo.getPayload().tools();
            List<ToolEntity> toolList = new ArrayList<>(tools.size());

            for (ToolManagerRequest.SchemaInfo toolInfo : tools) {
                ToolEntity tool = new ToolEntity();
                tool.setAppId(toolsInfo.getHeader().appId());
                // 生成工具ID
                String toolId = "tool@" + Long.toHexString(System.nanoTime());
                tool.setToolId(toolId);
                tool.setName(toolInfo.getName());
                tool.setDescription(toolInfo.getDescription());
                tool.setOpenApiSchema(Base64.decodeStr(toolInfo.getOpenapiSchema()));
                tool.setVersion("V1.0"); // 默认版本
                tool.setIsDeleted(0); // 未删除
                toolList.add(tool);
            }

            // 保存工具
            toolCrudService.addTools(toolList);

            // 构造响应数据
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
     * 这里迁移了 Python 版本中的 delete_version 函数逻辑
     *
     * @param appId
     * @param toolIds
     * @param versions
     * @return
     */
    public ToolManagerResponse deleteVersion(String appId, String[] toolIds, String[] versions) {
        // 验证输入参数
        if (toolIds.length == 0) {
            return ToolManagerResponse.error(LinkErrorCode.NO_TOOL_ID_PROVIDER);
        }

        for (String toolId : toolIds) {
            if (!TOOL_ID_PATTERN.matcher(toolId).matches()) {
                return ToolManagerResponse.error(LinkErrorCode.TOOL_NOT_EXIST_ERR, toolId);
            }
        }

        try {
            // 构造要删除的工具列表
            List<ToolEntity> toolsToDelete = new ArrayList<>();
            IntStream.range(0, toolIds.length).forEach(i -> {
                ToolEntity tool = new ToolEntity();
                tool.setAppId(appId);
                tool.setToolId(toolIds[i]);
                if (versions != null && i < versions.length) {
                    tool.setVersion(versions[i]);
                }
                tool.setIsDeleted(1); // 标记为已删除
                toolsToDelete.add(tool);
            });

            // 删除工具
            toolCrudService.deleteTools(toolsToDelete);

            // 设置响应数据
            return ToolManagerResponse.success(Map.of("message", "Tools deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting tools: ", e);
            return ToolManagerResponse.error(LinkErrorCode.UNKNOWN_ERR, e.getMessage());
        }
    }

    /**
     * 这里迁移了 Python 版本中的 update_version 函数逻辑
     *
     * @param toolsInfo
     * @return
     */
    public ToolManagerResponse updateVersion(ToolManagerRequest toolsInfo) {
        // 实现更新工具版本逻辑
        try {
            // 处理工具更新逻辑
            List<ToolManagerRequest.SchemaInfo> tools = toolsInfo.getPayload().tools();
            List<ToolEntity> toolList = new ArrayList<>();

            for (ToolManagerRequest.SchemaInfo toolInfo : tools) {
                ToolEntity tool = new ToolEntity();
                tool.setAppId(toolsInfo.getHeader().appId());
                tool.setToolId(toolInfo.getId());
                tool.setName(toolInfo.getName());
                tool.setDescription(toolInfo.getDescription());
                // toolInfo.getOpenapiSchema() 需要base64解码
                tool.setOpenApiSchema(Base64.decodeStr(toolInfo.getOpenapiSchema()));
                tool.setVersion(toolInfo.getVersion());
                tool.setIsDeleted(0); // 未删除
                toolList.add(tool);
            }

            // 更新工具
            toolCrudService.addToolVersion(toolList);
            return ToolManagerResponse.success(Map.of("message", "Tools updated successfully"));

        } catch (Exception e) {
            log.error("Error updating tools: ", e);
            return ToolManagerResponse.error(LinkErrorCode.UNKNOWN_ERR, e.getMessage());
        }
    }

    /**
     * 这里迁移了 Python 版本中的 read_version 函数逻辑
     *
     * @param appId
     * @param toolIds
     * @param versions
     * @return
     */
    public ToolManagerResponse readVersion(String appId, String[] toolIds, String[] versions) {
        // 实现读取工具版本逻辑
        // 验证输入参数
        if (toolIds.length == 0 || versions.length == 0 || toolIds.length != versions.length) {
            return ToolManagerResponse.error(LinkErrorCode.NO_TOOL_ID_PROVIDER);
        }

        // 验证工具ID格式
        for (String toolId : toolIds) {
            if (!TOOL_ID_PATTERN.matcher(toolId).matches()) {
                return ToolManagerResponse.error(LinkErrorCode.INVALID_TOOL_ID_FORMAT, toolId);
            }
        }

        try {
            // 构造要查询的工具列表
            List<ToolEntity> toolsToQuery = new ArrayList<>();
            IntStream.range(0, toolIds.length).forEachOrdered(i -> {
                ToolEntity tool = new ToolEntity();
                tool.setAppId(appId);
                tool.setToolId(toolIds[i]);
                tool.setVersion(versions[i]);
                tool.setIsDeleted(0); // 查询未删除
                toolsToQuery.add(tool);
            });

            // 查询工具
            List<ToolEntity> queriedTools = toolCrudService.getTools(toolsToQuery);

            // 构造响应数据
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