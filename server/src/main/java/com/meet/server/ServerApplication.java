package com.meet.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class ServerApplication {

    private static final Logger log = LogManager.getLogger(ServerApplication.class);

    public static void main(String[] args) throws GitAPIException {
        SpringApplication.run(ServerApplication.class, args);
    }
}
