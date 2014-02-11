package isf.module;

import org.semanticweb.owlapi.model.OWLOntology;

public class SimpleModule extends AbstractModule {

	public SimpleModule(String moduleName, String trunkPath, String outputPath) {
		super(moduleName, trunkPath, outputPath);
	}

	public OWLOntology getMOduleOntology() {

		return null;
	}

	@Override
	public void generateModule() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}
}
