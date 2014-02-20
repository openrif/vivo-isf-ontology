package isf.release.action;

import isf.ISFUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class OneDefinitionCheck extends Action {

	Set<String> ignoredPrefixes = new HashSet<String>();

	{
		ignoredPrefixes.add("http://aims.fao.org/aos/geopolitical.owl");
		ignoredPrefixes.add("http://purl.obolibrary.org/obo/MP");
		ignoredPrefixes.add("http://purl.obolibrary.org/obo/NCBITaxon");
		ignoredPrefixes.add("http://purl.obolibrary.org/obo/UBERON");
		ignoredPrefixes.add("http://www.ebi.ac.uk/efo/swo/SWO");
		ignoredPrefixes.add("http://www.w3.org/2006/vcard/ns");
	}

	boolean isIgnored(OWLEntity entity) {
		if (entity.isOWLNamedIndividual()) {
			return true;
		}
		for (String ignoredPrefix : ignoredPrefixes) {
			if (entity.getIRI().toString().startsWith(ignoredPrefix)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void doAction(OWLOntology ontology, OWLOntologyManager man, Reporter reporter) {

		super.doAction(ontology, man, reporter);

		reporter.setHeading("Checking definitions");

		List<OWLEntity> entities = ISFUtil.getEntitiesSortedByIri(ontology, true);

		HashMap<IRI, Set<String>> iriDefinitions = new HashMap<IRI, Set<String>>();
		for (OWLEntity entity : entities) {
			Set<OWLAnnotationAssertionAxiom> axioms = ISFUtil.getSubjectAnnotationAxioms(ontology,
					true, entity.getIRI());

			Set<String> entityDefnitions = iriDefinitions.get(entity.getIRI());
			if (entityDefnitions == null) {
				entityDefnitions = new HashSet<String>();
				iriDefinitions.put(entity.getIRI(), entityDefnitions);
			}

			for (OWLAnnotationAssertionAxiom axiom : axioms) {
				if (axiom.getProperty().getIRI().toString()
						.equals("http://purl.obolibrary.org/obo/IAO_0000115")) {
					String line = "iao:definition: " + axiom.getValue().toString();
					entityDefnitions.add(line);
				}
			}
		}

		for (OWLEntity entity : entities) {
			if (isIgnored(entity)) {
				continue;
			}
			Set<String> definitions = iriDefinitions.get(entity.getIRI());
			if (definitions.size() == 0) {
				reporter.addLine("Missing def for: " + reporter.renderOWLObject(entity) + " IRI: "
						+ entity.getIRI());
			} else if (definitions.size() > 1) {
				reporter.addLine("Multiple defs for: " + reporter.renderOWLObject(entity)
						+ " IRI: " + entity.getIRI());
				for (String definition : definitions) {
					reporter.addLine("\tdef: " + definition.replace('\n', '_'));
				}
			}
		}

	}
}
