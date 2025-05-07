![Build Status](https://github.com/KvalitetsIT/hjemmebehandling-medarbejder-bff/workflows/CICD/badge.svg) ![Test Coverage](.github/badges/jacoco.svg)
# Medarbejder-bff

The medarbejder-bff is a Backend For Frontend (BFF) for the employee application. In short the BFF layer consists of multiple backends developed to address the needs of respective frontend frameworks, like desktop, browser, and native-mobile apps. This service is backend for the employees and administrators using a browser.  

The service is a Java Spring Boot application with the following properties

1. Exposes REST interfaces 
2. Interfaces are documented using OpenApi (see url below). 
3. Is a FHIR client to FHIR server backend
4. Exposes metrics url for prometheus to scrape

## Prerequesites
To build the project you need:

 * OpenJDK 11+
 * Maven 3.1.1+
 * Docker 20+
 * Docker-compose 1.29+

## Building the service

The simplest way to build the service is to use maven:

```
mvn clean install
```

This will build the service as a JAR used by spring boot.

## Running the service
Use the local docker-compose setup

```
cd compose/
docker-compose up
```
This will start the hapi server and the bff.

## Logging

The service uses Logback for logging. At runtime Logback log levels can be configured via environment variable.


## Monitoring

On http the service exposes a health endpoint on the path /info. This will return 200 ok if the service is basically in a healthy state, meaning that the web service is up and running.

On http the service exposes Prometheus metrics on the path /prometheus. 

## Testing
Automated unit- and integrationtest are used.

### Running the unit test
The CICD pipeline runs the unittest for each commit the complete test report can be found under Actions. The total coverage is available on the top of this page.

Unit tests can run local by running

```
mvn clean install
```

### Running the integrationtest
The CICD pipeline runs the Integrationtest for each commit.

Integration tests can run locally by running

```
mvn clean install
cd compose 
docker-compose up --build
``mvn verify -Pintegration-test``
```

## Endpoints

The service is listening for connections on port 8080.

Spring boot actuator is listening for connections on port 9090. This is used as prometheus scrape endpoint and health monitoring. 

Prometheus scrape endpoint: `http://localhost:9090/actuator/prometheus`  
Health URL that can be used for readiness probe: `http://localhost:9090/actuator/health`
The application API documentation(Json): `http://localhost:8080/api/v3/api-docs`

## Configuration

| Environment variable         | Description                                                                                          | Required                       |
|------------------------------|------------------------------------------------------------------------------------------------------|--------------------------------|
| cpr.url                      | URL - cprservice                                                                                     | Yes                            |
| fhir.server.url              | URL - fhir server                                                                                    | Yes                            |
| user.context.handler         | Handler for user context. Values MOCK or DIAS                                                        | Yes                            |
| user.context.organization.id | Sets the organization id of the mocked context                                                       | if user.context.handler = MOCK |
| user.context.entitlements    | Injects the entitlements into the mocked context (Has to match atleast one of the allowed.roles)     | if user.context.handler = MOCK |
| audit.url                    | URL - audit server                                                                                   | No                             |
| LOG_LEVEL                    | Log Level for applikation  log. Defaults to INFO.                                                    | No                             |
| LOG_LEVEL_FRAMEWORK          | Log level for framework. Defaults to INFO.                                                           | No                             |
| CORRELATION_ID               | HTTP header to take correlation id from. Used to correlate log messages. Defaults to "x-request-id". | No                             |
| allowed.roles                | Sets the allowed roles                                                                               | Yes                            |

## Generate for frontend (bff.json)
Start bff on port 8080. After that run the following command;

```
curl http://localhost:8080/api/v3/api-docs > resources/bff.json
```

The Web application uses the OpenApi specification to generate DTOs (Data Transfer Objects) on the client side.

## Openapitools
Openapi tools can be used to generate docs

Install openapitools
swagger
```shell
mkdir -p ~/bin/openapitools
curl https://raw.githubusercontent.com/OpenAPITools/openapi-generator/master/bin/utils/openapi-generator-cli.sh > ~/bin/openapitools/openapi-generator-cli
chmod u+x ~/bin/openapitools/openapi-generator-cli
export PATH=$PATH:~/bin/openapitools/
openapi-generator-cli version
```

Generate html

```
openapi-generator-cli generate -i bff.json -g html
```
