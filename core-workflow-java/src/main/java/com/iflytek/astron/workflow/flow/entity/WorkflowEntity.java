package com.iflytek.astron.workflow.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow entity
 */
@Data
@TableName("flow")
public class WorkflowEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private Long groupId;

    /**
     * 协议名称
     */
    @TableField("name")
    private String name;

    /**
     * DSL 工作流数据
     */
    @TableField("data")
    private String data;

    /**
     * DSL 工作流发布数据
     */
    @TableField("release_data")
    private String releaseData;

    /**
     * 工作流描述
     */
    @TableField("description")
    private String description;

    /**
     * 工作流版本
     */
    @TableField("version")
    private String version;

    /**
     * 工作流发布状态
     */
    @TableField("release_status")
    private Integer releaseStatus;

    /**
     * 工作流所属应用
     */
    @TableField("app_id")
    private String appId;

    /**
     * 工作流来源
     */
    @TableField("source")
    private Integer source;

    /**
     * 标记工作流标签 0：无标签；1：对照组
     */
    @TableField("tag")
    private Integer tag;

    /**
     * 创建人
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 修改人
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 创建时间
     */
    @TableField("create_at")
    private LocalDateTime createAt;

    /**
     * 修改时间
     */
    @TableField("update_at")
    private LocalDateTime updateAt;
}
