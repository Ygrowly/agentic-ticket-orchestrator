package com.iflytek.astron.link.controller.tools;

import com.iflytek.astron.link.controller.vo.req.ToolManagerRequest;
import com.iflytek.astron.link.controller.vo.res.ToolManagerResponse;
import com.iflytek.astron.link.tools.service.ToolManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工具管理端点
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolManagementController {

    @Autowired
    private ToolManagementService toolManagementService;

    @PostMapping("/versions")
    public ToolManagerResponse createVersion(@RequestBody ToolManagerRequest toolsInfo) {
        return toolManagementService.createVersion(toolsInfo);
    }

    @PutMapping("/versions")
    public ToolManagerResponse updateVersion(@RequestBody ToolManagerRequest toolsInfo) {
        return toolManagementService.updateVersion(toolsInfo);
    }

    @DeleteMapping("/versions")
    public ToolManagerResponse deleteVersion(
            @RequestParam(name = "app_id") String appId,
            @RequestParam(name = "tool_ids") String[] toolIds,
            @RequestParam(required = false) String[] versions) {
        return toolManagementService.deleteVersion(appId, toolIds, versions);
    }

    @GetMapping("/versions")
    public ToolManagerResponse readVersion(
            @RequestParam(name = "app_id") String appId,
            @RequestParam(name = "tool_ids") String[] toolIds,
            @RequestParam(name = "versions") String[] versions) {
        return toolManagementService.readVersion(appId, toolIds, versions);
    }
}