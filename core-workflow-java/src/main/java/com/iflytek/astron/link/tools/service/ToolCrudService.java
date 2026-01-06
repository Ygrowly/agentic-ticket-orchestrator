package com.iflytek.astron.link.tools.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.astron.link.tools.entity.ToolEntity;
import com.iflytek.astron.link.tools.mapper.ToolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ToolCrudService extends ServiceImpl<ToolMapper, ToolEntity> {

    @Autowired
    private ToolMapper toolMapper;

    public void addTools(List<ToolEntity> tools) {
        // 实现添加工具逻辑，存储到数据库
        for (ToolEntity tool : tools) {
            toolMapper.insert(tool);
        }
    }

    public void deleteTools(List<ToolEntity> tools) {
        // 实现删除工具逻辑
        // 通过设置 isDeleted 标志来软删除
        for (ToolEntity tool : tools) {
            toolMapper.softDeleteByToolIdAndVersion(tool.getToolId(), tool.getVersion(), tool.getIsDeleted());
        }
    }

    public void addToolVersion(List<ToolEntity> tools) {
        // 实现添加工具版本逻辑
        // 先检查工具是否存在，如果存在则更新，否则插入新记录
        for (ToolEntity tool : tools) {
            // 检查工具是否存在
            QueryWrapper<ToolEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("tool_id", tool.getToolId()).eq("version", tool.getVersion());

            if (toolMapper.selectCount(queryWrapper) > 0) {
                // 工具已存在，更新它
                toolMapper.updateByToolIdAndVersion(tool);
            } else {
                // 工具不存在，插入新记录
                toolMapper.insert(tool);
            }
        }
    }

    public List<ToolEntity> getTools(List<ToolEntity> tools) {
        if (tools == null || tools.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<ToolMapper.ToolIdQuery> query = new ArrayList<>();
        for (ToolEntity tool : tools) {
            if (tool.getVersion() != null) {
                query.add(new ToolMapper.ToolIdQuery(tool.getToolId(), tool.getVersion()));
            } else {
                query.add(new ToolMapper.ToolIdQuery(tool.getToolId(), null));
            }
        }
        return toolMapper.selectByToolIdAndVersions(query);
    }

    public List<ToolEntity> getToolsByToolId(String appId, String toolId) {
        QueryWrapper<ToolEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", appId)
                    .eq("tool_id", toolId)
                    .eq("is_deleted", 0)
                    .orderByDesc("version");
        return toolMapper.selectList(queryWrapper);
    }
}