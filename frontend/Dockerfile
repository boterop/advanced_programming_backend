ARG NODE_VERSION=26.8.1

FROM node:${NODE_VERSION}-alpine as base
WORKDIR /app
RUN npm i -g pnpm@latest
COPY package.json pnpm-lock.yaml ./
RUN pnpm i

FROM base AS development
COPY . .
EXPOSE 3000
CMD ["pnpm", "dev"]

FROM base as build
COPY . .
RUN pnpm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
