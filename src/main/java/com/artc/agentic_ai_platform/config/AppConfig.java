package com.artc.agentic_ai_platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Storage storage = new Storage();
    private Queue queue = new Queue();
    private Agents agents = new Agents();

    @Data
    public static class Storage {
        private String backend; // "ram" or "redis"
        private RamConfig ram = new RamConfig();
        private long defaultTtlSec = 3600;
    }

    @Data
    public static class RamConfig {
        private int maxEntries = 100000;
    }

    @Data
    public static class Queue {
        private String backend;
        private Concurrency concurrency = new Concurrency();

        @Data
        public static class Concurrency {
            private int workers = 4;
            private int maxRetries = 3;
            private String backoffStrategy = "exponential";
        }
    }

    @Data
    public static class Agents {
        private WorkerConfig workers = new WorkerConfig();
        private PlannerConfig planner = new PlannerConfig();
        private ReviewerConfig reviewer = new ReviewerConfig();

        @Data
        public static class PlannerConfig {
            private boolean enabled = true;
        }

        @Data
        public static class ReviewerConfig {
            private boolean enabled = true;
        }

        @Data
        public static class WorkerConfig {
            private int poolSize = 4;
            private boolean enabled = true;
        }
    }
}
