#!/bin/sh

# Build resource container and start it
docker build -t resources -f ./integrationtest/docker/Dockerfile-resources --no-cache ./integrationtest/docker
docker run -d --name hjemmebehandling-medarbejder-bff-resources resources

# Build hapi-server resource container and start it
docker build -t hapi-server-resources -f ./compose/Dockerfile-hapi-server-resources --no-cache ./compose
docker run -d --name hapi-server-resources hapi-server-resources

# Build inside docker container
docker run -v /var/run/docker.sock:/var/run/docker.sock  -v $HOME/.docker/config.json:/root/.docker/config.json -v $(pwd):/src -v $HOME/.m2:/root/.m2 --volumes-from hjemmebehandling-medarbejder-bff-resources --volumes-from hapi-server-resources maven:3-openjdk-16 /src/build/maven.sh
