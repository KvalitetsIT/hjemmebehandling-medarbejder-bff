![Build Status](https://github.com/KvalitetsIT/hjemmebehandling-medarbejder-bff/workflows/CICD/badge.svg) ![Test Coverage](.github/badges/jacoco.svg)
# hjemmebehandling-medarbejder-bff


## Prerequesites
### Openapitools
```shell
mkdir -p ~/bin/openapitools
curl https://raw.githubusercontent.com/OpenAPITools/openapi-generator/master/bin/utils/openapi-generator-cli.sh > ~/bin/openapitools/openapi-generator-cli
chmod u+x ~/bin/openapitools/openapi-generator-cli
export PATH=$PATH:~/bin/openapitools/
openapi-generator-cli version
```
## Running the integration test
1. run 'mvn clean install'
2. cd compose, run 'docker-compose up --build', wait until the hapi-server is initialized
3. run 'mvn verify -Pintegration-test'

## Endpoints

The service is listening for connections on port 8080.

Spring boot actuator is listening for connections on port 8081. This is used as prometheus scrape endpoint and health monitoring. 

Prometheus scrape endpoint: `http://localhost:8081/actuator/prometheus`  
Health URL that can be used for readiness probe: `http://localhost:8081/actuator/health`

## Configuration

| Environment variable | Description | Required |
|----------------------|-------------|---------- |
| cpr.url | URL - cprservice | Yes |
| fhir.server.url | URL - fhir server | Yes|
| user.context.handler | Handler for user context. Values MOCK or DIAS | Yes |
| LOG_LEVEL | Log Level for applikation  log. Defaults to INFO. | No |
| LOG_LEVEL_FRAMEWORK | Log level for framework. Defaults to INFO. | No |
| CORRELATION_ID | HTTP header to take correlation id from. Used to correlate log messages. Defaults to "x-request-id". | No|
