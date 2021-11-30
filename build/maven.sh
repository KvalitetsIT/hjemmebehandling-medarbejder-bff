#!/bin/sh

apt-get update
apt-get install -y docker.io

SRC_FOLDER=src

if [ -d $SRC_FOLDER ]; then
  cd $SRC_FOLDER

  # Build the bff service
  mvn clean install

  # Start the hapi server
  docker rm hapi-server
  docker run -d --volumes-from maven-builder --network rim --name hapi-server hapiproject/hapi:latest

  # Initialize the server with data
  docker run --volumes-from maven-builder --network rim -e data_dir='/src/compose/hapi-server-initializer' alpine:3.11.5 /src/compose/hapi-server-initializer/init.sh

  # Start the bff service
  docker rm medarbejder-bff
  docker run -d --network rim --name medarbejder-bff -p 8080:8080 --volumes-from maven-builder kvalitetsit/hjemmebehandling-medarbejder-bff:latest

  # Wait for it to be ready
  echo 'waiting for bff to be ready ...'
  curl -o /dev/null --retry 5 --retry-max-time 40 --retry-connrefused http://medarbejder-bff:8080

  # Run the integration test
  cd integrationtest
  mvn verify -Pintegration-test -Dmedarbejder-bff-host=medarbejder-bff

  docker stop medarbejder-bff
  docker stop hapi-server
else
  echo "$SRC_FOLDER folder not found."
  exit 1
fi

