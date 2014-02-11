package isf.module;

import org.semanticweb.owlapi.reasoner.OWLReasoner;

public abstract class AbstractModule {

	private OWLReasoner reasoner;
	private String name;
	private String outputPath;
	private String trunkPath;

	public AbstractModule(String moduleName, String trunkPath, String outputDirectory) {
		this.name = moduleName;
		this.outputPath = outputDirectory;
		this.trunkPath = trunkPath;
	}

	public String geTrunkPath() {
		return trunkPath;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public OWLReasoner getReasoner() {

		return null;
	}

	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public abstract void generateModule();

	public abstract void close();
}