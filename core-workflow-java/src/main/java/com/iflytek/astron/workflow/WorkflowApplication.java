package com.iflytek.astron.workflow;

import com.iflytek.astron.link.tools.config.LinkConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Java Workflow 应用启动类
 * 
 * @author 二哥编程星球&Java进阶之路（沉默王二&一灰）
 * @version 1.0.0
 */
@SpringBootApplication
@Import(LinkConfiguration.class)
public class WorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
        System.out.println("""
            
            ========================================
              Java Workflow Engine Started!
            ========================================
              Version: 1.0.0-SNAPSHOT
              Port: 7880
              Health: http://localhost:7880/actuator/health
            ========================================
            
            """);
    }
}
