# Compilar fora da VPS: ./mvnw clean verify
# Antes da publicação definitiva, fixe também o digest da imagem validada.
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --gid 10001 psa && useradd --uid 10001 --gid psa --no-create-home --home-dir /tmp psa
COPY --chown=psa:psa target/psa-gestor-api.jar app.jar
USER psa
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx384m -Duser.timezone=UTC -Dfile.encoding=UTF-8"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
