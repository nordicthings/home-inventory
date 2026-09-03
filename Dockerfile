FROM eclipse-temurin:25-jre

WORKDIR /app

RUN addgroup --system home-inventory \
    && adduser --system --ingroup home-inventory home-inventory

COPY build/libs/home-inventory-*.jar /app/home-inventory.jar

ARG APP_VERSION=dev
ENV SPRING_PROFILES_ACTIVE=mariadb
ENV JAVA_OPTS=""
ENV HOME_INVENTORY_VERSION=${APP_VERSION}

EXPOSE 8080

USER home-inventory:home-inventory

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/home-inventory.jar"]
