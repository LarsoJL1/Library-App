FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

COPY src ./src
COPY schema.sql seed.sql ./
RUN mkdir -p out lib \
    && curl -fsSL "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar" \
       -o lib/sqlite-jdbc.jar \
     && curl -fsSL "https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar" \
         -o lib/slf4j-api.jar \
    && javac -encoding UTF-8 -cp lib/sqlite-jdbc.jar -d out \
       src/model/*.java src/dao/*.java src/app/*.java

FROM eclipse-temurin:17-jre-jammy

ENV DISPLAY=:0
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       fluxbox novnc python3-websockify x11vnc xvfb \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /data

COPY --from=build /app/out ./out
COPY --from=build /app/lib/sqlite-jdbc.jar ./lib/sqlite-jdbc.jar
COPY --from=build /app/lib/slf4j-api.jar ./lib/slf4j-api.jar
COPY schema.sql seed.sql ./
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 6080
VOLUME ["/data"]
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
