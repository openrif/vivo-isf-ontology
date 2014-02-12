package isf.module;

import isf.ISFUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
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
	private OWLOntologyManager man = new OWLManager().buildOWLOntologyManager();

	public OWLOntologyManager getManager() {
		return man;
	}

	public AbstractModule(String moduleName, String moduleTrunkRelativePath, String trunkPath,
			String outputDirectory) {
		if (moduleName == null) {
			throw new IllegalStateException("Module name cannot be null.");
		}
		this.name = moduleName;

		// trunk path is either passed in, or looked up in ISFUtil when needed
		// (see the static block in ISFUtil)
		if (trunkPath != null) {
			try {
				ISFUtil.setISFTrunkDirecotry(new File(trunkPath).getCanonicalFile());
			} catch (IOException e) {
				throw new IllegalStateException("Failed to set canonical trunk directory.", e);
			}
		}

		if (moduleTrunkRelativePath != null) {
			this.directory = new File(ISFUtil.getTrunkDirectory(), moduleTrunkRelativePath);
		} else {
			this.directory = new File(ISFUtil.getDefaultModuleDirectory(), this.name);
		}
		this.directory.mkdirs();

		if (outputDirectory != null) {
			try {
				this.outputDirectory = new File(outputDirectory).getCanonicalFile();
			} catch (IOException e) {
				throw new IllegalStateException("Failed to get canonical output directory.", e);
			}

		} else {
			this.outputDirectory = new File(ISFUtil.getGeneratedDirectory(), "module/" + name);
		}
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

	/**
	 * Not client API
	 * 
	 * @param phase
	 */
	@Deprecated
	public void builderPhase(String phase) {
		System.out.println(phase);
	}

	/**
	 * Not client API
	 * 
	 * @param phase
	 */
	@Deprecated
	public void builderMessage(String message) {
		System.out.println(message);
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

	public void remoteImport(Module module) {
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