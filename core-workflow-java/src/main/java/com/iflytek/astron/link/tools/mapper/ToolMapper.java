package com.iflytek.astron.link.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.astron.link.tools.entity.ToolEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Tool mapper
 */
@Mapper
public interface ToolMapper extends BaseMapper<ToolEntity> {
    /**
     * 根据 toolId 和 version 查询工具
     */
    List<ToolEntity> selectByToolIdAndVersion(@Param("toolId") String toolId, @Param("version") String version);

    List<ToolEntity> selectByToolIdAndVersions(@Param("query") List<ToolIdQuery> query);

    /**
     * 软删除工具
     */
    int softDeleteByToolIdAndVersion(@Param("toolId") String toolId, @Param("version") String version, @Param("isDeleted") int isDeleted);

    /**
     * 根据 toolId 和 version 更新工具
     */
    int updateByToolIdAndVersion(ToolEntity tool);

    record ToolIdQuery(@Param("toolId") String toolId, @Param("version") String version) {
    }
}