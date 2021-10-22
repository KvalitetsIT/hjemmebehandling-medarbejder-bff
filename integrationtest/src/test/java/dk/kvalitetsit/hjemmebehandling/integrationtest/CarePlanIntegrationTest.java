package dk.kvalitetsit.hjemmebehandling.integrationtest;

import com.github.dockerjava.api.model.VolumesFrom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.model.CarePlanDto;
import org.openapitools.client.model.CreateCarePlanRequest;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CarePlanIntegrationTest {
    private CarePlanApi subject;

    private static String exposedServicePort = "8080";

    @BeforeAll
    public static void setupEnvironment() throws Exception {
        //System.setProperty("startDocker", "true");
        if(Boolean.getBoolean("startDocker")) {
            Network network = Network.newNetwork();

            var resourcesContainerName = "hapi-server-resources";
            var resourcesRunning = containerRunning(resourcesContainerName);
            System.out.println("Resource container is running: " + resourcesRunning);

            VolumesFrom volumesFrom = new VolumesFrom(resourcesContainerName);

            GenericContainer service = new GenericContainer("kvalitetsit/rim-medarbejder-bff:dev")
                    .withNetwork(network)
                    .withNetworkAliases("medarbejder-bff")
                    .withExposedPorts(8080);

            System.out.println("../compose/hapi-server/application.yaml exists: " + new File("../compose/hapi-server/application.yaml").exists());
            GenericContainer hapiServer = new GenericContainer<>("hapiproject/hapi:latest")
                    .withNetwork(network)
                    .withNetworkAliases("hapi-server")
                    .withEnv("SPRING_CONFIG_LOCATION", "file:///hapi-server/application.yaml")

                    .withCreateContainerCmdModifier(modifier -> modifier.withVolumesFrom(volumesFrom))

                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/fhir").forStatusCode(400))                    ;
            //hapiServer.withFileSystemBind("../compose/hapi-server/application.yaml", "/data/hapi/application.yaml");

            GenericContainer hapiServerInitializer = new GenericContainer<>("alpine:3.11.5")
                    .withNetwork(network)

                    .withCreateContainerCmdModifier(modifier -> modifier.withVolumesFrom(volumesFrom))

                    .withCommand("/hapi-server-initializer/init.sh");
            //hapiServerInitializer.withFileSystemBind("../compose/hapi-server-initializer", "/hapi-server-initializer");

            service.start();
            hapiServer.start();
            hapiServerInitializer.start();

            exposedServicePort = Integer.toString(service.getMappedPort(8080));
            System.out.println("Sleeping for a little while, until hapi-server is definitely initialized.");
            Thread.sleep(5000);
        }
    }

    @BeforeEach
    public void setup() {
        subject = new CarePlanApi();

        String basePath = subject.getApiClient().getBasePath().replace("8586", exposedServicePort);
        subject.getApiClient().setBasePath(basePath);
    }

    @Test
    public void createCarePlan_success() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest()
                .cpr("0101010101")
                .planDefinitionId("2");

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
}
