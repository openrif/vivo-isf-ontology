package isf.module;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.reasoner.OWLReasoner;

public abstract class AbstractModule {

	private OWLReasoner reasoner;
	private String name;
	private String trunkPath;
	private File outputDirectory;

	public AbstractModule(String moduleName, String trunkPath, String outputDirectory) {
		this.name = moduleName;
		try {
			this.outputDirectory = new File(outputDirectory).getCanonicalFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.trunkPath = trunkPath;
	}

	public String getName() {
		return name;
	}

	public String geTrunkPath() {
		return trunkPath;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public OWLReasoner getReasoner() {

		return null;
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

	public abstract void generateModule();

	public abstract void close();
}