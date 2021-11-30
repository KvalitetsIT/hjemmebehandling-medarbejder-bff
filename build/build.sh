#!/bin/sh

docker network rm rim
docker network create rim

docker rm maven-builder
docker run -v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.docker/config.json:/root/.docker/config.json -v $(pwd):/src -v $HOME/.m2:/root/.m2 --name maven-builder --network rim  maven:3-jdk-11 /src/build/maven.sh

