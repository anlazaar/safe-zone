FROM golang:1.24-alpine

RUN apk add --no-cache git curl

WORKDIR /src

RUN git clone https://github.com/minio/minio.git .

RUN go build -o /minio .

ENTRYPOINT ["/minio"]