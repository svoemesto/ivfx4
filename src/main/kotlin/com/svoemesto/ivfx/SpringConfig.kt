package com.svoemesto.ivfx

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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
class SpringConfig {


    @Bean
    fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        val currDb = getCurrentDatabase()
        if (currDb != null) {
            currDb.driver?.let { dataSource.setDriverClassName(it) }
            currDb.url?.let { dataSource.url = it }
            currDb.user?.let { dataSource.username = it }
            currDb.password?.let { dataSource.password = it }
        }
        return dataSource
    }

//    @Bean
//    fun dataSource(): DataSource {
//        val hikariConfig = HikariConfig()
//        val currDb = getCurrentDatabase()
//        hikariConfig.driverClassName = currDb!!.driver
//        hikariConfig.jdbcUrl = currDb.url
//        hikariConfig.username = currDb.user
//        hikariConfig.password = currDb.password
//        hikariConfig.maximumPoolSize = 5
//        hikariConfig.connectionTestQuery = "SELECT 1"
//        hikariConfig.poolName = "springHikariCP"
//        hikariConfig.addDataSourceProperty("dataSource.cachePrepStmts", "true")
//        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize", "250")
//        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", "2048")
//        hikariConfig.addDataSourceProperty("dataSource.useServerPrepStmts", "true")
//        return HikariDataSource(hikariConfig)
//    }

    @Bean
    fun jdbcTemplate(): JdbcTemplate {
        return JdbcTemplate(dataSource())
    }
}