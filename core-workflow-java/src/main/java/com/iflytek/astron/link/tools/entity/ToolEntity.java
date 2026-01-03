package com.iflytek.astron.link.tools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tools_schema")
public class ToolEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("app_id")
    private String appId;

    @TableField("tool_id")
    private String toolId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("open_api_schema")
    private String openApiSchema;

    @TableField("version")
    private String version;

    @TableField("is_deleted")
    private int isDeleted;
}