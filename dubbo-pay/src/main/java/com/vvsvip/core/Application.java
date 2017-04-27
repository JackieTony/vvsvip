package com.vvsvip.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource(locations = {"classpath:spring-cloud.xml", "classpath:spring-data.xml"}) //引入xml
@ComponentScan(basePackages = "com.vvsvip.*")
public class Application extends SpringBootServletInitializer implements EmbeddedServletContainerCustomizer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    private static int port = 8082;

    public static void main(String[] args) throws Exception {
        Object[] sources = new Object[]{Application.class};
        if (args.length > 0) {
            String portStr = args[0];
            if (portStr != null) {
                port = Integer.valueOf(portStr);
            }
        }
        SpringApplication.run(sources, args);

    }

    @Override
    public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
        configurableEmbeddedServletContainer.setPort(port);
    }
}