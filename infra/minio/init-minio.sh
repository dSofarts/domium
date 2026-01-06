#!/bin/sh
set -e

echo "Настройка алиаса MinIO..."
mc alias set local http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"

echo "Ожидание готовности MinIO..."
until mc ready local 2>/dev/null; do
  echo "MinIO еще не готов, ожидание..."
  sleep 2
done
echo "MinIO готов!"

echo "Создание бакетов..."

if [ -n "${PROJECTS_BUCKET}" ]; then
  echo "Создание бакета: ${PROJECTS_BUCKET}"
  mc mb --ignore-existing local/"${PROJECTS_BUCKET}"
  mc anonymous set download local/"${PROJECTS_BUCKET}"
fi

if [ -n "${DOCUMENTS_BUCKET}" ]; then
  echo "Создание бакета: ${DOCUMENTS_BUCKET}"
  mc mb --ignore-existing local/"${DOCUMENTS_BUCKET}"
fi

if [ -n "${TEMPLATES_BUCKET}" ]; then
  echo "Создание бакета: ${TEMPLATES_BUCKET}"
  mc mb --ignore-existing local/"${TEMPLATES_BUCKET}"
fi

if [ -n "${LOKI_BUCKET}" ]; then
  echo "Создание бакета: ${LOKI_BUCKET}"
  mc mb --ignore-existing local/"${LOKI_BUCKET}"
  mc anonymous set download local/"${LOKI_BUCKET}"
fi

# Создание политик и пользователей для project-service
if [ -n "${PROJECTS_BUCKET}" ] && [ -f "/policies/project-service.json" ]; then
  echo "Настройка политик и пользователей для project-service..."
  
  # Создание политики (если не существует)
  if mc admin policy info local project-service-policy > /dev/null 2>&1; then
    echo "Политика project-service-policy уже существует, обновление..."
    mc admin policy create local project-service-policy /policies/project-service.json
  else
    echo "Создание политики project-service-policy..."
    mc admin policy create local project-service-policy /policies/project-service.json
  fi
  
  # Создание пользователя (если не существует)
  if [ -n "${PROJECT_SERVICE_USER}" ] && [ -n "${PROJECT_SERVICE_SECRET}" ]; then
    if mc admin user info local "${PROJECT_SERVICE_USER}" > /dev/null 2>&1; then
      echo "Пользователь ${PROJECT_SERVICE_USER} уже существует"
    else
      echo "Создание пользователя ${PROJECT_SERVICE_USER}..."
      mc admin user add local "${PROJECT_SERVICE_USER}" "${PROJECT_SERVICE_SECRET}"
    fi
    
    # Привязка политики к пользователю
    echo "Привязка политики к пользователю ${PROJECT_SERVICE_USER}..."
    mc admin policy attach local project-service-policy --user "${PROJECT_SERVICE_USER}"
  fi
fi

# Загрузка seed данных
if [ -d "/seed/images/projects" ] && [ -n "${PROJECTS_BUCKET}" ]; then
  echo "Загрузка seed данных в бакет ${PROJECTS_BUCKET}..."
  mc mirror --overwrite /seed/images/projects local/"${PROJECTS_BUCKET}" || echo "Seed данные не загружены или уже существуют"
fi

echo "Инициализация MinIO завершена успешно!"

