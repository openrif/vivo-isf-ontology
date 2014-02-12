package isf.module;

import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class EagleiModule extends CompositeModule {

	public static final String EAGLEI_MODULE_NAME = "eaglei-module";

	public EagleiModule(String svnTrunk, String outputDirectory) {
		super(EAGLEI_MODULE_NAME, svnTrunk, outputDirectory);
	}

	@Override
	public void generateModule() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveGeneratedModule() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveModuleDefinitionFiles() throws OWLOntologyStorageException {
		// TODO Auto-generated method stub
		
	}

}
