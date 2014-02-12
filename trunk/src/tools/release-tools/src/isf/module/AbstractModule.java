package isf.module;

import isf.ISFUtil;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public abstract class AbstractModule implements Module {

	private OWLReasoner reasoner;
	private String name;
	private File directory;
	private File outputDirectory;

	public AbstractModule(String moduleName, String moduleTrunkRelativePath, String trunkPath,
			String outputDirectory) {
		if (moduleName == null) {
			throw new IllegalStateException("Module name cannot be null.");
		}
		this.name = moduleName;
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
				throw new IllegalStateException("Failed to get canonical output directory.");
			}

		} else {
			this.outputDirectory = new File(ISFUtil.getGeneratedDirectory(), "module/" + name);
		}
		this.outputDirectory.mkdirs();
		if (trunkPath != null) {
			try {
				ISFUtil.setISFTrunkDirecotry(new File(trunkPath).getCanonicalFile());
			} catch (IOException e) {
				throw new IllegalStateException("Failed to get canonical trunk directory.");
			}
		}
	}

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

	public abstract void generateModule() throws Exception;

	public abstract void saveGeneratedModule() throws OWLOntologyStorageException;

	public abstract void close();

	public abstract void saveModuleDefinitionFiles() throws OWLOntologyStorageException;

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}
}