# 235 — Контекст Spring зависает; приложение не стартует

**Задача была**: снять петлю, из-за которой контекст нельзя было поднять вне GUI.

**Оказалось**: петля была и причиной, и укрытием. За ней — приложение,
которое вообще не запускается.

---

## Что было

`Main.kt:30` создаёт контекст в статическом инициализаторе класса `Main`:

```kotlin
val context = AnnotationConfigApplicationContext(SpringConfig::class.java)
```

`Main` был помечен `@SpringBootApplication`, а он включает
`@SpringBootConfiguration`, который помечен `@Configuration`. `SpringConfig`
сканирует пакет `com.svoemesto.ivfx` целиком, значит `Main` попадал в список
конфигураций.

`ConfigurationClassEnhancer` CGLIB-усиливает каждую `@Configuration`, а для
этого определяет класс — то есть загружает его и запускает статическую
инициализацию. Инициализация `Main` создаёт новый контекст, который запускает
свой cglib, который снова грузит `Main`. Замкнутая петля.

Дамп потоков:

```
Class.forName("com.svoemesto.ivfx.Main")
  └─ Main.<clinit>(Main.kt:30)
       └─ AnnotationConfigApplicationContext(...)
            └─ ConfigurationClassEnhancer → cglib LoadingCache
                 └─ FutureTask.get()              ← ждёт
                      └─ [поток cglib] ReflectUtils.defineClass
                           └─ Class.forName("com.svoemesto.ivfx.Main")   ← заново
```

Воспроизведено **3 раза из 3**, в том числе когда `Main` был исключён из скана
и ни разу не упоминался в коде пробы. В GUI петля не проявлялась только
из-за порядка потоков: если `Main` инициализирует тот же поток, что потом
работает с cglib, повторный вход разрешён. Из другого потока — блокировка.

Это и было причиной, почему за четыре года не появилось ни одного теста:
контекст нельзя было поднять вне GUI, а вне GUI — единственный способ тестить.

---

## Правка

Одна строка: снята аннотация `@SpringBootApplication` с `Main` и её импорт.

По смыслу она была мёртвой — Spring Boot в проекте не используется (ADR-0001),
component scan работает от `@ComponentScan` в `SpringConfig`. По устройству
она и была причиной петли, как описано выше.

Ничего больше не менялось: ни `SpringConfig`, ни репозитории, ни модели.

---

## Что вскрылось за петлёй

Контекст после правки поднимается за **~1,2 с**, 46 бинов. Дальше — что в нём:

```
org.springframework.transaction.PlatformTransactionManager   бинов: 0  <-- НЕТ
org.springframework.transaction.support.TransactionTemplate  бинов: 0  <-- НЕТ
org.springframework.orm.jpa.JpaTransactionManager            бинов: 0  <-- НЕТ
javax.persistence.EntityManagerFactory                       бинов: 0  <-- НЕТ
org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean бинов: 0
javax.sql.DataSource                                         бинов: 1
```

И главное — **репозиториев среди бинов нет вообще**:

```
fileRepo  -> NoSuchBeanDefinitionException: No bean named 'fileRepo' available
shotRepo  -> NoSuchBeanDefinitionException
sceneRepo -> NoSuchBeanDefinitionException
```

Из 46 бинов — только конкретные классы: контроллеры и сущности. Ни одного
интерфейса.

Поэтому путь самого приложения падает:

```
Main не инициализировался за 1187 мс
  ExceptionInInitializerError
  NoSuchBeanDefinitionException: No bean named 'propertyRepo' available
```

Проверено на настоящем classpath из `run-old-app.sh` (`target/runtime-classpath.txt`,
168 банков, включая test-scope) — не на самодельном. Владелец подтвердил:
приложение не запускается, ошибка есть.

### Почему репозитории не регистрируются

1. Репозитории — `interface FileRepo : CrudRepository<…>`, помеченные
   `@Component`. Но сканер компонентов **интерфейсы пропускает**: candidate
   требует concrete-класс либо abstract с аннотированными методами.
   `@Component` на интерфейсе не даёт ничего.
2. Зарегистрировать их должен `@EnableJpaRepositories` или
   `@EnableJdbcRepositories`. В проекте их нет — grep по всему репозиторию
   (не только `src`) не находит ни того, ни другого.
3. Spring Boot подставил бы это автоконфигурацией, но Boot не запускается:
   контекст поднимается вручную через `AnnotationConfigApplicationContext`.
4. `spring.factories` и `META-INF` в ресурсах отсутствуют, так что
   автоконфигурация не подхватывается и здесь.

Дополнительно: в `pom.xml` явно объявлены **и** `spring-boot-starter-data-jdbc`,
**и** `spring-boot-starter-data-jpa`, **и** `spring-boot-starter-jdbc`. Отсюда
то, что видно при старте:

```
Multiple Spring Data modules found, entering strict repository configuration mode!
Bootstrapping Spring Data JDBC repositories in DEFAULT mode.
Spring Data JDBC - Could not safely identify store assignment for repository
candidate interface ...FileRepo
```

Spring Data не может выбрать хранилище и оставляет репозитории незарегистрированными.

### Что это значит для записанных в правилах утверждений

Проверено фактами, а не выведено:

| Утверждалось в документации | Что показал запуск контекста |
|---|---|
| «`@Transactional` не работает на FX-контроллерах» | Не работает **нигде**: менеджера транзакций в контексте нет, Spring не создаёт прокси-транзакции |
| «каскады JPA покрывают не всё» | Каскадов нет: `EntityManagerFactory` отсутствует, ORM не участвует. Все «каскады» — ручные вызовы `deleteAll` |
| `spring.jpa.hibernate.ddl-auto=update` управляет схемой | Инертно: ни Boot, ни Hibernate в контексте нет |
| `@Entity`, `@Id`, `@ManyToOne`, `CascadeType` | Мёртвые аннотации |

Данные в Postgres (4837 планов, 807 718 кадров, 62 905 лица) — от прошлого
рабочего состояния. Кода, который их читает, не существует в рабочем виде.

---

## Что проверено и как

| Проверка | Результат |
|---|---|
| Петля до правки | 3 прогона из 3 зависают намертво (240 с, затем kill) |
| Петля после правки | контекст поднимается за ~1,2 с, 46 бинов |
| Путь приложения (`Main`) | падает: `NoSuchBeanDefinitionException: propertyRepo` |
| Classpath | настоящий, из `run-old-app.sh` — 168 банков с test-scope |
| Тесты | `mvn -B test` → `Tests run: 28, Failures: 0` |
| Сборка | `mvn -B -q clean compile` → exit 0 |
| Стиль | `bash tools/check-ktlint.sh` → новых нарушений нет, долг сократился на 2 (удалён импорт) |
| Связь FXML | `bash tools/check-fx-wiring.sh` → форма и контроллер связаны |
| Документация | `python3 docs/scripts/lint-docs.py` → exit 0 |

Временные пробы удалены, копий баз не создавалось, рабочая база Postgres не
изменялась. `h2db.mv.db` открывался пробой — содержимое не изменилось
(4 базы, 1 свойство — как в копии от 13:06).

---

## Что осталось

Эта задача закрывает только петлю. Приложение по-прежнему не запускается —
по другой причине, и она требует решения владельца, потому что вариантов
несколько и они меняют устройство проекта:

1. **Как регистрировать репозитории.** `@EnableJdbcRepositories` (не требует
   переписывания моделей) или `@EnableJpaRepositories` (требует
   `EntityManagerFactory`, то есть фактически включить JPA), либо переписать
   интерфейсы под конкретное хранилище.
2. **Что делать с двумя конкурирующими стартерами** в `pom.xml` — `data-jdbc`
   и `data-jpa` объявлены оба, и именно это переводит Spring Data в строгий
   режим, где он не выбирает сторону.
3. **Нужны ли транзакции.** Пока менеджера нет, любая последовательность
   записей применяется по частям — это ровно то, что привело к потере данных
   в задаче #234.
4. **Утечка подключения** — `context.close()` не вызывается нигде.
5. **Приведение документации в соответствие.** Утверждения в `AGENTS.md` и
   доменах про `@Transactional`, каскады и `ddl-auto` надо переписать по
   фактам — но только после того, как выбрано хранилище.

## Чего не сделано и почему

- **Ремонта слоя данных.** Задача была узкой — снять петлю и выяснить, что
  поднимается. Оба сделаны. Ремонт выбора хранилища меняет устройство
  проекта и это решение владельца.
- **Правки утверждений в `AGENTS.md`.** Факты уже установлены и записаны в
  отчёт, но правила правились бы под хранилище, которого пока нет.