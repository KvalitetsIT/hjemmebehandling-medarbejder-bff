package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.HelloRequest;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.service.HelloService;
import dk.kvalitetsit.hjemmebehandling.api.HelloResponse;
import dk.kvalitetsit.hjemmebehandling.service.model.HelloServiceInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);
    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @PostMapping(value = "/v1/hello")
    @CrossOrigin(origins = "http://localhost:3000")
    public @ResponseBody HelloResponse hello(@RequestBody(required = false) HelloRequest body) {
        logger.debug("Enter POST hello.");

        if(body == null) {
            throw new BadRequestException();
        }

        var serviceInput = new HelloServiceInput();
        serviceInput.setName(body.getName());

        var serviceResponse = helloService.helloServiceBusinessLogic(serviceInput);

        var helloResponse = new HelloResponse();
        helloResponse.setName(serviceResponse.getName());
        helloResponse.setNow(serviceResponse.getNow());

        return helloResponse;
    }
}