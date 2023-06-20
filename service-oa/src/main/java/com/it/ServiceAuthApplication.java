package com.it;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 启动类
 *
 * @author 杨振华
 * @since 2023/6/20
 */
@SpringBootApplication
@ComponentScan("com.it")
@Slf4j
public class ServiceAuthApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ServiceAuthApplication.class);
        ConfigurableEnvironment env = application.run(args).getEnvironment();
        log.info("success start!");
        log.info("knife4j API URL: \thttp://localhost:{}/doc.html",env.getProperty("server.port"));
    }

}