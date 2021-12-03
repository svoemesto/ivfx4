package com.svoemesto.ivfx

import com.svoemesto.ivfx.fxcontrollers.DatabaseSelectFXController
import com.svoemesto.ivfx.utils.ComputerIdentifier
import org.h2.jdbc.JdbcSQLSyntaxErrorException
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement


const val H2DB_EMBEDDED_NAME = "<Встроенная база данных>"
const val H2DB_EMBEDDED_DRIVER = "org.h2.Driver"
const val H2DB_EMBEDDED_URL = "jdbc:h2:./ivfxdb"
const val H2DB_EMBEDDED_USER = "sa"
const val H2DB_EMBEDDED_PASSWORD = ""
const val H2DB_PROPERTYKEY_CURRENTDB_ID = "CURRENTDB_ID"
const val H2DB_PROPERTYKEY_CURRENTCOMPUTER_ID = "CURRENTCOMPUTER_ID"


fun main() {

    initializeH2db()

    DatabaseSelectFXController.getDatabase(null)

}

fun deleteH2Property(h2property: H2properties) {
    if (h2property.key != null) {
        val st = getConnection().createStatement()
        var sql = "delete from tbl_properties where key = '${h2property.key}';"
        st.execute(sql)
    }
}

fun deleteH2Database(h2database: H2database) {
    if (h2database.id != null) {
        val st = getConnection().createStatement()
        var sql = "delete from tbl_databases where id = ${h2database.id};"
        st.execute(sql)
    }
}

fun saveH2Property(h2property: H2properties) {

    if (h2property.key != null) {

        val st = getConnection().createStatement()
        var sql = "select * from tbl_properties where key = '${h2property.key}';"
        val rs = st.executeQuery(sql)
        val valueToUpdate = if (h2property.value == null) "NULL" else "'${h2property.value}'"
        if (rs.next()) {
            sql = "update tbl_properties set value = $valueToUpdate where key = '${h2property.key}';"
        } else {
            sql = "insert into tbl_properties (key, value) values ('${h2property.key}', $valueToUpdate);"
        }
        st.execute(sql)

    }
}

fun saveH2database(h2database: H2database) : Int{

    var st = getConnection().createStatement()
    var sql: String
    var rs: ResultSet

    if (h2database.id != null) {
        sql = "select * from tbl_databases where id = ${h2database.id};"
        rs = st.executeQuery(sql)
        if (rs.next()) {
            sql = "update tbl_databases set " +
                    "name = '${h2database.name}', " +
                    "driver = '${h2database.driver}', " +
                    "url = '${h2database.url}', " +
                    "user = '${h2database.user}', " +
                    "password = '${h2database.password}' " +
                    "where id = ${h2database.id};"
            st.execute(sql)
            println("Обновлена запись для базы данных «${h2database.name}» с идентификатором ${h2database.id}")
            return h2database.id!!
        } else {
            sql = "insert into TBL_DATABASES (ID, NAME, DRIVER, URL, USER, PASSWORD) values (" +
                    "${h2database.id}, " +
                    "'${h2database.name}', " +
                    "'${h2database.driver}', " +
                    "'${h2database.url}', " +
                    "'${h2database.user}', " +
                    "'${h2database.password}'" +
                    ");"
            st.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS)
            rs = st.generatedKeys
            if (rs.next()) {
                h2database.id = rs.getInt("id")
                return h2database.id!!
                println("Создана запись для базы данных «${h2database.name}» с идентификатором ${h2database.id}")
            } else {
                return 0
            }
        }
    } else {
        sql = "insert into TBL_DATABASES (NAME, DRIVER, URL, USER, PASSWORD) values (" +
                "'${h2database.name}', " +
                "'${h2database.driver}', " +
                "'${h2database.url}', " +
                "'${h2database.user}', " +
                "'${h2database.password}'" +
                ");"
        st.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS)
        rs = st.generatedKeys
        if (rs.next()) {
            h2database.id = rs.getInt("id")
            println("Создана запись для базы данных «${h2database.name}» с идентификатором ${h2database.id}")
            return h2database.id!!
        } else {
            return 0
        }
    }

}

fun getH2Property(key : String) : H2properties? {
    val h2property = H2properties()
    val rs = getConnection().createStatement().executeQuery("select * from tbl_properties where key = '$key';")
    if (rs.next()) {
        h2property.key = rs.getString("key")
        h2property.value = rs.getString("value")
    }
    return if (h2property.key == null) null else h2property
}

fun getPropertyValue(key : String) : String? {
    val h2property = getH2Property(key)
    return if (h2property == null) null else h2property.value
}

fun setPropertyValue(key : String, value : String?) {

    val st = getConnection().createStatement()
    var sql = "select * from tbl_properties where key = '$key';"
    val rs = st.executeQuery(sql)
    val valueToUpdate = if (value == null) "NULL" else "'$value'"
    if (rs.next()) {
        sql = "update tbl_properties set value = $valueToUpdate where key = '$key';"
    } else {
        sql = "insert into tbl_properties (key, value) values ('$key', $valueToUpdate);"
    }
    st.execute(sql)

}

fun getCurrentComputerId() : Int {
    return ComputerIdentifier.getComputerId()
}

fun getCurrentDatabase(): H2database? {
    return getPropertyValue(H2DB_PROPERTYKEY_CURRENTDB_ID)?.let { getH2database(it.toInt()) }
}

fun setDatabaseAsCurrent(h2database: H2database) {
    setPropertyValue(H2DB_PROPERTYKEY_CURRENTDB_ID, h2database.id.toString())
}

fun getH2database(id: Int) : H2database? {

    val st = getConnection().createStatement()
    var sql = "select * from tbl_databases where id = $id;"
    val rs = st.executeQuery(sql)
    if (rs.next()) {

        val h2database = H2database()
        h2database.id = rs.getInt("id")
        h2database.name = rs.getString("name")
        h2database.driver = rs.getString("driver")
        h2database.url = rs.getString("url")
        h2database.user = rs.getString("user")
        h2database.password = rs.getString("password")
        return h2database

    } else {
        return null
    }

}

fun getListH2Properties() : List<H2properties> {

    val result = mutableListOf<H2properties>()
    val rs = getConnection().createStatement().executeQuery("select * from tbl_properties;")
    while (rs.next()) {

        val h2property = H2properties()
        h2property.key = rs.getString("key")
        h2property.value = rs.getString("value")
        result.add(h2property)

    }
    return result

}

fun getListH2databases() : List<H2database> {
    val result = mutableListOf<H2database>()
    val rs = getConnection().createStatement().executeQuery("select * from tbl_databases;")
    while (rs.next()) {
        val h2database = H2database()
        h2database.id = rs.getInt("id")
        h2database.name = rs.getString("name")
        h2database.driver = rs.getString("driver")
        h2database.url = rs.getString("url")
        h2database.user = rs.getString("user")
        h2database.password = rs.getString("password")
        result.add(h2database)
    }
    return result
}

fun getConnection() : Connection {

    val drv = "org.h2.Driver"
    val url = "jdbc:h2:./h2db"
    val user = "sa"
    val pass = ""

    Class.forName(drv).newInstance()
    return DriverManager.getConnection(url,user,pass)

}

fun isTablePresent(tableName : String) : Boolean {

    try {
        getConnection().createStatement().executeQuery("select * from $tableName")
        println("Найдена таблица $tableName")
        return true
    } catch (e: JdbcSQLSyntaxErrorException) {
        println("Не найден таблица $tableName")
        return false
    }

}

fun createTableDatabases() {

    var st = getConnection().createStatement()
    var sql = "create table tbl_databases (" +
            "id int auto_increment, " +
            "name varchar(255), " +
            "driver varchar(255), " +
            "url varchar(255), " +
            "user varchar(255), " +
            "password varchar(255)" +
            ");"
    st.execute(sql)

    sql = "create unique index TBL_DATABASES_ID_UINDEX on tbl_databases (id);"
    st.execute(sql)

    sql = "alter table tbl_databases add constraint TBL_DATABASES_PK primary key (id);"
    st.execute(sql)

    println("Создана таблица tbl_databases.")

}

fun createTableProperties() {

    var st = getConnection().createStatement()
    var sql = "create table tbl_properties (" +
            "key varchar(255) not null, " +
            "value varchar(255)" +
            ");"
    st.execute(sql)

    sql = "create unique index TBL_PROPERTIES_KEY_UINDEX on tbl_properties (key);"
    st.execute(sql)

    sql = "alter table tbl_properties add constraint TBL_PROPERTIES_PK primary key (key);"
    st.execute(sql)

    println("Создана таблица tbl_properties.")

}

fun initializeH2db() {

        // Проверяем наличие таблицы tbl_databases и создаем её если нужно
    if (!isTablePresent("tbl_databases")) createTableDatabases()

    // Проверяем наличие таблицы tbl_properties и создаем её если нужно
    if (!isTablePresent("tbl_properties")) createTableProperties()

    // Проверяем количество записей в таблице tbl_databases
    val listH2databases = getListH2databases()
    val countDatabases = listH2databases.size
    println("Количество баз данных: $countDatabases")

    var h2database = H2database()

    when (countDatabases) {
        0 -> {
            println("Нет ни одной базы. Создаем запись о embedded-базе, заполняем её fields и назначем текущей базой.")

            h2database.name = H2DB_EMBEDDED_NAME
            h2database.driver = H2DB_EMBEDDED_DRIVER
            h2database.url = H2DB_EMBEDDED_URL
            h2database.user = H2DB_EMBEDDED_USER
            h2database.password = H2DB_EMBEDDED_PASSWORD

            val idDb = saveH2database(h2database)
            setPropertyValue(H2DB_PROPERTYKEY_CURRENTDB_ID, idDb.toString())

        }
        1 -> {
            println("Найдена одна база. Назначаем её текущей.")
            h2database = listH2databases[0]
            setPropertyValue(H2DB_PROPERTYKEY_CURRENTDB_ID, h2database.id.toString())
            println("Единственная запись для базы данных «${h2database.name}» с идентификатором ${h2database.id} установлена как текущая.")

        }
        else -> {
            println("Найдено несколько баз. Проверяем значение текущей базы. Если такой базы нет - назначаем текущей первую запись.")
            val currDb = getCurrentDatabase()
            if (!listH2databases.contains(currDb)) {
                h2database = listH2databases[0]
                setPropertyValue(H2DB_PROPERTYKEY_CURRENTDB_ID, h2database.id.toString())
                println("Первая запись для базы данных «${h2database.name}» с идентификатором ${h2database.id} установлена как текущая.")
            } else {
                if (currDb != null) {
                    println("Запись для базы данных «${currDb.name}» с идентификатором ${currDb.id} является текущей.")
                }
            }


        }
    }

}

data class H2database (
    var id : Int? = null,
    var name : String? = null,
    var driver : String? = null,
    var url : String? = null,
    var user : String? = null,
    var password : String? = null
)

data class H2properties (
    var key : String? = null,
    var value : String? = null
)