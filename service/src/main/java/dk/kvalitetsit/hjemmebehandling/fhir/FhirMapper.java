package dk.kvalitetsit.hjemmebehandling.fhir;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Text;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Question;
import org.hl7.fhir.r4.model.*;

import org.hl7.fhir.r4.model.Enumeration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;

@Component
public class FhirMapper {
    @Autowired
    private DateProvider dateProvider;

    @Autowired
    private FhirClient fhirClient;

    public CarePlan mapCarePlanModel(CarePlanModel carePlanModel) {
        CarePlan carePlan = new CarePlan();

        mapBaseAttributesToFhir(carePlan, carePlanModel);

        carePlan.setTitle(carePlanModel.getTitle());
        carePlan.setStatus(Enum.valueOf(CarePlan.CarePlanStatus.class, carePlanModel.getStatus().toString()));
        carePlan.setCreated(Date.from(carePlanModel.getCreated()));
        if(carePlanModel.getStartDate() != null) {
            carePlan.setPeriod(new Period());
            carePlan.getPeriod().setStart(Date.from(carePlanModel.getStartDate()));
        }
        carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(carePlanModel.getSatisfiedUntil()));

        // Set the subject
        if(carePlanModel.getPatient().getId() != null) {
            carePlan.setSubject(new Reference(carePlanModel.getPatient().getId().toString()));
        }

        // Map questionnaires to activities
        if(carePlanModel.getQuestionnaires() != null) {
            carePlan.setActivity(carePlanModel.getQuestionnaires()
                    .stream()
                    .map(this::buildCarePlanActivity)
                    .collect(Collectors.toList()));
        }

        if(carePlanModel.getPlanDefinitions() != null) {
            // Add references to planDefinitions
            carePlan.setInstantiatesCanonical(carePlanModel.getPlanDefinitions()
                    .stream()
                    .map(pd -> new CanonicalType(pd.getId().toString()))
                    .collect(Collectors.toList()));
        }

        return carePlan;
    }

    public CarePlanModel mapCarePlan(CarePlan carePlan, FhirLookupResult lookupResult) {
        CarePlanModel carePlanModel = new CarePlanModel();

        mapBaseAttributesToModel(carePlanModel, carePlan);

        carePlanModel.setTitle(carePlan.getTitle());
        carePlanModel.setStatus(Enum.valueOf(CarePlanStatus.class, carePlan.getStatus().toString()));
        carePlanModel.setCreated(carePlan.getCreated().toInstant());
        carePlanModel.setStartDate(carePlan.getPeriod().getStart().toInstant());
        if(carePlan.getPeriod().getEnd() != null) {
            carePlanModel.setEndDate(carePlan.getPeriod().getEnd().toInstant());
        }

        String patientId = carePlan.getSubject().getReference();
        Patient patient = lookupResult.getPatient(patientId).orElseThrow(() -> new IllegalStateException(String.format("Could not look up Patient for CarePlan %s!", carePlanModel.getId())));
        carePlanModel.setPatient(mapPatient(patient));

        carePlanModel.setPlanDefinitions(new ArrayList<>());
        for(var ic : carePlan.getInstantiatesCanonical()) {
            var planDefinition = lookupResult
                .getPlanDefinition(ic.getValue())
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up PlanDefinition for CarePlan %s!", carePlanModel.getId())));
            carePlanModel.getPlanDefinitions().add(mapPlanDefinition(planDefinition, lookupResult));
        }

        carePlanModel.setQuestionnaires(new ArrayList<>());
        for(var activity : carePlan.getActivity()) {
            String questionnaireId = activity.getDetail().getInstantiatesCanonical().get(0).getValue();
            var questionnaire = lookupResult
                    .getQuestionnaire(questionnaireId)
                    .orElseThrow(() -> new IllegalStateException(String.format("Could not look up Questionnaire for CarePlan %s!", carePlanModel.getId())));

            var questionnaireModel = mapQuestionnaire(questionnaire);
            var frequencyModel = mapTiming(activity.getDetail().getScheduledTiming());

            var wrapper = new QuestionnaireWrapperModel();
            wrapper.setQuestionnaire(questionnaireModel);
            wrapper.setFrequency(frequencyModel);
            wrapper.setSatisfiedUntil(ExtensionMapper.extractActivitySatisfiedUntil(activity.getDetail().getExtension()));

            // get thresholds from questionnaire
            List<ThresholdModel> questionnaireThresholds = ExtensionMapper.extractThresholds(
                questionnaire.getItem().stream()
                    .flatMap(q -> q.getExtensionsByUrl(Systems.THRESHOLD).stream())
                    .collect(Collectors.toList())
            );
            wrapper.getThresholds().addAll(questionnaireThresholds);

            // find thresholds from plandefinition
            Optional<List<ThresholdModel>> thresholds = carePlanModel.getPlanDefinitions().stream()
                .flatMap(p -> p.getQuestionnaires().stream())
                .filter(q -> q.getQuestionnaire().getId().equals(questionnaireModel.getId()))
                .findFirst()
                .map(QuestionnaireWrapperModel::getThresholds);
            thresholds.ifPresent(thresholdModels -> wrapper.getThresholds().addAll(thresholdModels));

            carePlanModel.getQuestionnaires().add(wrapper);
        }


        carePlanModel.setSatisfiedUntil(ExtensionMapper.extractCarePlanSatisfiedUntil(carePlan.getExtension()));

        String organizationId = ExtensionMapper.extractOrganizationId(carePlan.getExtension());
        Organization organization = lookupResult.getOrganization(organizationId)
                .orElseThrow(() -> new IllegalStateException(String.format("Organization with id %s was not present when trying to map careplan %s!", organizationId, carePlan.getId())));
        carePlanModel.setDepartmentName(organization.getName());

        return carePlanModel;
    }

    public Timing mapFrequencyModel(FrequencyModel frequencyModel) {
        Timing timing = new Timing();

        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();

        EnumFactory<Timing.DayOfWeek> factory = new Timing.DayOfWeekEnumFactory();
        repeat.setDayOfWeek(frequencyModel.getWeekdays().stream().map(w -> new Enumeration<>(factory, w.toString().toLowerCase())).collect(Collectors.toList()));
        repeat.setTimeOfDay(List.of(new TimeType(frequencyModel.getTimeOfDay().toString())));
        timing.setRepeat(repeat);

        return timing;
    }

    public Patient mapPatientModel(PatientModel patientModel) {
        Patient patient = new Patient();

        // Id may be null, in case we are creating the patient.
        if(patientModel.getId() != null) {
            patient.setId(patientModel.getId().toString());
        }

        var name = buildName(patientModel.getGivenName(), patientModel.getFamilyName());
        patient.addName(name);

        patient.getIdentifier().add(makeCprIdentifier(patientModel.getCpr()));
        
        patient.addExtension(ExtensionMapper.mapCustomUserId(patientModel.getCustomUserId()));
        patient.addExtension(ExtensionMapper.mapCustomUserName(patientModel.getCustomUserName()));

        if(patientModel.getContactDetails() != null) {
            var contactDetails = patientModel.getContactDetails();

            var address = buildAddress(contactDetails);
            patient.addAddress(address);

            if(contactDetails.getPrimaryPhone() != null) {
                var primaryContactPoint = buildContactPoint(contactDetails.getPrimaryPhone(), 1);
                patient.addTelecom(primaryContactPoint);
            }

            if(contactDetails.getSecondaryPhone() != null) {
                var secondaryContactPoint = buildContactPoint(contactDetails.getSecondaryPhone(), 2);
                patient.addTelecom(secondaryContactPoint);
            }
        }

        if(patientModel.getPrimaryContact().getName() != null) {
            var contact = new Patient.ContactComponent();

            var contactName = buildName(patientModel.getPrimaryContact().getName());
            contact.setName(contactName);
            var organisation = new Reference();
            organisation.setReference(patientModel.getPrimaryContact().getOrganisation());
            contact.setOrganization(organisation);

            if(patientModel.getPrimaryContact().getAffiliation() != null) {

                var codeableConcept = new CodeableConcept();
                codeableConcept.setText(patientModel.getPrimaryContact().getAffiliation());
                contact.setRelationship(List.of(codeableConcept));
            }

            if(patientModel.getPrimaryContact().getContactDetails() != null) {
                var primaryRelativeContactDetails = patientModel.getPrimaryContact().getContactDetails();
                if(primaryRelativeContactDetails.getPrimaryPhone() != null) {
                    var relativePrimaryContactPoint = buildContactPoint(primaryRelativeContactDetails.getPrimaryPhone(), 1);
                    contact.addTelecom(relativePrimaryContactPoint);
                }

                if(primaryRelativeContactDetails.getSecondaryPhone() != null) {
                    var relativeSecondaryContactPoint = buildContactPoint(primaryRelativeContactDetails.getSecondaryPhone(), 2);
                    contact.addTelecom(relativeSecondaryContactPoint);
                }
            }

            patient.addContact(contact);
        }

        return patient;
    }

    public PatientModel mapPatient(Patient patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setId(extractId(patient));
        patientModel.setCustomUserId(ExtensionMapper.extractCustomUserId(patient.getExtension()));
        patientModel.setCustomUserName(ExtensionMapper.extractCustomUserName(patient.getExtension()));
        patientModel.setGivenName(extractGivenNames(patient));
        patientModel.setFamilyName(extractFamilyName(patient));
        patientModel.setCpr(extractCpr(patient));
        patientModel.setContactDetails(extractPatientContactDetails(patient));

        if(patient.getContact() != null && !patient.getContact().isEmpty()) {
            var organizationId = this.fhirClient.getOrganizationId();
            if(organizationId == null) throw new IllegalStateException("Mapping contact is only possible while the organization id is known");

            var optionalContact = patient.getContact().stream()
                    .filter(c -> c.getOrganization().getReference().equals(organizationId))
                    .findFirst();

            if (optionalContact.isPresent()){
                var contact = optionalContact.get();
                patientModel.getPrimaryContact().setName(contact.getName().getText());
                patientModel.getPrimaryContact().setOrganisation(contact.getOrganization().getReference());

                for(var coding : contact.getRelationshipFirstRep().getCoding()) {
                    if(coding.getSystem().equals(Systems.CONTACT_RELATIONSHIP)) {
                        patientModel.getPrimaryContact().setAffiliation(coding.getCode());
                    }
                }

                // Extract phone numbers
                if(contact.getTelecom() != null && !contact.getTelecom().isEmpty()) {
                    var primaryRelativeContactDetails = new ContactDetailsModel();

                    for(var telecom : contact.getTelecom()) {
                        if(telecom.getRank() == 1) {
                            primaryRelativeContactDetails.setPrimaryPhone(telecom.getValue());
                        }
                        if(telecom.getRank() == 2) {
                            primaryRelativeContactDetails.setSecondaryPhone(telecom.getValue());
                        }
                    }

                    patientModel.getPrimaryContact().setContactDetails(primaryRelativeContactDetails);
                }

            }
        }

        return patientModel;
    }

    public PlanDefinitionModel mapPlanDefinition(PlanDefinition planDefinition, FhirLookupResult lookupResult) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        mapBaseAttributesToModel(planDefinitionModel, planDefinition);

        planDefinitionModel.setName(planDefinition.getName());
        planDefinitionModel.setTitle(planDefinition.getTitle());
        planDefinitionModel.setStatus(Enum.valueOf(PlanDefinitionStatus.class, planDefinition.getStatus().toString()));
        if(planDefinition.getDate() != null)
            planDefinitionModel.setCreated(planDefinition.getDate().toInstant());

        if(planDefinition.getMeta().getLastUpdated() != null)
            planDefinitionModel.setLastUpdated(planDefinition.getMeta().getLastUpdated().toInstant());

        // Map actions to questionnaires, along with their thresholds
        planDefinitionModel.setQuestionnaires(planDefinition.getAction().stream().map(a -> mapPlanDefinitionAction(a, lookupResult)).collect(Collectors.toList()));

        return planDefinitionModel;
    }

    public Questionnaire mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        Questionnaire questionnaire = new Questionnaire();

        mapBaseAttributesToFhir(questionnaire, questionnaireModel);

        questionnaire.setTitle(questionnaireModel.getTitle());
        questionnaire.setStatus( mapQuestionnaireStatus(questionnaireModel.getStatus()) );
        questionnaire.getItem().addAll(questionnaireModel.getQuestions().stream()
            .map(this::mapQuestionnaireItem)
            .toList());
        questionnaire.getItem().addAll(mapQuestionnaireCallToActions(questionnaireModel.getCallToActions()));
        questionnaire.setVersion(questionnaireModel.getVersion());

        return questionnaire;
    }



    public QuestionnaireModel mapQuestionnaire(Questionnaire questionnaire) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        mapBaseAttributesToModel(questionnaireModel, questionnaire);
        questionnaireModel.setLastUpdated(questionnaire.getMeta().getLastUpdated());
        questionnaireModel.setTitle(questionnaire.getTitle());
        questionnaireModel.setStatus( mapQuestionnaireStatus(questionnaire.getStatus()) );
        questionnaireModel.setQuestions(questionnaire.getItem().stream()
            .filter(type -> !type.getType().equals(Questionnaire.QuestionnaireItemType.GROUP)) // filter out call-to-action's
            .map(this::mapQuestionnaireItem).collect(Collectors.toList()));
        questionnaireModel.setCallToActions(questionnaire.getItem().stream()
            .filter(q -> q.getType().equals(Questionnaire.QuestionnaireItemType.GROUP)) // process call-to-action's
            .flatMap(group -> group.getItem().stream())
            .map(this::mapQuestionnaireItem).collect(Collectors.toList()));
        questionnaireModel.setVersion(questionnaire.getVersion());
        return questionnaireModel;
    }

    private Enumerations.PublicationStatus mapQuestionnaireStatus(QuestionnaireStatus status) {
        return switch (status) {
            case ACTIVE -> Enumerations.PublicationStatus.ACTIVE;
            case DRAFT -> Enumerations.PublicationStatus.DRAFT;
            case RETIRED -> Enumerations.PublicationStatus.RETIRED;
            default ->
                    throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireStatus %s", status));
        };
    }

    private QuestionnaireStatus mapQuestionnaireStatus(Enumerations.PublicationStatus status) {
        return switch (status) {
            case ACTIVE -> QuestionnaireStatus.ACTIVE;
            case DRAFT -> QuestionnaireStatus.DRAFT;
            case RETIRED -> QuestionnaireStatus.RETIRED;
            default -> throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.status %s", status));
        };
    }

    public QuestionnaireResponse mapQuestionnaireResponseModel(QuestionnaireResponseModel questionnaireResponseModel) {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();

        mapBaseAttributesToFhir(questionnaireResponse, questionnaireResponseModel);

        questionnaireResponse.setQuestionnaire(questionnaireResponseModel.getQuestionnaireId().toString());

        for(var question : questionnaireResponseModel.getQuestions()) {
            questionnaireResponse.getItem().add(getQuestionnaireResponseItem(adaptAnswer(question.getAnswer())));
        }
        questionnaireResponse.setBasedOn(List.of(new Reference(questionnaireResponseModel.getCarePlanId().toString())));
        questionnaireResponse.setAuthor(new Reference(questionnaireResponseModel.getAuthorId().toString()));
        questionnaireResponse.setSource(new Reference(questionnaireResponseModel.getSourceId().toString()));
        questionnaireResponse.setAuthored(Date.from(questionnaireResponseModel.getAnswered()));
        questionnaireResponse.getExtension().add(ExtensionMapper.mapExaminationStatus(questionnaireResponseModel.getExaminationStatus()));
        if (questionnaireResponseModel.getExaminationAuthor() != null) {
            questionnaireResponse.getExtension().add(ExtensionMapper.mapExaminationAuthor(questionnaireResponseModel.getExaminationAuthor()));
        }
        questionnaireResponse.getExtension().add(ExtensionMapper.mapTriagingCategory(questionnaireResponseModel.getTriagingCategory()));
        questionnaireResponse.setSubject(new Reference(questionnaireResponseModel.getPatient().getId().toString()));

        return questionnaireResponse;
    }


    public QuestionnaireResponseModel mapQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, FhirLookupResult lookupResult) {
        QuestionnaireResponseModel questionnaireResponseModel = constructQuestionnaireResponse(questionnaireResponse, lookupResult);

        Questionnaire questionnaire = lookupResult.getQuestionnaire(questionnaireResponse.getQuestionnaire())
                .orElseThrow(() -> new IllegalStateException(String.format("No Questionnaire found with id %s!", questionnaireResponse.getQuestionnaire())));

        // Populate questionAnswerMap
        List<BaseQuestion<?>> questions = new ArrayList<>();

        for(var item : questionnaireResponse.getItem()) {
            BaseQuestion<?> question;
            try {
                question = getQuestion(questionnaire, item.getLinkId());
                question.answer(adaptAnswerModel(getAnswer(item)));
                questions.add( question );

            }   catch (IllegalStateException e) {
                // Corresponding question could not be found in the current/newest questionnaire
                // ignore
                // Or use the overloaded version which runs thought historical versions as well
                // and returns deprecated questions
                question = null;
            }
        }
        questionnaireResponseModel.setQuestions(questions);

        return questionnaireResponseModel;
    }
    public QuestionnaireResponseModel mapQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, FhirLookupResult lookupResult, List<Questionnaire> historicalQuestionnaires) {
        if (historicalQuestionnaires == null) return mapQuestionnaireResponse(questionnaireResponse, lookupResult);

        QuestionnaireResponseModel questionnaireResponseModel = constructQuestionnaireResponse(questionnaireResponse, lookupResult);

        // Populate questionAnswerMap
        List<BaseQuestion<? extends Answer>> questions = new ArrayList<>();

        //Look through all the given questionnaires
        for(var item : questionnaireResponse.getItem()) {
            BaseQuestion<?> question = null;
            boolean deprecated = false;
            int i = 0;


            for (Questionnaire q : historicalQuestionnaires) {
                if (i > 0) deprecated = true;
                boolean hasNext = i < historicalQuestionnaires.size() - 1;
                try {
                    question = getQuestion(q, item.getLinkId());
                    question.setDeprecated(deprecated);
                    break;
                } catch (IllegalStateException e) {
                    if (!hasNext)
                        throw new IllegalStateException("Corresponding question could not be found in the given questionnaires");
                }
                i++;
            }

            /*for (Questionnaire q : historicalQuestionnaires) {
                if (i > 0) deprecated = true;
                boolean hasNext = i < historicalQuestionnaires.size()-1;
                try {
                    question = getQuestion(q, item.getLinkId());
                    question.setDeprecated(deprecated);
                    break;
                }catch (IllegalStateException e) {
                    if (!hasNext) throw new IllegalStateException("Corresponding question could not be found in the given questionnaires");
                }
                i++;
            }*/

            AnswerModel answer = getAnswer(item);
            if (question != null) {
                question.answer(adaptAnswerModel(answer));
            }
            questions.add(question);
        }

        questionnaireResponseModel.setQuestions(questions);
        return questionnaireResponseModel;
    }



    private QuestionnaireResponseModel constructQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, FhirLookupResult lookupResult) {
        QuestionnaireResponseModel questionnaireResponseModel = new QuestionnaireResponseModel();

        mapBaseAttributesToModel(questionnaireResponseModel, questionnaireResponse);

        String questionnaireId = questionnaireResponse.getQuestionnaire();

        Questionnaire questionnaire = lookupResult.getQuestionnaire(questionnaireId)
                .orElseThrow(() -> new IllegalStateException(String.format("No Questionnaire found with id %s!", questionnaireId)));
        questionnaireResponseModel.setQuestionnaireName(questionnaire.getTitle());
        questionnaireResponseModel.setQuestionnaireId(extractId(questionnaire));


        if(questionnaireResponse.getBasedOn() == null || questionnaireResponse.getBasedOn().size() != 1) {
            throw new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: Expected exactly one BasedOn-attribute!", questionnaireResponseModel.getId().toString()));
        }
        questionnaireResponseModel.setCarePlanId(new QualifiedId(questionnaireResponse.getBasedOn().get(0).getReference()));

        if(questionnaireResponse.getAuthor() == null) {
            throw new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: No Author-attribute present!!", questionnaireResponseModel.getId().toString()));
        }
        questionnaireResponseModel.setAuthorId(new QualifiedId(questionnaireResponse.getAuthor().getReference()));

        if(questionnaireResponse.getSource() == null) {
            throw new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: No Source-attribute present!!", questionnaireResponseModel.getId().toString()));
        }
        questionnaireResponseModel.setSourceId(new QualifiedId(questionnaireResponse.getSource().getReference()));

        questionnaireResponseModel.setAnswered(questionnaireResponse.getAuthored().toInstant());
        questionnaireResponseModel.setExaminationStatus(ExtensionMapper.extractExaminationStatus(questionnaireResponse.getExtension()));
        questionnaireResponseModel.setTriagingCategory(ExtensionMapper.extractTriagingCategoory(questionnaireResponse.getExtension()));

        String practitionerId = ExtensionMapper.tryExtractExaminationAuthorPractitionerId(questionnaireResponse.getExtension());
        Optional<Practitioner> practitioner = lookupResult.getPractitioner(practitionerId);
        practitioner.ifPresent(value -> questionnaireResponseModel.setExaminationAuthor(mapPractitioner(value)));

        String patientId = questionnaireResponse.getSubject().getReference();
        Patient patient = lookupResult.getPatient(patientId)
                .orElseThrow(() -> new IllegalStateException(String.format("No Patient found with id %s!", patientId)));
        questionnaireResponseModel.setPatient(mapPatient(patient));

        String carePlanId = questionnaireResponse.getBasedOnFirstRep().getReference();
        CarePlan carePlan = lookupResult.getCarePlan(carePlanId)
                .orElseThrow(() -> new IllegalStateException(String.format("No CarePlan found with id %s!", carePlanId)));

        // loop careplanens plandefinitions for at finde den der har referencen til questionnaire
        for (CanonicalType canonicalType : carePlan.getInstantiatesCanonical()) {
            String planDefinitionId = canonicalType.getValue();
            PlanDefinition planDefinition = lookupResult.getPlanDefinition(planDefinitionId)
                    .orElseThrow(() -> new IllegalStateException(String.format("No PlanDefinition found with id %s!", planDefinitionId)));

            boolean found = false;
            for (PlanDefinition.PlanDefinitionActionComponent planDefinitionActionComponent : planDefinition.getAction()) {
                if (planDefinitionActionComponent.getDefinitionCanonicalType().equals(questionnaireId)) {
                    found = true;
                    break;
                }
            }

            if (found) {
                questionnaireResponseModel.setPlanDefinitionTitle(planDefinition.getTitle());
                break;
            }
        }

        return questionnaireResponseModel;
    }

    public FrequencyModel mapTiming(Timing timing) {
        FrequencyModel frequencyModel = new FrequencyModel();

        if(timing.getRepeat() != null) {
            Timing.TimingRepeatComponent repeat = timing.getRepeat();
            frequencyModel.setWeekdays(repeat.getDayOfWeek().stream().map(d -> Enum.valueOf(Weekday.class, d.getValue().toString())).collect(Collectors.toList()));
            if(!repeat.getTimeOfDay().isEmpty())
                frequencyModel.setTimeOfDay(LocalTime.parse(repeat.getTimeOfDay().get(0).getValue()));
        }

        return frequencyModel;
    }

    private void mapBaseAttributesToModel(BaseModel target, DomainResource source) {
        target.setId(extractId(source));
        target.setOrganizationId(ExtensionMapper.extractOrganizationId(source.getExtension()));
    }

    private void mapBaseAttributesToFhir(DomainResource target, BaseModel source) {
        // We may be creating the resource, and in that case, it is perfectly ok for it not to have id and organization id.
        if(source.getId() != null) {
            target.setId(source.getId().toString());
        }
        if(source.getOrganizationId() != null) {
            target.addExtension(ExtensionMapper.mapOrganizationId(source.getOrganizationId()));
        }
    }

    private QualifiedId extractId(DomainResource resource) {
        String unqualifiedVersionless = resource.getIdElement().toUnqualifiedVersionless().getValue();
        if(FhirUtils.isPlainId(unqualifiedVersionless)) {
            return new QualifiedId(unqualifiedVersionless, resource.getResourceType());
        }
        else if (FhirUtils.isQualifiedId(unqualifiedVersionless, resource.getResourceType())) {
            return new QualifiedId(unqualifiedVersionless);
        }
        else {
            throw new IllegalArgumentException(String.format("Illegal id for resource of type %s: %s!", resource.getResourceType(), unqualifiedVersionless));
        }
    }

    private Identifier makeCprIdentifier(String cpr) {
        Identifier identifier = new Identifier();

        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);

        return identifier;
    }

    public String extractCpr(Patient patient) {
        return patient.getIdentifier().get(0).getValue();
    }

    private HumanName buildName(String givenName, String familyName) {
        return buildName(givenName, familyName, null);
    }

    private HumanName buildName(String text) {
        return buildName(null, null, text);
    }

    private HumanName buildName(String givenName, String familyName, String text) {
        var name = new HumanName();

        name.addGiven(givenName);
        name.setFamily(familyName);
        name.setText(text);

        return name;
    }

    private Address buildAddress(ContactDetailsModel contactDetailsModel) {
        var address = new Address();

        address.addLine(contactDetailsModel.getStreet());
        address.setPostalCode(contactDetailsModel.getPostalCode());
        address.setCity(contactDetailsModel.getCity());

        return address;
    }

    private ContactPoint buildContactPoint(String phone, int rank) {
        var contactPoint = new ContactPoint();

        contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
        contactPoint.setValue(phone);
        contactPoint.setRank(rank);

        return contactPoint;
    }

    private String extractFamilyName(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getFamily();
    }

    private String extractGivenNames(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getGivenAsSingleString();
    }

    private ContactDetailsModel extractPatientContactDetails(Patient patient) {
        ContactDetailsModel contactDetails = new ContactDetailsModel();

        var lines = patient.getAddressFirstRep().getLine();
        if(lines != null && !lines.isEmpty()) {
            contactDetails.setStreet(lines.stream().map(PrimitiveType::getValue).collect(Collectors.joining(", ")));
        }
        contactDetails.setCity(patient.getAddressFirstRep().getCity());
        contactDetails.setPostalCode(patient.getAddressFirstRep().getPostalCode());
        contactDetails.setPrimaryPhone(extractPrimaryPhone(patient.getTelecom()));
        contactDetails.setSecondaryPhone(extractSecondaryPhone(patient.getTelecom()));
        contactDetails.setCountry(extractCountry(patient));

        return contactDetails;
    }

    private String extractCountry(Patient patient) {
        var country = patient.getAddressFirstRep().getCountry();
        if(country == null || country.isEmpty()) {
            return null;
        }
        return country;
    }

    private String extractPrimaryPhone(List<ContactPoint> contactPoints) {
        return extractPhone(contactPoints, 1);
    }

    private String extractSecondaryPhone(List<ContactPoint> contactPoints) {
        return extractPhone(contactPoints, 2);
    }

    private String extractPhone(List<ContactPoint> contactPoints, int rank) {
        if(contactPoints == null || contactPoints.isEmpty()) {
            return null;
        }
        for(ContactPoint cp : contactPoints) {
            if(cp.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && cp.getRank() == rank) {
                return cp.getValue();
            }
        }
        return null;
    }

    private BaseQuestion<?> getQuestion(Questionnaire questionnaire, String linkId) {
        var item = getQuestionnaireItem(questionnaire, linkId);
        if(item == null) {
            throw new IllegalStateException(String.format("Malformed QuestionnaireResponse: Question for linkId %s not found in Questionnaire %s!", linkId, questionnaire.getId()));
        }

        return mapQuestionnaireItem(item);
    }

    private Questionnaire.QuestionnaireItemComponent getQuestionnaireItem(Questionnaire questionnaire, String linkId) {
        for(var item : questionnaire.getItem()) {
            if(item != null && item.getLinkId() != null && item.getLinkId().equals(linkId)) {
                return item;
            }
        }
        return null;
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireItem(BaseQuestion<?> question) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();

        item.setLinkId(question.getLinkId());
        item.setText(question.getText());

//        TODO: Must be implemented. Temporarily excluded
//        if (question.getAbbreviation() != null) {
//            item.addExtension(ExtensionMapper.mapQuestionAbbreviation(question.getAbbreviation()));
//        }
//        if (question.getThresholds() != null) {
//            item.getExtension().addAll(ExtensionMapper.mapThresholds(question.getThresholds()));
//        }
//        if (question.getHelperText() != null) {
//            item.addItem(mapQuestionHelperText(question.getHelperText()));
//        }
//        item.setRequired(question.isRequired());
//        if (question.getOptions() != null) {
//            item.setAnswerOption( mapAnswerOptions(question.getOptions()) );
//        }
//        item.setType( mapQuestionType(question.getQuestionType()) );
//        if (question.getEnableWhens() != null) {
//            item.setEnableWhen( mapEnableWhens(question.getEnableWhens()) );
//        }
//
//        if (question.getMeasurementType() != null ) {
//            item.getCodeFirstRep()
//                    .setCode(question.getMeasurementType().getCode())
//                    .setDisplay(question.getMeasurementType().getDisplay())
//                    .setSystem(question.getMeasurementType().getSystem());
//        }
        return item;
    }

    private BaseQuestion<?> mapQuestionnaireItem(Questionnaire.QuestionnaireItemComponent item) {
        BaseQuestion<?> question = new Question<>(item.getText());

        question.setLinkId(item.getLinkId());
        //question.setText(item.getText());
        question.setAbbreviation(ExtensionMapper.extractQuestionAbbreviation(item.getExtension()));
        question.setHelperText( mapQuestionnaireItemHelperText(item.getItem()));
        question.setRequired(item.getRequired());
//
//       TODO: Must be implemented. Temporarily excluded
//        if(item.getAnswerOption() != null) {
//            question.setOptions( mapAnswerOptionComponents(item.getAnswerOption()) );
//        }
//        question.setQuestionType( mapQuestionType(item.getType()) );
//        if (item.hasEnableWhen()) {
//            question.setEnableWhens( mapEnableWhenComponents(item.getEnableWhen()) );
//        }
//        if (item.hasCode()) {
//            question.setMeasurementType(mapCodingConcept(item.getCodeFirstRep().getSystem(), item.getCodeFirstRep().getCode(), item.getCodeFirstRep().getDisplay()));
//        }
//
//        question.setThresholds(ExtensionMapper.extractThresholds(item.getExtensionsByUrl(Systems.THRESHOLD)));

        return question;
    }

    private String mapQuestionnaireItemHelperText(List<Questionnaire.QuestionnaireItemComponent> item) {
        return item.stream()
            .filter(i -> i.getType().equals(Questionnaire.QuestionnaireItemType.DISPLAY))
            .map(Questionnaire.QuestionnaireItemComponent::getText)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionHelperText(String text) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();
        item.setLinkId(IdType.newRandomUuid().getValueAsString())
            .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
            .setText(text);

        return item;
    }

    private List<Questionnaire.QuestionnaireItemComponent> mapQuestionnaireCallToActions(List<BaseQuestion<?>> callToActions) {
        List<Questionnaire.QuestionnaireItemComponent> result = new ArrayList<>();
        if (callToActions == null || callToActions.isEmpty()) {
            return result;
        }

        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();
        item.setType(Questionnaire.QuestionnaireItemType.GROUP);

        int counter = 1;
        for (BaseQuestion<?> callToAction : callToActions) {
            Questionnaire.QuestionnaireItemComponent cta = mapQuestionnaireItem(callToAction);
            cta.setLinkId(String.format("call-to-action%s", counter++));
            item.addItem(cta);
        }

        result.add(item);
        return result;
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireCallToAction(BaseQuestion<?> question) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();

//      TODO: Must be implemented. Temporarily excluded
//        item.setLinkId(question.getLinkId());
//        item.setText(question.getText());
//        item.setRequired(question.isRequired());
//        if (question.getOptions() != null) {
//            item.setAnswerOption( mapAnswerOptions(question.getOptions()) );
//        }
//        item.setType( mapQuestionType(question.getQuestionType()) );
//        if (question.getEnableWhens() != null) {
//            item.setEnableWhen( mapEnableWhens(question.getEnableWhens()) );
//        }
        return item;
    }

    private List<Questionnaire.QuestionnaireItemEnableWhenComponent> mapEnableWhens(List<BaseQuestion.EnableWhen> enableWhens) {
        return enableWhens
            .stream()
            .map(this::mapEnableWhen)
            .collect(Collectors.toList());
    }

    private Questionnaire.QuestionnaireItemEnableWhenComponent mapEnableWhen(BaseQuestion.EnableWhen enableWhen) {
        Questionnaire.QuestionnaireItemEnableWhenComponent enableWhenComponent = new Questionnaire.QuestionnaireItemEnableWhenComponent();

        enableWhenComponent.setOperator( mapEnableWhenOperator(enableWhen.getOperator()) );
        enableWhenComponent.setQuestion(enableWhen.getAnswer().getLinkId());
        enableWhenComponent.setAnswer(getValue(enableWhen.getAnswer()));
        enableWhenComponent.setOperator(mapEnableWhenOperator(enableWhen.getOperator()));

        return enableWhenComponent;
    }

    private List<BaseQuestion.EnableWhen> mapEnableWhenComponents(List<Questionnaire.QuestionnaireItemEnableWhenComponent> enableWhen) {
        return enableWhen
            .stream()
            .map(this::mapEnableWhenComponent)
            .collect(Collectors.toList());
    }

    private BaseQuestion.EnableWhen mapEnableWhenComponent(Questionnaire.QuestionnaireItemEnableWhenComponent enableWhen) {
        BaseQuestion.EnableWhen newEnableWhen = new BaseQuestion.EnableWhen();

        newEnableWhen.setOperator( mapEnableWhenOperator(enableWhen.getOperator()) );
        newEnableWhen.setAnswer( mapAnswer(enableWhen.getQuestion(), enableWhen.getAnswer()) );

        return newEnableWhen;
    }

    private AnswerModel mapAnswer(String question, Type answer) {
        AnswerModel answerModel = new AnswerModel();
        answerModel.setLinkId(question);

        if (answer instanceof StringType) {
            answerModel.setAnswerType(AnswerType.STRING);
            answerModel.setValue( ((StringType)answer).asStringValue() );
        }
        else if (answer instanceof BooleanType) {
            answerModel.setAnswerType(AnswerType.BOOLEAN);
            answerModel.setValue(((BooleanType) answer).asStringValue() );
        }
        else if (answer instanceof Quantity) {
            answerModel.setAnswerType(AnswerType.QUANTITY);
            answerModel.setValue(((Quantity) answer).getValueElement().asStringValue() );
        }
        else if (answer instanceof IntegerType) {
            answerModel.setAnswerType(AnswerType.INTEGER);
            answerModel.setValue(((IntegerType) answer).asStringValue() );
        }
        else {
            throw new IllegalArgumentException(String.format("Unsupported AnswerItem of type: %s", answer));
        }

        return answerModel;
    }

    private Questionnaire.QuestionnaireItemOperator mapEnableWhenOperator(EnableWhenOperator operator) {
        return switch (operator) {
            case EQUAL -> Questionnaire.QuestionnaireItemOperator.EQUAL;
            case LESS_THAN -> Questionnaire.QuestionnaireItemOperator.LESS_THAN;
            case LESS_OR_EQUAL -> Questionnaire.QuestionnaireItemOperator.LESS_OR_EQUAL;
            case GREATER_THAN -> Questionnaire.QuestionnaireItemOperator.GREATER_THAN;
            case GREATER_OR_EQUAL -> Questionnaire.QuestionnaireItemOperator.GREATER_OR_EQUAL;
            default ->
                    throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.QuestionnaireItemOperator %s", operator));
        };
    }

    private EnableWhenOperator mapEnableWhenOperator(Questionnaire.QuestionnaireItemOperator operator) {
        return switch (operator) {
            case EQUAL -> EnableWhenOperator.EQUAL;
            case LESS_THAN -> EnableWhenOperator.LESS_THAN;
            case LESS_OR_EQUAL -> EnableWhenOperator.LESS_OR_EQUAL;
            case GREATER_THAN -> EnableWhenOperator.GREATER_THAN;
            case GREATER_OR_EQUAL -> EnableWhenOperator.GREATER_OR_EQUAL;
            default ->
                    throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireItemOperator %s", operator));
        };
    }

    private List<Questionnaire.QuestionnaireItemAnswerOptionComponent> mapAnswerOptions(List<String> answerOptions) {
        return answerOptions
            .stream()
            .map(oc -> new Questionnaire.QuestionnaireItemAnswerOptionComponent().setValue( new StringType(oc)))
            .collect(Collectors.toList());
    }

    private List<String> mapAnswerOptionComponents(List<Questionnaire.QuestionnaireItemAnswerOptionComponent> optionComponents) {
        return optionComponents
                .stream()
                .map(oc -> oc.getValue().primitiveValue())
                .collect(Collectors.toList());
    }

    private QuestionType mapQuestionType(Questionnaire.QuestionnaireItemType type) {
        return switch (type) {
            case CHOICE -> QuestionType.CHOICE;
            case INTEGER -> QuestionType.INTEGER;
            case QUANTITY -> QuestionType.QUANTITY;
            case STRING -> QuestionType.STRING;
            case BOOLEAN -> QuestionType.BOOLEAN;
            case DISPLAY -> QuestionType.DISPLAY;
            default -> throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireItemType %s", type));
        };
    }

    private Questionnaire.QuestionnaireItemType mapQuestionType(QuestionType type) {
        return switch (type) {
            case CHOICE -> Questionnaire.QuestionnaireItemType.CHOICE;
            case INTEGER -> Questionnaire.QuestionnaireItemType.INTEGER;
            case QUANTITY -> Questionnaire.QuestionnaireItemType.QUANTITY;
            case STRING -> Questionnaire.QuestionnaireItemType.STRING;
            case BOOLEAN -> Questionnaire.QuestionnaireItemType.BOOLEAN;
            case DISPLAY -> Questionnaire.QuestionnaireItemType.DISPLAY;
            default -> throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.ItemType %s", type));
        };
    }

    private AnswerModel getAnswer(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        AnswerModel answer = new AnswerModel();

        var answerItem = extractAnswerItem(item);
        answer.setLinkId(item.getLinkId());

        if(answerItem.hasValueStringType()) {
            answer.setValue(answerItem.getValue().primitiveValue());
        }
        else if(answerItem.hasValueIntegerType()) {
            answer.setValue(answerItem.getValueIntegerType().primitiveValue());
        }
        else if(answerItem.hasValueQuantity()) {
            answer.setValue(answerItem.getValueQuantity().getValueElement().primitiveValue());
        }
        else if(answerItem.hasValueBooleanType()) {
            answer.setValue(answerItem.getValueBooleanType().primitiveValue());
        }
        answer.setAnswerType(getAnswerType(answerItem));

        return answer;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent extractAnswerItem(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        if(item.getAnswer() == null || item.getAnswer().size() != 1) {
            throw new IllegalStateException("Expected exactly one answer!");
        }
        return item.getAnswer().get(0);
    }

    private AnswerType getAnswerType(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answerItem) {
        Type answerType = answerItem.getValue();
        return getAnswerType(answerType);
    }

    private AnswerType getAnswerType(Type answerType) {
        if (answerType instanceof StringType) {
            return AnswerType.STRING;
        }
        if (answerType instanceof BooleanType) {
            return AnswerType.BOOLEAN;
        }
        else if (answerType instanceof Quantity) {
            return AnswerType.QUANTITY;
        }
        else if (answerType instanceof IntegerType) {
            return AnswerType.INTEGER;
        }
        else {
            throw new IllegalArgumentException(String.format("Unsupported AnswerItem of type: %s", answerType));
        }
    }

    private CarePlan.CarePlanActivityComponent buildCarePlanActivity(QuestionnaireWrapperModel questionnaireWrapperModel) {
        CanonicalType instantiatesCanonical = new CanonicalType(questionnaireWrapperModel.getQuestionnaire().getId().toString());
        Type timing = mapFrequencyModel(questionnaireWrapperModel.getFrequency());
        Extension activitySatisfiedUntil = ExtensionMapper.mapActivitySatisfiedUntil(questionnaireWrapperModel.getSatisfiedUntil());

        return buildActivity(instantiatesCanonical, timing, activitySatisfiedUntil);
    }

    private CarePlan.CarePlanActivityComponent buildActivity(CanonicalType instantiatesCanonical, Type timing, Extension activitySatisfiedUntil) {
        CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();

        activity.setDetail(buildDetail(instantiatesCanonical, timing, activitySatisfiedUntil));

        return activity;
    }

    private CarePlan.CarePlanActivityDetailComponent buildDetail(CanonicalType instantiatesCanonical, Type timing, Extension activitySatisfiedUntil) {
        CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();

        detail.setInstantiatesCanonical(List.of(instantiatesCanonical));
        detail.setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED);
        detail.addExtension(activitySatisfiedUntil);
        detail.setScheduled(timing);

        return detail;
    }

    private <T extends Answer> AnswerModel adaptAnswer(T answer){
        var model = new AnswerModel();

        // TODO: implement the conversion

        return model;
    }

    private <T extends Answer> T adaptAnswerModel(AnswerModel answer){
        var a = new Text("");

        // TODO: implement the conversion

        return (T) a;
    }


    private QuestionnaireResponse.QuestionnaireResponseItemComponent getQuestionnaireResponseItem(AnswerModel answer) {
        var item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();

        item.setLinkId(answer.getLinkId());
        item.getAnswer().add(getAnswerItem(answer));

        return item;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent getAnswerItem(AnswerModel answer) {
        var answerItem = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();

        answerItem.setValue(getValue(answer));

        return answerItem;
    }

    public static Type getValue(AnswerModel answer) {
        return switch (answer.getAnswerType()) {
            case INTEGER -> new IntegerType(answer.getValue());
            case STRING -> new StringType(answer.getValue());
            case QUANTITY -> new Quantity(Double.parseDouble(answer.getValue()));
            case BOOLEAN -> new BooleanType(answer.getValue());
            default ->
                    throw new IllegalArgumentException(String.format("Unknown AnswerType: %s", answer.getAnswerType()));
        };
    }

    private QuestionnaireWrapperModel mapPlanDefinitionAction(PlanDefinition.PlanDefinitionActionComponent action, FhirLookupResult lookupResult) {
        var wrapper = new QuestionnaireWrapperModel();

        String questionnaireId = action.getDefinitionCanonicalType().getValue();
        Questionnaire questionnaire = lookupResult
                .getQuestionnaire(questionnaireId)
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up Questionnaire with id %s!", questionnaireId)));
        wrapper.setQuestionnaire(mapQuestionnaire(questionnaire));

        wrapper.setThresholds( ExtensionMapper.extractThresholds(action.getExtensionsByUrl(Systems.THRESHOLD)) );

        return wrapper;
    }

    public PractitionerModel mapPractitioner(Practitioner practitioner) {
        PractitionerModel practitionerModel = new PractitionerModel();

        practitionerModel.setId(extractId(practitioner));
        practitionerModel.setGivenName(practitioner.getNameFirstRep().getGivenAsSingleString());
        practitionerModel.setFamilyName(practitioner.getNameFirstRep().getFamily());

        return practitionerModel;
    }

    public List<MeasurementTypeModel> extractMeasurementTypes(ValueSet valueSet) {
        List<MeasurementTypeModel> result = new ArrayList<>();

        valueSet.getCompose().getInclude()
            .forEach(csc -> {
                var measurementTypes = csc.getConcept().stream()
                    .map(crc -> mapCodingConcept(csc.getSystem(), crc))
                    .toList();

                result.addAll(measurementTypes);
            });

        return result;
    }

    private MeasurementTypeModel mapCodingConcept(String system, ValueSet.ConceptReferenceComponent concept) {
        return mapCodingConcept(system, concept.getCode(), concept.getDisplay());
    }

    private MeasurementTypeModel mapCodingConcept(String system, String code, String display) {
        MeasurementTypeModel measurementTypeModel = new MeasurementTypeModel();

        measurementTypeModel.setSystem(system);
        measurementTypeModel.setCode(code);
        measurementTypeModel.setDisplay(display);

        return measurementTypeModel;
    }

    public PlanDefinition mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinition planDefinition = new PlanDefinition();

        mapBaseAttributesToFhir(planDefinition, planDefinitionModel);

        planDefinition.setTitle(planDefinitionModel.getTitle());
        planDefinition.setStatus(Enumerations.PublicationStatus.valueOf(planDefinitionModel.getStatus().toString()));

        // Map questionnaires to actions
        if(planDefinitionModel.getQuestionnaires() != null) {
            planDefinition.setAction(planDefinitionModel.getQuestionnaires()
                .stream()
                .map(this::buildPlanDefinitionAction)
                .collect(Collectors.toList()));
        }

        return planDefinition;
    }

    private PlanDefinition.PlanDefinitionActionComponent buildPlanDefinitionAction(QuestionnaireWrapperModel questionnaireWrapperModel) {
        CanonicalType definitionCanonical = new CanonicalType(questionnaireWrapperModel.getQuestionnaire().getId().toString());

        List<Extension> thresholds = ExtensionMapper.mapThresholds(questionnaireWrapperModel.getThresholds());

        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        action.setDefinition(definitionCanonical);
        action.setExtension(thresholds);

        return action;
    }



}
