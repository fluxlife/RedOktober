ARG scalaVersion=2.13.10
ARG jdkVersion=11.0.16
ARG sbtVersion=1.8.0

FROM eclipse-temurin-${jdkVersion}_${sbtVersion}_${scalaVersion}

RUN mkdir /app
WORKDIR /app
COPY . /app
RUN sbt clean api/assembly
