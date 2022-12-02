ARG scalaVersion=2.13.10
ARG jdkVersion=11.0.16
ARG sbtVersion=1.8.0

FROM eclipse-temurin-${jdkVersion}_${sbtVersion}_${scalaVersion}

RUN mkdir /app
COPY build.sbt /app/build.sbt
COPY project /app/project
WORKDIR /app

#Build project
COPY . /app
RUN sbt clean