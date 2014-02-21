package isf.module;

import org.semanticweb.owlapi.model.OWLOntologyManager;

public abstract class CompositeModule extends AbstractModule {

	public CompositeModule(String moduleName,OWLOntologyManager man, String moduleTrunkRelativePath, String trunkPath, String outputDirectory) {
		super(moduleName, man, moduleTrunkRelativePath, trunkPath, outputDirectory);
		// TODO Auto-generated constructor stub
	}

}
