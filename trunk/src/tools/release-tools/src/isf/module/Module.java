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

}
