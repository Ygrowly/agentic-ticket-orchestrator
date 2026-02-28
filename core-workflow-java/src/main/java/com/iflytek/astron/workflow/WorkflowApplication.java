package com.iflytek.astron.workflow;

import com.iflytek.astron.link.tools.config.LinkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
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
    private static final Logger log = LoggerFactory.getLogger(WorkflowApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> applicationReadyEventListener() {
        return event -> {
            String version = WorkflowApplication.class.getPackage().getImplementationVersion();
            if (version == null || version.isBlank()) {
                version = "dev";
            }

            String port = event.getApplicationContext().getEnvironment().getProperty("local.server.port", "unknown");
            log.info("""

                ========================================
                  Java Workflow Engine Started!
                ========================================
                  Version: {}
                  Port: {}
                  Health: http://localhost:{}/actuator/health
                ========================================

                """, version, port, port);
        };
    }
}
