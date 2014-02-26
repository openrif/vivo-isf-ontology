package isf.module;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public abstract class AbstractModule implements Module {

	private OWLReasoner reasoner;
	private String name;
	private File directory;
	private File outputDirectory;
	private Set<Module> imports = new HashSet<Module>();
	private OWLOntologyManager man;

	public OWLOntologyManager getManager() {
		return man;
	}

	public AbstractModule(String moduleName, OWLOntologyManager manager, File directory,
			File outputDirectory) {

		if (moduleName == null) {
			throw new IllegalStateException("Module name cannot be null.");
		}
		this.name = moduleName;
		this.man = manager;

		this.directory = directory;
		this.directory.mkdirs();

		this.outputDirectory = outputDirectory;
		this.outputDirectory.mkdirs();

	}


	@Override
	public String getName() {
		return name;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public OWLReasoner getReasoner() {

		return reasoner;
	}

	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	protected void saveOntology(OWLOntology ontology) throws OWLOntologyStorageException {
		man.saveOntology(ontology);
	}


	@Override
	public abstract void generateModule() throws Exception;

	@Override
	public abstract void saveGeneratedModule() throws OWLOntologyStorageException;

	@Override
	public void saveGeneratedModuleTransitive() throws OWLOntologyStorageException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveModuleDefinitionFiles() throws OWLOntologyStorageException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveModuleDefinitionFilesTransitive() throws OWLOntologyStorageException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void generateModuleTransitive() throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public IRI getIri() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OWLOntology getOntology() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLegacyOntologies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cleanLegacyOntologies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLegacyOntologiesTransitive() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void cleanLegacyOntologiesTransitive() {
		throw new UnsupportedOperationException();

	}

	@Override
	public void saveLegacyOntologies() throws OWLOntologyStorageException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void saveLegacyOntologiesTransitive() throws OWLOntologyStorageException {
		throw new UnsupportedOperationException();

	}

	public abstract void close();

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public void addImport(Module module) {
		this.imports.add(module);
	}

	public void removeImport(Module module) {
		this.imports.remove(module);
	}

	protected Set<Module> getImports() {
		return imports;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Module) {
			return this.name.equals(((Module) obj).getName());
		}
		return false;
	}

	OWLOntology createOntology(IRI iri, File directory) {
		OWLOntology ontology = null;
		try {
			ontology = man.createOntology(iri);
		} catch (OWLOntologyCreationException e) {
			throw new IllegalStateException("Failed to create new ontology for: " + iri, e);
		}
		man.setOntologyDocumentIRI(ontology, IRI.create(getOntologyFile(directory, iri).toURI()));
		return ontology;
	}

	File getOntologyFile(File dir, IRI iri) {
		int i = iri.toString().lastIndexOf('/');
		String fileName = iri.toString().substring(i + 1);
		return new File(dir, fileName);
	}

	public OWLDataFactory getDataFactory() {
		return man.getOWLDataFactory();
	}
	
}