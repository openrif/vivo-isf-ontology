package isf.module;

import isf.ISFUtil;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

public class ModuleUtil {

	private static OWLDataFactory df = OWLManager.getOWLDataFactory();

	public static final String INCLUDE_ANNOTATION_IRI = "http://purl.obolibrary.org/obo/ARG_include";
	public static final String INCLUDE_SUBS_ANNOTATION_IRI = "http://purl.obolibrary.org/obo/ARG_include_subs";

	public static final String EXCLUDE_ANNOTATION_IRI = "http://purl.obolibrary.org/obo/ARG_exclude";
	public static final String EXCLUDE_SUBS_ANNOTATION_IRI = "http://purl.obolibrary.org/obo/ARG_exclude_subs";

	public static Set<OWLAnnotationAssertionAxiom> getIncludeAxioms(
			OWLOntology ontology, boolean includeImports) {

		return ISFUtil
				.getAnnotationAxioms(ontology, df.getOWLAnnotationProperty(IRI
						.create(INCLUDE_ANNOTATION_IRI)), includeImports);
	}

	public static Set<OWLAnnotationAssertionAxiom> getIncludeSubsAxioms(
			OWLOntology ontology, boolean includeImports) {

		return ISFUtil.getAnnotationAxioms(ontology, df
				.getOWLAnnotationProperty(IRI
						.create(INCLUDE_SUBS_ANNOTATION_IRI)), includeImports);
	}

	public static Set<OWLAnnotationAssertionAxiom> getExcludeAxioms(
			OWLOntology ontology, boolean includeImports) {

		return ISFUtil
				.getAnnotationAxioms(ontology, df.getOWLAnnotationProperty(IRI
						.create(EXCLUDE_ANNOTATION_IRI)), includeImports);
	}

	public static Set<OWLAnnotationAssertionAxiom> getExcludeSubsAxioms(
			OWLOntology ontology, boolean includeImports) {

		return ISFUtil.getAnnotationAxioms(ontology, df
				.getOWLAnnotationProperty(IRI
						.create(EXCLUDE_SUBS_ANNOTATION_IRI)), includeImports);
	}

	public static Set<OWLEntity> getIncludeEntities(OWLOntology ontology,
			boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getIncludeAxioms(ontology,
				includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	public static Set<OWLEntity> getIncludeSubsEntities(OWLOntology ontology,
			boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getIncludeSubsAxioms(
				ontology, includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	public static Set<OWLEntity> getExcludeEntities(OWLOntology ontology,
			boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getExcludeAxioms(ontology,
				includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	public static Set<OWLEntity> getExcludeSubsEntities(OWLOntology ontology,
			boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = getExcludeSubsAxioms(
				ontology, includeImports);
		return getSubjectEntities(ontology, includeImports, axioms);
	}

	private static Set<OWLEntity> getSubjectEntities(OWLOntology ontology,
			boolean includeImports, Set<OWLAnnotationAssertionAxiom> axioms) {
		Set<OWLEntity> entities = new HashSet<OWLEntity>();
		IRI subject;
		for (OWLAnnotationAssertionAxiom a : axioms) {
			if (a.getSubject() instanceof IRI) {
				subject = (IRI) a.getSubject();
				entities.addAll(ontology.getEntitiesInSignature(subject,
						includeImports));
			}
		}
		return entities;
	}

}
