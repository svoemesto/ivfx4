package com.svoemesto.ivfx

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

@Configuration
@ComponentScan("com.svoemesto.ivfx")
@PropertySource("/application.properties")
class SpringConfig() {

    @Bean
    fun dataSource(): DataSource {
        var dataSource: DriverManagerDataSource = DriverManagerDataSource()
        val currDb = getCurrentDatabase()
        if (currDb != null) {
            currDb.driver?.let { dataSource.setDriverClassName(it) }
            currDb.url?.let { dataSource.url = it }
            currDb.user?.let { dataSource.username = it }
            currDb.password?.let { dataSource.password = it }
        }
        return dataSource
    }

    @Bean
    fun jdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(dataSource())
    }
}