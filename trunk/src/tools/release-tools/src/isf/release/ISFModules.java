package isf.release;

import isf.ISFUtil;
import isf.module.internal.SimpleModuleBuilder;
import isf.release.action.Reporter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SetOntologyID;

public class ISFModules extends ReleaseBase {

	private String name;

	public ISFModules(Reporter reporter) {
		super(reporter);

	}

	@Override
	public void release() throws Exception {
		super.release();

		SimpleModuleBuilder buildModule = new SimpleModuleBuilder();
		buildModule.setReporter(reporter);
		buildModule.setModuleName(name);
		buildModule.setIsRelease(true);
		buildModule
				.setModuleDirectory(new File(ISFUtil.getTrunkDirectory(), "src/ontology/module"));
		buildModule.setIsfOntologyAndMan(isfOntology, man);
		buildModule.run();
		OWLOntologyID id = buildModule.moduleOntologyGenerated.getOntologyID();
		id = new OWLOntologyID(id.getOntologyIRI(), getVersionedIri(id.getOntologyIRI()));
		SetOntologyID setId = new SetOntologyID(buildModule.moduleOntologyGenerated, id);
		man.applyChange(setId);
		man.saveOntology(buildModule.moduleOntologyGenerated, new FileOutputStream(new File(
				RELEASE_DIR, name + "-module.owl")));

	}

	private static Set<String> getModuleNames() {

		Set<String> names = new HashSet<String>();
		for (File file : new File(ISFUtil.getTrunkDirectory(), "src/ontology/module").listFiles()) {
			if (file.getName().endsWith("-module-annotation.owl")) {
				names.add(file.getName().substring(0,
						file.getName().length() - "-module-annotation.owl".length()));
			}
		}

		return names;
	}

	void setModuleName(String name) {
		this.name = name;
	}

	private static OWLOntologyManager man;
	private static OWLOntology isfOntology;

	public static void main(String[] args) throws Exception {
		man = getManager();
		ISFUtil.setupAndLoadIsfOntology(man);
		OWLOntologyID isfId = new OWLOntologyID(ISFUtil.ISF_IRI);
		man.removeOntology(isfId);

		// we load the reasoned version but change its id temporarly so that the
		// module builder can continue to work.
		isfOntology = man
				.loadOntologyFromOntologyDocument(new File(RELEASE_DIR, "isf-reasoned.owl"));
		SetOntologyID setId = new SetOntologyID(isfOntology, ISFUtil.ISF_IRI);
		man.applyChange(setId);

		Set<String> names = getModuleNames();
		for (String name : names) {
			Reporter reporter = new Reporter(new File(RELEASE_DIR, name + "-module.owl.report.txt"));
			ISFModules module = new ISFModules(reporter);
			module.setModuleName(name);
			module.release();
			reporter.save();
		}
	}
}
