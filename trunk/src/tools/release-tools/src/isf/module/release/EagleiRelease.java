package isf.module.release;

import static isf.module.ModuleNames.*;
import isf.ISFUtil;
import isf.module.CompositeModule;
import isf.module.Module;
import isf.module.SimpleModule;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class EagleiRelease extends CompositeModule {

	public static final String EAGLEI_RELEASE_MODULE_NAME = "eaglei-release";

	public EagleiRelease(String svnTrunk, boolean cleanLegacy, String outputDirectory) {
		super(EAGLEI_RELEASE_MODULE_NAME, null, svnTrunk, outputDirectory);

	}

	Module topModule = null;

	@Override
	public void generateModule() throws Exception {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology isfOntology = null;
		try {
			isfOntology = ISFUtil.setupAndLoadIsfOntology(man);
		} catch (OWLOntologyCreationException e) {
			throw new IllegalStateException("Failed to load ISF ontology due to: ", e);
		}
		OWLReasoner reasoner = ISFUtil.getDefaultReasoner(isfOntology);

		// core
		SimpleModule eaglei = new SimpleModule(EAGLEI, null, isfOntology, null,
				getOutputDirectory().getAbsolutePath());
		eaglei.setReasoner(reasoner);

		// extended and its imports
		SimpleModule eagleiExtended = new SimpleModule(EAGLEI_EXTENDED, null, isfOntology, null,
				getOutputDirectory().getAbsolutePath());
		eagleiExtended.setReasoner(reasoner);

		SimpleModule eagleiExtendedGo = new SimpleModule(EAGLEI_EXTENDED_GO, null, isfOntology,
				null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedGo.setReasoner(reasoner);

		SimpleModule eagleiExtendedMesh = new SimpleModule(EAGLEI_EXTENDED_MESH, null, isfOntology,
				null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedMesh.setReasoner(reasoner);

		SimpleModule eagleiExtendedMp = new SimpleModule(EAGLEI_EXTENDED_MP, null, isfOntology,
				null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedMp.setReasoner(reasoner);

		SimpleModule eagleiExtendedPato = new SimpleModule(EAGLEI_EXTENDED_PATO, null, isfOntology,
				null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedPato.setReasoner(reasoner);

		SimpleModule eagleiExtendedUberon = new SimpleModule(EAGLEI_EXTENDED_UBERON, null,
				isfOntology, null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedUberon.setReasoner(reasoner);

		eagleiExtended.addImport(eaglei);
		eagleiExtended.addImport(eagleiExtendedGo);
		eagleiExtended.addImport(eagleiExtendedMesh);
		eagleiExtended.addImport(eagleiExtendedMp);
		eagleiExtended.addImport(eagleiExtendedPato);
		eagleiExtended.addImport(eagleiExtendedUberon);

		// app and its import
		SimpleModule eagleiApp = new SimpleModule(EAGLEI_APP, null, isfOntology, null,
				getOutputDirectory().getAbsolutePath());
		SimpleModule eagleiAppDef = new SimpleModule(EAGLEI_APP_DEF, null, isfOntology, null,
				getOutputDirectory().getAbsolutePath());
		eagleiApp.setReasoner(reasoner);
		eagleiApp.addImport(eaglei);
		eagleiApp.addImport(eagleiAppDef);

		// app extended and its imports
		SimpleModule eagleiExtendedApp = new SimpleModule(EAGLEI_EXTENDED_APP, null, isfOntology,
				null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedApp.setReasoner(reasoner);
		SimpleModule eagleiExtendedGoApp = new SimpleModule(EAGLEI_EXTENDED_GO_APP, null,
				isfOntology, null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedGoApp.setReasoner(reasoner);
		SimpleModule eagleiExtendedMeshApp = new SimpleModule(EAGLEI_EXTENDED_MESH_APP, null,
				isfOntology, null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedMeshApp.setReasoner(reasoner);
		SimpleModule eagleiExtendedMpApp = new SimpleModule(EAGLEI_EXTENDED_MP_APP, null,
				isfOntology, null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedMpApp.setReasoner(reasoner);
		SimpleModule eagleiExtendedPatoApp = new SimpleModule(EAGLEI_EXTENDED_PATO_APP, null,
				isfOntology, null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedPatoApp.setReasoner(reasoner);
		SimpleModule eagleiExtendedUberonApp = new SimpleModule(EAGLEI_EXTENDED_UBERON_APP, null,
				isfOntology, null, getOutputDirectory().getAbsolutePath());
		eagleiExtendedUberonApp.setReasoner(reasoner);

		// the core app (which includes the core ontology)
		eagleiExtendedApp.addImport(eagleiApp);
		// the extended ontology
		eagleiExtendedApp.addImport(eagleiExtended);
		// the extended app
		eagleiExtendedApp.addImport(eagleiExtendedGoApp);
		eagleiExtendedApp.addImport(eagleiExtendedMeshApp);
		eagleiExtendedApp.addImport(eagleiExtendedMpApp);
		eagleiExtendedApp.addImport(eagleiExtendedPatoApp);
		eagleiExtendedApp.addImport(eagleiExtendedUberonApp);

		topModule = eagleiExtendedApp;
		topModule.generateModuleTransitive();

	}
	
	@Override
	public void addLegacyOntologies() {
		topModule.addLegacyOntologiesTransitive();
	}
	
	@Override
	public void cleanLegacyOntologies() {
		topModule.cleanLegacyOntologiesTransitive();
	}

	@Override
	public void close() {

	}

	@Override
	public void saveGeneratedModule() throws OWLOntologyStorageException {
		topModule.saveGeneratedModuleTransitive();

	}

	public static void main(String[] args) throws Exception {

		// the ISF checkout is found from the ISF_TRUNK or isf.trunk in this
		// example.
		// Otherwise, the first constructor argument has to be the path to trunk

		EagleiRelease release = new EagleiRelease(null, false, null);
		release.generateModule();
		release.cleanLegacyOntologies();
		release.addLegacyOntologies();
		release.saveGeneratedModule();
		release.close();

	}

}
