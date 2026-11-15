# syntax=docker/dockerfile:1

# Build stage: the shell and both micro-frontends are built separately, as they would be in separate pipelines.
FROM node:24-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npx ng build shell && npx ng build claimant && npx ng build staff

# Runtime stage: one nginx serves the shell at /, each micro-frontend under /mfe/<name>/, and proxies /api.
FROM nginx:1.29-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/shell/browser /usr/share/nginx/html
COPY --from=build /workspace/dist/claimant/browser /usr/share/nginx/html/mfe/claimant
COPY --from=build /workspace/dist/staff/browser /usr/share/nginx/html/mfe/staff
# Where the shell finds each micro-frontend in this deployment. Pointing one at another host is a change here only.
COPY docker/federation.manifest.json /usr/share/nginx/html/federation.manifest.json
EXPOSE 80
