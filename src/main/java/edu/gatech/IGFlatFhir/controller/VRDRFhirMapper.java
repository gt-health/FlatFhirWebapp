package edu.gatech.IGFlatFhir.controller;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Composition.CompositionAttestationMode;
import org.hl7.fhir.dstu3.model.Composition.CompositionAttesterComponent;
import org.hl7.fhir.dstu3.model.Composition.SectionComponent;
import org.hl7.fhir.dstu3.model.Reference;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.gatech.IGFlatFhir.exception.MissingDataNodeException;
import edu.gatech.IGFlatFhir.exception.MissingInformationException;
import edu.gatech.IGFlatFhir.exception.MissingKeyException;
import edu.gatech.IGFlatFhir.exception.PathFormatException;
import edu.gatech.IGFlatFhir.exception.WrongTypeException;
import edu.gatech.IGFlatFhir.model.Data;
import edu.gatech.IGFlatFhir.model.IGMapDocument;
import edu.gatech.IGFlatFhir.model.Profiles;
import edu.gatech.VRDR.context.VRDRFhirContext;
import edu.gatech.VRDR.mapper.AutopsyPerformedIndicatorMapper;
import edu.gatech.VRDR.mapper.CertifierMapper;
import edu.gatech.VRDR.mapper.DecedentAgeMapper;
import edu.gatech.VRDR.mapper.DecedentMapper;
import edu.gatech.VRDR.mapper.MannerOfDeathMapper;
import edu.gatech.VRDR.model.AutopsyPerformedIndicator;
import edu.gatech.VRDR.model.Certifier;
import edu.gatech.VRDR.model.DeathCertificate;
import edu.gatech.VRDR.model.DeathCertificateDocument;
import edu.gatech.VRDR.model.Decedent;
import edu.gatech.VRDR.model.DecedentAge;
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
		CommonUtil.addSection(deathCertificate, decedent);
		
		Certifier certifier = new CertifierMapper().map(documentObject,"");
		Reference certifierReference = new Reference(certifier.getId());
		deathCertificate.addAttester(new CompositionAttesterComponent()
				.setParty(certifierReference)
				.addMode(CompositionAttestationMode.LEGAL));
		CommonUtil.addBundleEntry(deathRecordDocument, certifier);
		CommonUtil.addSection(deathCertificate, certifier);
		
		AutopsyPerformedIndicator autopsyPerformedIndicator = new AutopsyPerformedIndicatorMapper().map(documentObject, "");
		if(autopsyPerformedIndicator != null) {
			autopsyPerformedIndicator.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, autopsyPerformedIndicator);
			CommonUtil.addSection(deathCertificate, autopsyPerformedIndicator);
		}
		DecedentAge decedentAge = new DecedentAgeMapper().map(documentObject,"");
		if(decedentAge != null) {
			decedentAge.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, decedentAge);
			CommonUtil.addSection(deathCertificate, decedentAge);
		}
		MannerOfDeath mannerOfDeath = new MannerOfDeathMapper().map(documentObject,"");
		if(mannerOfDeath != null) {
			decedentAge.setSubject(decedentReference);
			CommonUtil.addBundleEntry(deathRecordDocument, mannerOfDeath);
			CommonUtil.addSection(deathCertificate, mannerOfDeath);
		}
		return deathRecordDocument;
	}
}