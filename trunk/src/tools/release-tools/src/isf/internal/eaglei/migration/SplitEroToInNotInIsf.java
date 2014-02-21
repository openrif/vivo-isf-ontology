package isf.internal.eaglei.migration;

import isf.ISFUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * A commented script to show a simple processing of axioms to in/not in ISF for
 * the ERO ontology.
 * 
 * @author essaids
 * 
 */
public class SplitEroToInNotInIsf {

	public static void main(String[] args) throws OWLOntologyCreationException,
			OWLOntologyStorageException, FileNotFoundException {
		// A java program has to start with a "main" method like this but
		// usually this method only creates an instanced of some other class and
		// then hands control to it.

		new SplitEroToInNotInIsf().run();

	}

	// the path to the ISF/ERO checkout. Change this when needed.

	static String isfTrunkPath = "/srv/pass-through/git/googlecode/isf-svn/trunk";
	static String eroTrunkPath = "/srv/pass-through/git/googlecode/eaglei-clean-trunk";

	private void run() throws OWLOntologyCreationException, OWLOntologyStorageException,
			FileNotFoundException {

		// We need an ontology manager to work with ontology objects. We can
		// work with one manager if we are sure that there is no problematic
		// overlap (one ontology used in several places but we only want to
		// change it for one of the imports, not the other) in the imports of
		// the ISF/ERO but to be sure we'll use two managers
		OWLOntologyManager isfManager = OWLManager.createOWLOntologyManager();
		OWLOntologyManager eroManager = OWLManager.createOWLOntologyManager();

		// the default behavior for the manager is to resolve imports according
		// to the ontology URL (online) but we want to find imports in our
		// checkout so we override this behavior with a file based mapper that
		// looks into all subdirectories to find ontologies.

		AutoIRIMapper isfMapper = new AutoIRIMapper(new File(isfTrunkPath, "src/ontology"), true);
		isfManager.clearIRIMappers();
		isfManager.addIRIMapper(isfMapper);

		AutoIRIMapper eroMapper = new AutoIRIMapper(new File(eroTrunkPath, "src/ontology"), true);
		eroManager.clearIRIMappers();
		eroManager.addIRIMapper(eroMapper);

		// Load the ISF/ERO ontologies

		OWLOntology isfOntology = isfManager.loadOntology(ISFUtil.ISF_DEV_IRI);
		// OWLOntology eroOntology = eroManager.loadOntology(IRI
		// .create("http://purl.obolibrary.org/obo/ero.owl"));
		OWLOntology eroOntology = eroManager.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/ero.owl"));

		// we need to collect all axioms from all imports so we have a simple
		// helper method that does this
		Set<OWLAxiom> isfAxioms = getAllAxioms(isfOntology);
		Set<OWLAxiom> eroAxioms = getAllAxioms(eroOntology);

		// now we remove/subtract axioms from the sets as needed. thsi doesn't
		// change the ontologies, the sets are just copies of the axioms.

		eroAxioms.removeAll(isfAxioms);
		// now the eroAxioms set has all the axioms that are not in the ISF.
		// We'll assign this set to a new variable that reflects this but this
		// is not necessary.
		Set<OWLAxiom> notInIsfAxioms = eroAxioms;

		// now we reload the original ero axioms
		eroAxioms = getAllAxioms(eroOntology);
		eroAxioms.removeAll(notInIsfAxioms);

		// now we have the rest of the axioms (the ones already in the ISF).
		Set<OWLAxiom> inIsfAxioms = eroAxioms;

		// we now create two ontologies to hold the split axioms and we need to
		// use a manager to do that, either managers will do

		RDFXMLOntologyFormat of = new RDFXMLOntologyFormat();
		of.setAddMissingTypes(true);

		OWLOntology notInIsfOntology = isfManager.createOntology(IRI
				.create("http://temp/not-in-isf.owl"));
		isfManager.addAxioms(notInIsfOntology, notInIsfAxioms);
		isfManager.saveOntology(notInIsfOntology, of, new FileOutputStream("notInIsfAxioms.owl"));

		OWLOntology inIsfOntology = isfManager.createOntology(IRI.create("http://temp/in-isf.owl"));
		isfManager.addAxioms(inIsfOntology, inIsfAxioms);
		isfManager.saveOntology(inIsfOntology, of, new FileOutputStream("inIsfAxioms.owl"));

	}

	public static Set<OWLAxiom> getAllAxioms(OWLOntology isfOntology) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		// one by one get and add the axioms
		for (OWLOntology o : isfOntology.getImportsClosure()) {
			axioms.addAll(o.getAxioms());
		}
		return axioms;
	}

}
