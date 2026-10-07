package com.svoemesto.ivfx.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Пробный тест: доказывает, что конвейер тестов в проекте вообще работает.
 *
 * Задача 232 (шаг 4 плана): тестов не было ни одного, каталога
 * `src/test/kotlin` не существовало. Прежде чем писать тесты на логику,
 * надо убедиться, что `mvn test` их находит и запускает — иначе «зелёная»
 * сборка означала бы «тестов нет».
 *
 * JUnit 5.7.1 и spring-boot-starter-test уже были объявлены в pom.xml,
 * родитель spring-boot-starter-parent подтягивает совместимый surefire,
 * testSourceDirectory указывает на этот каталог. Не хватало самого каталога.
 */
class HarnessSmokeTest {
    @Test
    fun `конвейер тестов запускается`() {
        assertEquals(4, 2 + 2)
    }
}
