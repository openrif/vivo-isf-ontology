package isf.release;

import isf.ISFUtil;
import isf.release.action.Reporter;

import java.io.File;
import java.io.FileOutputStream;

import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class ISFSkos extends ReleaseBase {

	public ISFSkos(Reporter reporter) throws Exception {
		super(reporter);

	}

	public void release() throws Exception {
		super.release();
		OWLOntologyManager man = getManager();
		ISFUtil.setupManager(man);
		OWLOntology isfSkosOntology = man.loadOntology(ISFUtil.ISF_SKOS_IRI);
		OWLOntologyManager mergeMan = getManager();

		OWLOntology isfSkosMerged = mergeMan.createOntology(new OWLOntologyID(
				ISFUtil.ISF_SKOS_IRI, getVersionedIri(ISFUtil.ISF_SKOS_IRI)));

		for (OWLOntology ontology : isfSkosOntology.getImportsClosure()) {
			mergeMan.addAxioms(isfSkosMerged, ontology.getAxioms());
		}

		for (OWLAnnotation annotation : isfSkosOntology.getAnnotations()) {
			AddOntologyAnnotation aa = new AddOntologyAnnotation(isfSkosMerged,
					annotation);
			mergeMan.applyChange(aa);
		}

		mergeMan.saveOntology(isfSkosMerged, new FileOutputStream(new File(
				RELEASE_DIR, "isf-skos.owl")));
	}

	public static void main(String[] args) throws Exception {
		Reporter reporter = new Reporter(new File(RELEASE_DIR,
				"isf-skos.owl.report.txt"));
		reporter.setHeading("Merging isf-skos.owl");
		ISFSkos isfSkos = new ISFSkos(reporter);
		isfSkos.release();
		reporter.save();

	}

}
