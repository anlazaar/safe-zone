FROM node:20-alpine

RUN apk add --no-cache \
    chromium \
    nss \
    freetype \
    harfbuzz

ENV CHROME_BIN=/usr/bin/chromium