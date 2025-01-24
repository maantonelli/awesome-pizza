FROM openjdk:21-jdk

COPY target/awesome-pizza*.jar /app/awesome-pizza.jar

WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["java","-jar","awesome-pizza.jar"]