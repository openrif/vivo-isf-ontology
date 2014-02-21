package isf.module.internal;

import isf.ISFUtil;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

public class Util {

	private static OWLDataFactory df = OWLManager.getOWLDataFactory();

	public static final String INCLUDE_ANNOTATION_IRI = ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-module-include";
	public static final String INCLUDE_SUBS_ANNOTATION_IRI = ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-module-include-subs";
	public static final String INCLUDE_INSTANCES_ANNOTATION_IRI = ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-module-include-instances";

	public static final String EXCLUDE_ANNOTATION_IRI = ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-module-exclude";

	public static final String EXCLUDE_SUBS_ANNOTATION_IRI = ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-module-exclude-subs";

	public static Set<OWLAnnotationAssertionAxiom> getIncludeAxioms(OWLOntology ontology,
			boolean includeImports) {

		return ISFUtil.getAnnotationAssertionAxioms(ontology,
				df.getOWLAnnotationProperty(IRI.create(INCLUDE_ANNOTATION_IRI)), includeImports);
	}

	public static Set<OWLAnnotationAssertionAxiom> getIncludeInstancesAxioms(OWLOntology ontology,
			boolean includeImports) {

		return ISFUtil.getAnnotationAssertionAxioms(ontology,
				df.getOWLAnnotationProperty(IRI.create(INCLUDE_INSTANCES_ANNOTATION_IRI)),
				includeImports);
	}

	public static Set<OWLAnnotationAssertionAxiom> getIncludeSubsAxioms(OWLOntology ontology,
			boolean includeImports) {

		return ISFUtil.getAnnotationAssertionAxioms(ontology,
				df.getOWLAnnotationProperty(IRI.create(INCLUDE_SUBS_ANNOTATION_IRI)),
				includeImports);
	}

	public static Set<OWLAnnotationAssertionAxiom> getExcludeAxioms(OWLOntology ontology,
			boolean includeImports) {

		return ISFUtil.getAnnotationAssertionAxioms(ontology,
				df.getOWLAnnotationProperty(IRI.create(EXCLUDE_ANNOTATION_IRI)), includeImports);
	}

	public static Set<OWLAnnotationAssertionAxiom> getExcludeSubsAxioms(OWLOntology ontology,
			boolean includeImports) {

		return ISFUtil.getAnnotationAssertionAxioms(ontology,
				df.getOWLAnnotationProperty(IRI.create(EXCLUDE_SUBS_ANNOTATION_IRI)),
				includeImports);
	}

	public static Set<OWLEntity> getIncludeEntities(OWLOntology ontology, boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getIncludeAxioms(ontology, includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	public static Set<OWLEntity> getIncludeInstances(OWLOntology ontology, boolean includeImports) {

		Set<OWLAnnotationAssertionAxiom> axioms = getIncludeInstancesAxioms(ontology,
				includeImports);

		return getSubjectEntities(ontology, includeImports, axioms);

	}

	public static Set<OWLEntity> getIncludeSubsEntities(OWLOntology ontology, boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getIncludeSubsAxioms(ontology, includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	public static Set<OWLEntity> getExcludeEntities(OWLOntology ontology, boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getExcludeAxioms(ontology, includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	public static Set<OWLEntity> getExcludeSubsEntities(OWLOntology ontology, boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getExcludeSubsAxioms(ontology, includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	private static Set<OWLEntity> getSubjectEntities(OWLOntology ontology, boolean includeImports,
			Set<OWLAnnotationAssertionAxiom> axioms) {
		Set<OWLEntity> entities = new HashSet<OWLEntity>();
		IRI subject;
		for (OWLAnnotationAssertionAxiom a : axioms) {
			if (a.getSubject() instanceof IRI) {
				subject = (IRI) a.getSubject();
				entities.addAll(ontology.getEntitiesInSignature(subject, includeImports));
			}
		}
		return entities;
	}

}
