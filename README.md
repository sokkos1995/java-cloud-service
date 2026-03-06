# Облачное хранилище (Cloud Service)

REST-сервис для загрузки, скачивания и управления файлами пользователей. Все операции требуют авторизации по токену.

---

## Функционал

| Возможность | Описание |
|-------------|----------|
| **Авторизация** | Вход по логину и паролю, получение токена |
| **Выход** | Деактивация токена |
| **Список файлов** | Получение списка загруженных файлов с размерами |
| **Загрузка** | Загрузка файла на сервер (с возможностью перезаписи) |
| **Скачивание** | Получение файла по имени |
| **Удаление** | Удаление файла пользователя |
| **Переименование** | Изменение имени файла |

---

## Стек технологий

- Java 17
- Spring Boot 3.2
- Spring Security (токен-based авторизация)
- Spring Data JPA + PostgreSQL
- Maven
- Docker / Docker Compose

---

## Запуск

### Вариант 1: Docker Compose (рекомендуется)

```bash
docker-compose up -d
```

Запустит PostgreSQL на порту 5432 и приложение на порту 8080. Файлы сохраняются в папке `./storage`.

### Вариант 2: Локально (с отдельной БД)

1. Запустите PostgreSQL (или `docker-compose up db -d` только для БД).

2. Соберите и запустите приложение:
   ```bash
   mvn clean package
   java -jar target/cloudservice-1.0.0.jar
   ```

3. Либо через Maven:
   ```bash
   mvn spring-boot:run
   ```

По умолчанию приложение слушает порт **8080**.

---

## Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `SERVER_PORT` | Порт HTTP-сервера | 8080 |
| `DB_HOST` | Хост PostgreSQL | localhost |
| `DB_PORT` | Порт PostgreSQL | 5432 |
| `DB_NAME` | Имя базы данных | cloudservice |
| `DB_USER` | Пользователь БД | cloud |
| `DB_PASSWORD` | Пароль БД | cloud |
| `STORAGE_ROOT` | Папка для хранения файлов | ./storage |

---

## Тестовый пользователь

При первом запуске создаётся пользователь:

- **Логин:** `user`
- **Пароль:** `password`

---

## API

Базовый URL: `http://localhost:8080`

Все запросы (кроме `/login`) требуют заголовок `auth-token` с токеном. Фронтенд может отправлять токен с префиксом `Bearer ` — он обрабатывается автоматически.

### 1. Вход (login)

```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"login":"user","password":"password"}'
```

**Ответ 200:**
```json
{"auth-token":"550e8400-e29b-41d4-a716-446655440000"}
```

Сохраните значение `auth-token` для последующих запросов.

---

### 2. Выход (logout)

```bash
curl -X POST http://localhost:8080/logout \
  -H "auth-token: ВАШ_ТОКЕН"
```

---

### 3. Список файлов (list)

```bash
curl -X GET "http://localhost:8080/list?limit=10" \
  -H "auth-token: ВАШ_ТОКЕН"
```

**Ответ 200:**
```json
[
  {"filename":"document.pdf","size":102400},
  {"filename":"image.png","size":51200}
]
```

---

### 4. Загрузка файла (upload)

```bash
curl -X POST "http://localhost:8080/file?filename=test.txt" \
  -H "auth-token: ВАШ_ТОКЕН" \
  -F "file=@/путь/к/файлу.txt"
```

Если файл с таким именем уже существует — он будет перезаписан.

---

### 5. Скачивание файла (download)

```bash
curl -X GET "http://localhost:8080/file?filename=test.txt" \
  -H "auth-token: ВАШ_ТОКЕН" \
  -o downloaded.txt
```

---

### 6. Переименование файла (rename)

```bash
curl -X PUT "http://localhost:8080/file?filename=test.txt" \
  -H "auth-token: ВАШ_ТОКЕН" \
  -H "Content-Type: application/json" \
  -d '{"name":"newname.txt"}'
```

---

### 7. Удаление файла (delete)

```bash
curl -X DELETE "http://localhost:8080/file?filename=test.txt" \
  -H "auth-token: ВАШ_ТОКЕН"
```

---

## Работа с фронтендом

В репозитории есть Vue.js-фронтенд в папке `../netology-diplom-frontend`.

1. Откройте `netology-diplom-frontend/.env`
2. Укажите URL бэкенда:
   ```
   VUE_APP_BASE_URL=http://localhost:8080
   ```
3. Запустите фронт:
   ```bash
   cd netology-diplom-frontend
   npm install
   npm run serve
   ```
4. По умолчанию фронт на порту 8080. Если он занят — будет 8081. Тогда в `WebConfig` добавьте оба порта в `allowedOrigins`.

---

## Ошибки

| Код | Значение |
|-----|----------|
| 400 | Bad credentials, некорректные данные, файл не найден |
| 401 | Не передан или невалидный токен |
| 500 | Ошибка сервера (диск, БД и т.п.) |

Формат ошибки:
```json
{"message":"Описание ошибки","id":400}
```

---

## Структура проекта

```
src/main/java/ru/netology/cloudservice/
├── config/          # Конфигурация (CORS, properties, инициализация)
├── controller/      # REST-контроллеры (Auth, File)
├── service/         # Бизнес-логика (Auth, File)
├── repository/      # JPA-репозитории
├── entity/          # Сущности БД (User, File, AuthToken)
├── dto/             # DTO для запросов/ответов
├── security/        # Фильтр токенов, SecurityConfig
├── storage/         # Сохранение файлов на диск
└── exception/       # Глобальная обработка ошибок
```
