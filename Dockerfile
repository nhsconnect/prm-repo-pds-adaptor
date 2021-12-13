FROM openjdk:11.0.7-jdk-buster
RUN mkdir /home/spring
RUN groupadd --gid 102 spring
RUN useradd --home-dir /home/spring --uid 1000 --gid 102 --shell /bin/bash spring
RUN usermod -a -G spring spring
USER spring
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-Dlog4j2.formatMsgNoLookups=true","-XX:+ExitOnOutOfMemoryError","-XX:MaxRAMPercentage=80.0","-jar","/app.jar"]
