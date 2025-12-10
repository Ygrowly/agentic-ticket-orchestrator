package com.iflytek.astron.workflow.flow.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.iflytek.astron.workflow.components.id.IdUtil;
import com.iflytek.astron.workflow.controller.vo.WorkflowUpdateRequest;
import com.iflytek.astron.workflow.engine.domain.WorkflowDSL;
import com.iflytek.astron.workflow.engine.domain.chain.Node;
import com.iflytek.astron.workflow.flow.entity.WorkflowEntity;
import com.iflytek.astron.workflow.flow.mapper.WorkflowMapper;
import com.iflytek.astron.workflow.controller.vo.WorkflowAddRequest;
import com.iflytek.astron.workflow.controller.vo.SaveComparisonRequest;
import com.iflytek.astron.workflow.controller.vo.DeleteComparisonRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Workflow service
 * Handles workflow storage and retrieval, combined with ProtocolService functionality
 */
@Slf4j
@Service
public class WorkflowService {
    private final WorkflowMapper workflowMapper;

    public WorkflowService(WorkflowMapper workflowMapper) {
        this.workflowMapper = workflowMapper;
    }

    /**
     * Get workflow DSL by workflow ID
     *
     * @param workflowId workflow ID (e.g., "184736")
     * @return workflow DSL
     */
    public WorkflowDSL getWorkflowDSL(String workflowId) {
        if (log.isDebugEnabled()) {
            log.debug("Loading workflow: {}", workflowId);
        }

        LambdaQueryWrapper<WorkflowEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkflowEntity::getId, workflowId);

        WorkflowEntity entity = workflowMapper.selectOne(queryWrapper);

        if (entity == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        String dslData = JSONObject.parse(entity.getData()).getString("data");
        WorkflowDSL dsl = JSON.parseObject(dslData, WorkflowDSL.class);
        dsl.setFlowId(workflowId);

        if (log.isDebugEnabled()) {
            log.info("Loaded workflow: id={}, nodes={}, edges={}",
                    workflowId, dsl.getNodes().size(), dsl.getEdges().size());
        }
        return dsl;
    }

    /**
     * Save a new workflow
     *
     * @param request Workflow add request
     * @return Saved workflow entity
     */
    public WorkflowEntity saveWorkflow(WorkflowAddRequest request) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(IdUtil.genId());
        entity.setGroupId(request.getGroupId() != null ? request.getGroupId() : IdUtil.genId());
        entity.setName(request.getName() != null ? request.getName() : "");
        entity.setDescription(request.getDescription() != null ? request.getDescription() : "");
        entity.setAppId(request.getAppId() != null ? request.getAppId() : "");
        entity.setSource(request.getSource() != null ? request.getSource() : 0);
        entity.setVersion(request.getVersion() != null ? request.getVersion() : "");
        entity.setTag(request.getTag() != null ? request.getTag() : 0);

        // Convert data map to JSON string
        if (request.getData() != null) {
            entity.setData(JSON.toJSONString(request.getData()));
        } else {
            entity.setData("{}");
        }

        entity.setCreateAt(LocalDateTime.now());
        entity.setUpdateAt(LocalDateTime.now());

        workflowMapper.insert(entity);
        return entity;
    }

    /**
     * Get workflow by ID
     *
     * @param flowId Flow ID
     * @return Workflow entity
     */
    public WorkflowEntity getWorkflow(String flowId) {
        try {
            LambdaQueryWrapper<WorkflowEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WorkflowEntity::getId, Long.parseLong(flowId));
            WorkflowEntity entity = workflowMapper.selectOne(queryWrapper);
            if (entity == null) {
                throw new RuntimeException("Workflow not found: " + flowId);
            }
            return entity;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid flow ID format: " + flowId);
        }
    }

    /**
     * Update workflow
     *
     * @param flowId  Flow ID
     * @param request Update request
     */
    public void updateWorkflow(String flowId, WorkflowUpdateRequest request) {
        try {
            LambdaUpdateWrapper<WorkflowEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WorkflowEntity::getId, Long.parseLong(flowId));

            if (request.getName() != null) {
                updateWrapper.set(WorkflowEntity::getName, request.getName());
            }
            if (request.getDescription() != null) {
                updateWrapper.set(WorkflowEntity::getDescription, request.getDescription());
            }
            if (request.getAppId() != null) {
                updateWrapper.set(WorkflowEntity::getAppId, request.getAppId());
            }
            if (request.getData() != null) {
                updateWrapper.set(WorkflowEntity::getData, JSON.toJSONString(request.getData()));
            }

            updateWrapper.set(WorkflowEntity::getUpdateAt, LocalDateTime.now());

            workflowMapper.update(null, updateWrapper);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid flow ID format: " + flowId);
        }
    }

    /**
     * Delete workflow
     *
     * @param flowId Flow ID
     */
    public void deleteWorkflow(String flowId) {
        try {
            LambdaQueryWrapper<WorkflowEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WorkflowEntity::getId, Long.parseLong(flowId));
            workflowMapper.delete(queryWrapper);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid flow ID format: " + flowId);
        }
    }

    /**
     * Validate workflow data
     *
     * @param data Workflow data
     * @return WorkflowDSL
     */
    public WorkflowDSL validateWorkflow(Map<String, Object> data) {
        // Convert data to JSON string and then parse as WorkflowDSL
        String jsonData = JSON.toJSONString(data);
        JSONObject jsonObject = JSON.parseObject(jsonData);
        String dslData = jsonObject.getString("data");

        if (dslData != null) {
            WorkflowDSL dsl = JSON.parseObject(dslData, WorkflowDSL.class);
            // Validate the workflow DSL
            verifyWorkflow(dsl);
            return dsl;
        } else {
            throw new RuntimeException("Invalid workflow data format");
        }
    }

    /**
     * Build workflow
     *
     * @param flowId Flow ID
     */
    public void buildWorkflow(String flowId) {
        // Get workflow data
        WorkflowEntity entity = getWorkflow(flowId);

        // Parse workflow DSL
        WorkflowDSL dsl = parseWorkflowDSL(entity);

        // Validate workflow
        verifyWorkflow(dsl);

        log.info("Building workflow: {}", flowId);
    }

    /**
     * Generate MCP input schema
     *
     * @param flowId           Flow ID
     * @param consumerUsername Consumer username
     * @return MCP input schema
     */
    public Map<String, Object> generateMcpInputSchema(String flowId, String consumerUsername) {
        // Get workflow data
        WorkflowEntity entity = getWorkflow(flowId);

        // Parse workflow DSL
        WorkflowDSL dsl = parseWorkflowDSL(entity);

        // Validate workflow
        verifyWorkflow(dsl);

        log.info("Generating MCP input schema for flow: {}, user: {}", flowId, consumerUsername);

        // Return empty map for now
        return Map.of();
    }

    /**
     * Save comparison
     *
     * @param request Save comparison request
     */
    public void saveComparison(SaveComparisonRequest request) {
        try {
            // Get original workflow
            WorkflowEntity originalEntity = getWorkflow(request.getFlowId());

            // Create comparison entity
            WorkflowEntity comparisonEntity = new WorkflowEntity();
            comparisonEntity.setGroupId(originalEntity.getGroupId());
            comparisonEntity.setName(originalEntity.getName());
            comparisonEntity.setDescription(originalEntity.getDescription());
            comparisonEntity.setAppId(originalEntity.getAppId());
            comparisonEntity.setSource(originalEntity.getSource());
            comparisonEntity.setVersion(request.getVersion());
            comparisonEntity.setTag(1); // Comparison tag (1 = COMPARISON)

            // Set comparison data
            if (request.getData() != null) {
                comparisonEntity.setData(JSON.toJSONString(request.getData()));
            } else {
                comparisonEntity.setData("{}");
            }

            comparisonEntity.setCreateAt(LocalDateTime.now());
            comparisonEntity.setUpdateAt(LocalDateTime.now());

            workflowMapper.insert(comparisonEntity);
        } catch (Exception e) {
            log.error("Failed to save comparison", e);
            throw new RuntimeException("Failed to save comparison: " + e.getMessage());
        }
    }

    /**
     * Delete comparison
     *
     * @param request Delete comparison request
     */
    public void deleteComparison(DeleteComparisonRequest request) {
        try {
            // Get original workflow
            WorkflowEntity originalEntity = getWorkflow(request.getFlowId());

            // Delete comparison records
            LambdaQueryWrapper<WorkflowEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WorkflowEntity::getGroupId, originalEntity.getGroupId());
            queryWrapper.eq(WorkflowEntity::getVersion, request.getVersion());

            workflowMapper.delete(queryWrapper);
        } catch (Exception e) {
            log.error("Failed to delete comparison", e);
            throw new RuntimeException("Failed to delete comparison: " + e.getMessage());
        }
    }

    /**
     * Parse WorkflowDSL from workflow entity
     *
     * @param entity Workflow entity
     * @return WorkflowDSL
     */
    private WorkflowDSL parseWorkflowDSL(WorkflowEntity entity) {
        if (entity.getData() == null || entity.getData().isEmpty()) {
            throw new RuntimeException("Workflow data is empty");
        }

        JSONObject jsonObject = JSON.parseObject(entity.getData());
        String dslData = jsonObject.getString("data");

        if (dslData != null) {
            return JSON.parseObject(dslData, WorkflowDSL.class);
        } else {
            throw new RuntimeException("Invalid workflow data format");
        }
    }

    /**
     * Verify workflow DSL
     *
     * @param workflowDSL Workflow DSL to verify
     */
    private void verifyWorkflow(WorkflowDSL workflowDSL) {
        // A simple DSL validation, requires start and end nodes, and each node must have a corresponding executor
        if (CollectionUtils.isEmpty(workflowDSL.getNodes()) || CollectionUtils.isEmpty(workflowDSL.getEdges())) {
            throw new IllegalStateException("Invalid workflow DSL: missing start or end node");
        }

        boolean hasStartNode = false;
        boolean hasEndNode = false;

        for (Node node : workflowDSL.getNodes()) {
            if (node.getNodeType() == null) {
                throw new IllegalStateException("Invalid workflow DSL: node type is null");
            }

            // Check for start and end nodes
            if (node.getNodeType().name().equals("START")) {
                hasStartNode = true;
            } else if (node.getNodeType().name().equals("END")) {
                hasEndNode = true;
            }
        }

        if (!hasStartNode || !hasEndNode) {
            throw new IllegalStateException("Invalid workflow DSL: missing start or end node");
        }

        // TODO: Add more validation logic as needed
    }
}