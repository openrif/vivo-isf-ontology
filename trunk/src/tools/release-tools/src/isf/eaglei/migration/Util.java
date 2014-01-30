package isf.eaglei.migration;

import isf.ISFUtil;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public class Util {

	public static OWLOntologyManager getNewManager() {
		return OWLManager.createOWLOntologyManager();
	}

	public static void prepareEagleiTrunkManager(OWLOntologyManager man) {
		man.clearIRIMappers();

		String trunkPath = Constants.getTrunkPath();
		if (trunkPath == null) {
			throw new IllegalStateException("ERO_SVN_TRUNK_PATH not set");
		}
		AutoIRIMapper mapper = new AutoIRIMapper(new File(trunkPath), true);

		man.addIRIMapper(mapper);
	}

	public static void prepareIsfTrunkManager(OWLOntologyManager man) {
		man.clearIRIMappers();
		// AutoIRIMapper mapper = new AutoIRIMapper(new File(
		// Constants.ISF_ROOT, "release"), true);
		// man.addIRIMapper(mapper);
//		String trunkPath = System.getenv("ISF_SVN_TRUNK_PATH");
		File svnRoot = ISFUtil.getSvnRootDir();
		if (svnRoot == null) {
			throw new IllegalStateException("ISF_SVN_TRUNK_PATH not set");
		}
		AutoIRIMapper mapper = new AutoIRIMapper(new File(svnRoot, "trunk/src/ontology"), true);
		man.addIRIMapper(mapper);
	}

	public static OWLOntology getEagleiExtendedTrunk(OWLOntologyManager man)
			throws OWLOntologyCreationException {

		OWLOntology eagleio =  man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/ero/ero-extended.owl"));
		
		for(OWLOntology o: man.getOntologies()){
			System.out.println(man.getOntologyDocumentIRI(o));
		}
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return eagleio;
	}

	// public static OWLOntology getEagleiReleaseUnreasoned(OWLOntologyManager
	// man)
	// throws Exception {
	//
	// OWLOntology ontology = man
	// .loadOntologyFromOntologyDocument(new File(
	// Constants.EAGLEI_ROOT,
	// Constants.EAGLEI_RELEASE_UNREASONED_PATH));
	// return ontology;
	// }

	public static OWLOntology getIsfOntology(OWLOntologyManager man)
			throws Exception {

		OWLOntology ontology = man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/arg/isf.owl"));
		return ontology;
	}

	// public static OWLOntology
	// getEagleiReleaseUnreasonedLessIsfReleaseUnreasoned()
	// throws Exception {
	// OWLOntologyManager man = getNewManager();
	// Set<OWLAxiom> axioms = getEagleiReleaseUnreasoned(man).getAxioms();
	//
	// axioms.removeAll(getIsfReleaseUnreasoned(man).getAxioms());
	//
	// return man.createOntology(axioms);
	// }

	public static OWLOntology getIgnoreOntology(OWLOntologyManager isfManager)
			throws OWLOntologyCreationException {
		return isfManager.loadOntologyFromOntologyDocument(new File(
				"ignoreOntology.owl"));
	}

}
