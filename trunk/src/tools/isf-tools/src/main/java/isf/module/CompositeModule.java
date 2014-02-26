package isf.module;

import java.io.File;

import org.semanticweb.owlapi.model.OWLOntologyManager;

public abstract class CompositeModule extends AbstractModule {

	public CompositeModule(String moduleName, OWLOntologyManager man, File directory,
			File outputDirectory) {
		super(moduleName, man, directory, outputDirectory);
		// TODO Auto-generated constructor stub
	}

}
