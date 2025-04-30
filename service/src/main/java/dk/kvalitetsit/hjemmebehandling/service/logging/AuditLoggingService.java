package dk.kvalitetsit.hjemmebehandling.service.logging;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.model.AuditModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import dk.kvalitetsit.hjemmebehandling.service.audit.DiasAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AuditLoggingService {
    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingService.class);

    @Autowired
    private UserContextProvider userContextProvider;

    @Autowired
    private DiasAuditLogger diasAuditLogger;

    private void auditLog(String message, Map<String, String> citizenCprToFullNameMap) {
        // TODO: Handle 'Optional.get()' without 'isPresent()' check below
        var employee = new AuditModel.Employee(userContextProvider.getUserContext().getUserId().get(), userContextProvider.getUserContext().getFullName().get());

        for (String citizenCpr : citizenCprToFullNameMap.keySet()) {
            String citizenFullName = citizenCprToFullNameMap.get(citizenCpr);
            var citizen = new AuditModel.Citizen(citizenCpr, citizenFullName);
            AuditModel event = new AuditModel(employee, citizen, message);
            diasAuditLogger.postAuditLog(event);
        }
    }

    public void log(String message, PatientModel patient) {
        log(message, List.of(patient));
    }

    public void log(String message, List<PatientModel> patients) {
        Map<String, String> result = patients.stream()
                .collect(Collectors.toMap(x -> x.cpr().value(), u -> u.name().given().getFirst() + " " + u.name().family(), (existing, replacement) -> existing));

        auditLog(message, result);
    }

    public void log(String message, PersonModel person) {
        auditLog(message, Stream.of(person)
                .collect(Collectors.toMap(
                        p -> p.identifier().id(),
                        p -> String.join(" ", p.name().given()) + " " + p.name().family())));
    }
}
