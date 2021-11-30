package com.svoemesto.ivfx

import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.AnnotationConfigApplicationContext

//@SpringBootApplication
class MyClass {

}
fun main() {
    val context = AnnotationConfigApplicationContext(SpringConfig::class.java)
    val propertyRepo = context.getBean("propertyRepo", PropertyRepo::class.java)

    println(propertyRepo.count())

}