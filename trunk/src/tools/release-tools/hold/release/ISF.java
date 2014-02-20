package isf.release;

import isf.ISFUtil;
import isf.release.action.DuplicateLabelCheck;
import isf.release.action.OneDefinitionCheck;
import isf.release.action.OneLabelCheck;
import isf.release.action.Reporter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;

import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SetOntologyID;

public class ISF extends ReleaseBase {

	public ISF(Reporter reporter) {
		super(reporter);
	}

	@Override
	public void release() throws Exception {
		reporter.setHeading("Merging ISF files.");
		mergeIsf();
		reporter.setManaagerForRenderer(mergeMan);

		// label check
		OneLabelCheck labelCheck = new OneLabelCheck();
		labelCheck.doAction(mergedIsfOntology, mergeMan, reporter);

		// duplicate labels check
		DuplicateLabelCheck dl = new DuplicateLabelCheck();
		dl.doAction(mergedIsfOntology, mergeMan, reporter);

		// definitions check
		OneDefinitionCheck defCheck = new OneDefinitionCheck();
		defCheck.doAction(mergedIsfOntology, mergeMan, reporter);

		OWLOntologyID id = new OWLOntologyID(ISFUtil.ISF_IRI, getVersionedIri(ISFUtil.ISF_IRI));

		SetOntologyID ontologyId = new SetOntologyID(mergedIsfOntology, id);

		mergeMan.applyChange(ontologyId);

		mergeMan.saveOntology(mergedIsfOntology, new FileOutputStream(new File(RELEASE_DIR,
				"isf.owl")));
	}

	OWLOntologyManager man;
	OWLOntologyManager mergeMan;
	OWLOntology isfOntology;
	OWLOntology mergedIsfOntology;

	// OWLOntology includeOntology;
	// OWLOntology excludeOntology;

	void mergeIsf() throws OWLOntologyCreationException {
		man = getManager();
		mergeMan = getManager();

		ISFUtil.setupAndLoadIsfOntology(man);
		isfOntology = man.getOntology(ISFUtil.ISF_IRI);
		mergedIsfOntology = mergeMan.createOntology(ISFUtil.ISF_IRI);
		// includeOntology = man.getOntology(ISFUtil.ISF_INCLUDE_IRI);
		// excludeOntology = man.getOntology(ISFUtil.ISF_EXCLUDE_IRI);

		for (OWLOntology o : isfOntology.getImportsClosure()) {
			reporter.addLine("Merging: " + o.getOntologyID().getOntologyIRI());
			Set<OWLAxiom> axioms = o.getAxioms();
			for (OWLAxiom axiom : axioms) {
				// if (!skipAxiom(axiom)) {
				// mergeMan.addAxiom(mergedIsfOntology, axiom);
				// }
			}
		}

		// mergeMan.addAxioms(mergedIsfOntology, includeOntology.getAxioms());

		for (OWLAnnotation annotation : isfOntology.getAnnotations()) {
			AddOntologyAnnotation aa = new AddOntologyAnnotation(mergedIsfOntology, annotation);
			mergeMan.applyChange(aa);
		}

	}

	// private boolean skipAxiom(OWLAxiom axiom) {
	// if (excludeOntology.containsAxiom(axiom)) {
	// reporter.addLine("\tSkipping: " + axiom);
	// return true;
	// }
	// return false;
	// }

	public static void main(String[] args) throws Exception {

		Reporter reporter = new Reporter(new File(RELEASE_DIR, "isf.owl.report.txt"));
		new ISF(reporter).release();
		reporter.save();
	}

}
