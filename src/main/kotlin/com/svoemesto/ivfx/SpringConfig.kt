package com.svoemesto.ivfx

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionTemplate
import java.util.Properties
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
@ComponentScan("com.svoemesto.ivfx")
@PropertySource("/application.properties")
// Три аннотации ниже появились в задаче #236. До них в контексте не было ни
// фабрики сущностей, ни регистрации репозиториев, ни менеджера транзакций,
// из-за чего Main.context.getBean("propertyRepo") падал и приложение не
// запускалось вовсе.
//
// Выбор JPA, а не Spring Data JDBC, не мой: проект написан под JPA целиком.
// Все 20 моделей размечены @Table(name = "tbl_…") из javax.persistence, в
// репозиториях 89 нативных запросов с nativeQuery = true в 19 файлах, а
// колонки id в базе помечены is_identity — их породил Hibernate при
// @GeneratedValue(strategy = IDENTITY). Перевод на JDBC означал бы
// переписывание 89 запросов и разметки 20 моделей.
@EnableJpaRepositories(basePackages = ["com.svoemesto.ivfx.repos"])
@EntityScan(basePackages = ["com.svoemesto.ivfx.models"])
@EnableTransactionManagement
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

    @Bean
    fun jdbcTemplate(): JdbcTemplate = JdbcTemplate(dataSource())

    /**
     * Фабрика сущностей. Без неё JPA в проекте не работал нигде.
     *
     * `hbm2ddl.auto = none` поставлен жёстко и не зависит от application.properties.
     * Настройка `spring.jpa.hibernate.ddl-auto=update` там была инертной, пока
     * Hibernate не существовал; с появлением фабрики она ожила бы и Hibernate
     * начал бы менять схему у базы, где уже 807 тысяч кадров. Никаких правок
     * схемы приложению не нужно — она создана этим же Hibernate.
     *
     * Диалект намеренно не задан: база выбирается оператором (Postgres, MySQL
     * или встроенная H2), и Hibernate определит его по метаданным соединения.
     * Жёстко прописанный диалект сломал бы две базы из трёх.
     */
    @Bean
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val emf = LocalContainerEntityManagerFactoryBean()
        emf.setDataSource(dataSource)
        emf.setPackagesToScan("com.svoemesto.ivfx.models")
        emf.setPersistenceUnitName("ivfx")
        emf.setJpaVendorAdapter(HibernateJpaVendorAdapter())
        emf.setJpaProperties(
            Properties().apply {
                // Стратегия именования колонок. Без неё Hibernate подставляет имена
                // дословно из полей: faceCount -> faceCount -> в Postgres facecount,
                // а в таблице лежит face_count. Падало с
                //   ERROR: column facetrack0_.facecount does not exist
                // Схему создавал Hibernate под Spring Boot, где эта стратегия
                // подставляется автоматически; вне Boot её нужно назвать явно.
                // Затрагивает все модели с полями без @Column, а не одну.
                setProperty(
                    "hibernate.physical_naming_strategy",
                    "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy",
                )
                setProperty("hibernate.hbm2ddl.auto", "none")
                setProperty("hibernate.show_sql", "false")
                setProperty("hibernate.jdbc.batch_size", "50")
                // Код написан в расчёте на сущности без сессии: JPA в проекте не
                // было, сущности были обычными объектами, и связи читались где
                // угодно (file.tracks в ProjectEditFXController, каскад при
                // удалении файла). С включением JPA ленивая загрузка стала
                // настоящей, и каждое такое чтение вне транзакции падало бы с
                // LazyInitializationException. Свойство разрешает открыть сессию
                // по требованию ровно там, где её раньше не было.
                setProperty("hibernate.enable_lazy_load_no_trans", "true")
            },
        )
        return emf
    }

    /**
     * Менеджер транзакций. До него @Transactional на репозиториях была
     * инертна: Spring не создавал прокси-транзакции, и каждая запись
     * коммитилась отдельно. Это же стоило данных в задаче #234 — удаление
     * файла падало на внешнем ключе, а планы, кадры и лица к этому моменту
     * уже были удалены неоткатываемо.
     */
    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager =
        JpaTransactionManager(entityManagerFactory)

    /**
     * Обёртка для цепочки операций, которая должна быть одной транзакцией.
     *
     * Нужна потому, что контроллеры вызываются напрямую, минуя прокси: на
     * них `@Transactional` не действует, и каждый вызов репозитория коммитится
     * отдельно. Из-за этого удаление файла падало в середине, оставляя файл
     * в списке с уже стёртым анализом (#234).
     */
    @Bean
    fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate = TransactionTemplate(transactionManager)
}
