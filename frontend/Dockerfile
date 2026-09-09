# Etapa 1: Build de la aplicación React con Node.js 24
FROM node:24-alpine AS build

WORKDIR /app

# Copiar paquetes y resolver dependencias
COPY package*.json ./
RUN npm ci

# Copiar código fuente y compilar
COPY . .
RUN npm run build

# Etapa 2: Servidor Nginx Alpine para producción
FROM nginx:alpine

# Copiar bundle estático a Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuración personalizada de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
