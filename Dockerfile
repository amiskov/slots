FROM openjdk:8-alpine

COPY target/uberjar/slots.jar /slots/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/slots/app.jar"]
