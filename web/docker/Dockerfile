FROM eclipse-temurin:21.0.13.1_2-jre-alpine-3.24

RUN apk update && apk upgrade && apk add --no-cache tzdata && apk --no-cache add bash

RUN addgroup -g 11000 appuser && adduser -D -u 11001 -G appuser -h /home/appuser appuser

WORKDIR /home/appuser
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

COPY config/logback.xml logback-spring.xml
COPY /maven/web-exec.jar web.jar

EXPOSE 8080
EXPOSE 8081

USER appuser
ENTRYPOINT ["/home/appuser/entrypoint.sh"]