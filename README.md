Проект построен на базе арзитектуры порты и адаптеры.

# Запуск проекта

1. Инициализация конфигов:\
   `make init`
2. Для работы бота надо вставить токен (файл bot/SRC/application/resource/application.conf)
3. Запуск бд:

```
docker run -d \
 --name postgres \
 -e POSTGRES_USER=myuser \
 -e POSTGRES_PASSWORD=mypassword \
 -e POSTGRES_DB=mydatabase \
 -p 5432:5432 \
 postgres:latest
```

5. Запуск приложение:\
   `make run`