# ---------- builder ----------
FROM oven/bun:1.3.4 AS builder
WORKDIR /app

# Копируем зависимости
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

# Копируем исходники
COPY . .

# Собираем standalone build
RUN bun run build

# ---------- runner ----------
FROM oven/bun:1.3.4 AS runner
WORKDIR /app
ENV NODE_ENV=production

# Копируем только standalone build + статические файлы
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static


EXPOSE 3000

# В standalone build создаётся server.js
CMD ["bun", "server.js"]
