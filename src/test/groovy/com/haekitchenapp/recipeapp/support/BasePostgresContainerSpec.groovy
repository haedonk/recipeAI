package com.haekitchenapp.recipeapp.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
abstract class BasePostgresContainerSpec extends Specification {

    @Shared
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.3-alpine")
            .withDatabaseName("recipe-ai-test")
            .withUsername("test")
            .withPassword("test")

    def setupSpec() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start()
        }
    }

    def cleanupSpec() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop()
        }
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES.&getJdbcUrl)
        registry.add("spring.datasource.username", POSTGRES.&getUsername)
        registry.add("spring.datasource.password", POSTGRES.&getPassword)
        registry.add("spring.datasource.driver-class-name", POSTGRES.&getDriverClassName)
        registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.PostgreSQLDialect" }
        registry.add("spring.test.database.replace") { "NONE" }
    }
}
