package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.audit.AuditModel;
import dk.kvalitetsit.hjemmebehandling.service.audit.DiasAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuditLoggingService {
  private static final Logger logger = LoggerFactory.getLogger(AuditLoggingService.class);

  @Autowired
  private UserContextProvider userContextProvider;

  @Autowired
  private DiasAuditLogger diasAuditLogger;

  private void auditLog(String message, Map<String, String> citizenCprToFullNameMap) {
    String employeeRegionsId = userContextProvider.getUserContext().getUserId();
    String employeeFullName = userContextProvider.getUserContext().getFullName();

    for (String citizenCpr: citizenCprToFullNameMap.keySet()) {
      String citizenFullName = citizenCprToFullNameMap.get(citizenCpr);
      AuditModel event = new AuditModel(employeeRegionsId, employeeFullName, citizenCpr, citizenFullName, message);

      diasAuditLogger.postAuditLog(event);
    }
  }

  public void log(String message, PatientModel patient) {
    log(message, List.of(patient));
  }
  public void log(String message, List<PatientModel> patients) {
    Map<String, String> result = patients.stream()
        .collect(Collectors.toMap(
            PatientModel::getCpr,
            u -> u.getGivenName() + " " + u.getFamilyName(),
            (existing, replacement) -> existing))
        ;

    auditLog(message, result);
  }

  public void log(String message, PersonModel person) {
    auditLog(message, List.of(person).stream()
        .collect(Collectors.toMap(
            p -> p.getIdentifier().getId(),
            p -> String.join(" ", p.getName().getGiven()) + " " + p.getName().getFamily())))
        ;
  }
}
