**Problema:**
@NotNull decrepted

**Solução:**
Atualizar dependência para projetos spring 3+
<!-- Para Jakarta EE 9+ (recomendado para novos projetos) -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Implementação (Hibernate Validator) -->
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>8.0.1.Final</version>
</dependency>

************************
DockerFile

#FROM eclipse-temurin:17-jdk-jammy
#WORKDIR /app
#COPY target/*.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]


FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copia os arquivos do projeto
COPY . .

# Constrói a aplicação
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/jornal-facil-*.jar"]

**********************
# ERRO AO CONECTAR O BANCO APOS DOCKER ON

Configurei PublicKeyRetrieval=true em advanced nas configurações 
do DATABASE mysql do intellij e apliquei.
