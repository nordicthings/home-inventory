FROM eclipse-temurin:25-jre

WORKDIR /app

RUN addgroup --system home-inventory \
    && adduser --system --ingroup home-inventory home-inventory

COPY build/libs/home-inventory-*.jar /app/home-inventory.jar

ENV SPRING_PROFILES_ACTIVE=mariadb
ENV JAVA_OPTS=""

EXPOSE 8080

USER home-inventory:home-inventory

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/home-inventory.jar"]
