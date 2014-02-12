package isf.module;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public interface Module {

	Object getName();

	void saveGeneratedModule() throws OWLOntologyStorageException;

	void saveGeneratedModuleTransitive() throws OWLOntologyStorageException;

	IRI getIri();

	void saveModuleDefinitionFiles() throws OWLOntologyStorageException;

	void saveModuleDefinitionFilesTransitive() throws OWLOntologyStorageException;

	OWLOntology getOntology();

	void generateModuleTransitive() throws Exception;

	void generateModule() throws Exception;

	/**
	 * This will cause any axiom in the legacy ontologies to be also included in
	 * the module. This method, and the cleanLegacyOntologies() can help with
	 * migrating a legacy ontology to become an ISF module. This call will allow
	 * the module to include legacy content that is not yet in the ISF, or
	 * content that will not be in the ISF but is still needed in the generated
	 * module.
	 */
	void addLegacyOntologies();

	void addLegacyOntologiesTransitive();

	/**
	 * This will remove all axioms from all legacy ontologies based on what is
	 * currently in the module ontology. The idea is that after the module
	 * ontology is populated, the legacy ontology files can be cleaned from any
	 * module axiom since the module now generates those axioms. It is a way to
	 * simplify migrating legacy ontologies to being ISF modules (i.e. being an
	 * ISF module based on the ISF ontology).
	 */
	void cleanLegacyOntologies();

	void cleanLegacyOntologiesTransitive();

	void saveLegacyOntologies() throws OWLOntologyStorageException;

	void saveLegacyOntologiesTransitive() throws OWLOntologyStorageException;

}
