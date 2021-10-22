package dk.kvalitetsit.hjemmebehandling.integrationtest;

import com.github.dockerjava.api.model.VolumesFrom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.model.CarePlanDto;
import org.openapitools.client.model.CreateCarePlanRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CarePlanIntegrationTest {
    private static final Logger serviceLogger = LoggerFactory.getLogger("rim-medarbejder-bff");
    private static final Logger hapiLogger = LoggerFactory.getLogger("hapi-server");

    private CarePlanApi subject;

    private static String host = "localhost";
    private static String exposedServicePort = "8080";

    @BeforeAll
    public static void setupEnvironment() throws Exception {
        if(Boolean.getBoolean("startDocker")) {
            Network network = Network.newNetwork();

            var resourcesContainerName = "hapi-server-resources";
            var resourcesRunning = containerRunning(resourcesContainerName);
            System.out.println("Resource container is running: " + resourcesRunning);

            VolumesFrom volumesFrom = new VolumesFrom(resourcesContainerName);

            GenericContainer service = new GenericContainer("kvalitetsit/rim-medarbejder-bff:dev")
                    .withNetwork(network)
                    .withNetworkAliases("medarbejder-bff")
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/api/v3/api-docs"));

            GenericContainer hapiServer = new GenericContainer<>("hapiproject/hapi:latest")
                    .withNetwork(network)
                    .withNetworkAliases("hapi-server")
                    .withEnv("SPRING_CONFIG_LOCATION", "file:///hapi-server/application.yaml")
                    .withCreateContainerCmdModifier(modifier -> modifier.withVolumesFrom(volumesFrom))
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/fhir").forStatusCode(400))                    ;

            GenericContainer hapiServerInitializer = new GenericContainer<>("alpine:3.11.5")
                    .withNetwork(network)
                    .withCreateContainerCmdModifier(modifier -> modifier.withVolumesFrom(volumesFrom))
                    .withCommand("/hapi-server-initializer/init.sh");

            service.start();
            attachLogger(serviceLogger, service);

            hapiServer.start();
            attachLogger(hapiLogger, hapiServer);

            hapiServerInitializer.start();
            attachLogger(hapiLogger, hapiServerInitializer);

            host = service.getContainerIpAddress();
            exposedServicePort = Integer.toString(service.getMappedPort(8080));
            System.out.println("Sleeping for a little while, until hapi-server is definitely initialized.");
            Thread.sleep(5000);
        }
    }

    @BeforeEach
    public void setup() {
        subject = new CarePlanApi();

        String basePath = subject.getApiClient().getBasePath().replace("localhost", host).replace("8586", exposedServicePort);
        subject.getApiClient().setBasePath(basePath);
    }

    @Test
    public void createCarePlan_success() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest()
                .cpr("0101010101")
                .planDefinitionId("2");

        System.out.println("Creating careplan at: " + subject.getApiClient().getBasePath());

        // Act
        ApiResponse<Void> response = subject.createCarePlanWithHttpInfo(request);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("Location"));
    }

    @Test
    public void getCarePlan_success() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        // Act
        ApiResponse<CarePlanDto> response = subject.getCarePlanWithHttpInfo(carePlanId);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    private static boolean containerRunning(String containerName) {
        return DockerClientFactory
                .instance()
                .client()
                .listContainersCmd()
                .withNameFilter(Collections.singleton(containerName))
                .exec()
                .size() != 0;
    }

    private static void attachLogger(Logger logger, GenericContainer container) {
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        container.followOutput(logConsumer);
    }
}
