package com.iflytek.astron.link.controller.tools;

import com.iflytek.astron.link.controller.vo.req.HttpToolRunRequest;
import com.iflytek.astron.link.controller.vo.req.ToolDebugRequest;
import com.iflytek.astron.link.controller.vo.res.HttpToolRunResponse;
import com.iflytek.astron.link.controller.vo.res.ToolDebugResponse;
import com.iflytek.astron.link.execution.ToolExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolExecutionController {

    @Autowired
    private ToolExecutionService toolExecutionService;

    @PostMapping("/http_run")
    public HttpToolRunResponse httpRun(@RequestBody HttpToolRunRequest runParams) {
        return toolExecutionService.httpRun(runParams);
    }

    @PostMapping("/tool_debug")
    public ToolDebugResponse toolDebug(@RequestBody ToolDebugRequest toolDebugParams) {
        return toolExecutionService.toolDebug(toolDebugParams);
    }
}