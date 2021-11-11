package dk.kvalitetsit.hjemmebehandling.integrationtest;

import com.github.dockerjava.api.model.VolumesFrom;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Collections;

public abstract class AbstractIntegrationTest {
    private static final Logger serviceLogger = LoggerFactory.getLogger("hjemmebehandling-medarbejder-bff");
    private static final Logger hapiLogger = LoggerFactory.getLogger("hapi-server");

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

            GenericContainer service = new GenericContainer("kvalitetsit/hjemmebehandling-medarbejder-bff:dev")
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

    protected String enhanceBasePath(String basePath) {
        return basePath.replace("localhost", host).replace("8586", exposedServicePort);
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
