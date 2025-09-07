FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Copia primeiro o POM e baixa as dependências (cache mais eficiente)
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio de execução
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Cria o diretório para uploads
RUN mkdir -p /app/uploads

# Copia o JAR mantendo o mesmo nome
COPY --from=build /app/target/jornal-facil-*.jar ./jornal-facil.jar

# Configura variáveis de ambiente padrão
ENV FILE_UPLOAD_DIR=/app/uploads \
    SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/jornalBD?useSSL=false \
    SPRING_DATASOURCE_USERNAME=root \
    SPRING_DATASOURCE_PASSWORD=senha123

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "jornal-facil.jar"]



#FROM eclipse-temurin:17-jdk-jammy
#WORKDIR /app
#COPY target/*.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]


#FROM eclipse-temurin:17-jdk-jammy
#WORKDIR /app
#
## Copia os arquivos do projeto
#COPY . .
#
## Constrói a aplicação
#RUN ./mvnw clean package -DskipTests
#
#EXPOSE 8080
#
#ENTRYPOINT ["java", "-jar", "target/jornal-facil-*.jar"]

# Estágio de build


#funcionando
#FROM maven:3.8.6-eclipse-temurin-17 AS build
#WORKDIR /app
#COPY . .
#RUN mvn clean package -DskipTests
#
## Estágio de execução
#FROM eclipse-temurin:17-jdk-jammy
#WORKDIR /app
#COPY --from=build /app/target/jornal-facil-*.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]