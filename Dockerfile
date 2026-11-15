# Imagem usada na nuvem (Render). Primeiro compila com o Maven, depois roda só o .jar numa imagem menor.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# baixa as dependências antes de copiar o código, para o Docker reaproveitar essa etapa entre deploys
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY Codigos Codigos
COPY src src
# os testes já rodam no GitHub Actions; aqui só empacota
RUN mvn -B -q package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# O plano gratuito do Render tem 512 MB de memória e uma fração de CPU. Por isso:
# - heap de no máximo 55% da memória, deixando espaço para o resto da JVM (classes, threads, código compilado);
# - TieredStopAtLevel=1: o Java compila menos otimizações pesadas, então sobe e responde antes com pouca CPU;
# - pilha das threads e cache de código menores.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:ReservedCodeCacheSize=64m -XX:MaxMetaspaceSize=128m"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
