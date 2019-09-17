package edu.gatech.IGCompass.controller;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Composition.CompositionAttestationMode;
import org.hl7.fhir.dstu3.model.Composition.CompositionAttesterComponent;
import org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent;
import org.hl7.fhir.dstu3.model.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.gatech.IGCompass.exception.MissingDataNodeException;
import edu.gatech.IGCompass.exception.MissingInformationException;
import edu.gatech.IGCompass.exception.MissingKeyException;
import edu.gatech.IGCompass.exception.PathFormatException;
import edu.gatech.IGCompass.exception.WrongTypeException;
import edu.gatech.IGCompass.model.IGMapDocument;
import edu.gatech.VRDR.context.VRDRFhirContext;
import edu.gatech.VRDR.mapper.AutopsyPerformedIndicatorMapper;
import edu.gatech.VRDR.mapper.CauseOfDeathConditionMapper;
import edu.gatech.VRDR.mapper.CertifierMapper;
import edu.gatech.VRDR.mapper.DeathDateMapper;
import edu.gatech.VRDR.mapper.DecedentAgeMapper;
import edu.gatech.VRDR.mapper.DecedentMapper;
import edu.gatech.VRDR.mapper.InjuryIncidentMapper;
import edu.gatech.VRDR.mapper.MannerOfDeathMapper;
import edu.gatech.VRDR.model.AutopsyPerformedIndicator;
import edu.gatech.VRDR.model.CauseOfDeathCondition;
import edu.gatech.VRDR.model.CauseOfDeathPathway;
import edu.gatech.VRDR.model.Certifier;
import edu.gatech.VRDR.model.DeathCertificate;
import edu.gatech.VRDR.model.DeathCertificateDocument;
import edu.gatech.VRDR.model.DeathDate;
import edu.gatech.VRDR.model.Decedent;
import edu.gatech.VRDR.model.DecedentAge;
import edu.gatech.VRDR.model.InjuryIncident;
import edu.gatech.VRDR.model.MannerOfDeath;
import edu.gatech.VRDR.model.util.CommonUtil;

@Path("VRDR")
public class VRDRFhirMapper {
	
	@POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public String mapVRDRToInternal(String requestData){
		ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode mapDocumentJson;
		try {
			mapDocumentJson = (ObjectNode)objectMapper.readTree(requestData);
		} catch (IOException e1) {
			return e1.toString();
		}
        IGMapDocument documentObject;
        DeathCertificateDocument deathRecordDocument;
        try {
			documentObject = IGMapDocument.parseFromJson(mapDocumentJson);
			deathRecordDocument = generatedVRDRDeathRecordDocument(documentObject);
		} catch (MissingInformationException | WrongTypeException | MissingKeyException | MissingDataNodeException | PathFormatException e) {
			return e.toString();
		}
        return new VRDRFhirContext().getCtx().newJsonParser().encodeResourceToString(deathRecordDocument);
	}
	
	public DeathCertificateDocument generatedVRDRDeathRecordDocument(IGMapDocument documentObject) throws MissingInformationException, WrongTypeException, MissingDataNodeException, PathFormatException {
		DeathCertificateDocument deathRecordDocument = new DeathCertificateDocument();
		DeathCertificate deathCertificate = new DeathCertificate();
		deathRecordDocument.addEntry(new BundleEntryComponent().setResource(deathCertificate));
		
		Decedent decedent = new DecedentMapper().map(documentObject,"");
		Reference decedentReference = new Reference(decedent.getId());
		deathCertificate.setSubject(decedentReference);
		CommonUtil.addBundleEntry(deathRecordDocument, decedent);
		CommonUtil.addSectionEntry(deathCertificate, decedent);
		
		Certifier certifier = new CertifierMapper().map(documentObject,"");
		Reference certifierReference = new Reference(certifier.getId());
		deathCertificate.addAttester(new CompositionAttesterComponent()
				.setParty(certifierReference)
				.addMode(CompositionAttestationMode.LEGAL));
		CommonUtil.addBundleEntry(deathRecordDocument, certifier);
		CommonUtil.addSectionEntry(deathCertificate, certifier);
		
		AutopsyPerformedIndicator autopsyPerformedIndicator = new AutopsyPerformedIndicatorMapper().map(documentObject, "");
		if(autopsyPerformedIndicator != null) {
			autopsyPerformedIndicator.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, autopsyPerformedIndicator);
			CommonUtil.addSectionEntry(deathCertificate, autopsyPerformedIndicator);
		}
		CauseOfDeathCondition causeOfDeathCondition = new CauseOfDeathConditionMapper().map(documentObject,"");
		if(causeOfDeathCondition != null) {
			causeOfDeathCondition.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, causeOfDeathCondition);
			CommonUtil.addSectionEntry(deathCertificate, causeOfDeathCondition);
		}
		
		//TODO: Work with CauseOfDeathPathway and multiple cause of death conditions
		CauseOfDeathPathway causeOfDeathPathway = new CauseOfDeathPathway();
		causeOfDeathPathway.addEntry(new ListEntryComponent().setItem(new Reference(causeOfDeathCondition)));
		CommonUtil.addBundleEntry(deathRecordDocument, causeOfDeathPathway);
		CommonUtil.addSectionEntry(deathCertificate, causeOfDeathPathway);
		
		DeathDate deathDate = new DeathDateMapper().map(documentObject,"");
		if(deathDate != null) {
			deathDate.addPerformer(certifierReference);
			deathDate.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, deathDate);
			CommonUtil.addSectionEntry(deathCertificate, deathDate);
		}
		
		DecedentAge decedentAge = new DecedentAgeMapper().map(documentObject,"");
		if(decedentAge != null) {
			decedentAge.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, decedentAge);
			CommonUtil.addSectionEntry(deathCertificate, decedentAge);
		}
		InjuryIncident injuryIncident = new InjuryIncidentMapper().map(documentObject,"");
		if(injuryIncident != null){
			injuryIncident.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, injuryIncident);
			CommonUtil.addSectionEntry(deathCertificate, injuryIncident);
		}
		MannerOfDeath mannerOfDeath = new MannerOfDeathMapper().map(documentObject,"");
		if(mannerOfDeath != null) {
			decedentAge.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, mannerOfDeath);
			CommonUtil.addSectionEntry(deathCertificate, mannerOfDeath);
		}
		return deathRecordDocument;
	}
}