package isf.release;

import isf.ISFUtil;
import isf.release.action.Reporter;

import java.io.File;
import java.io.FileOutputStream;

import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

public class ISFReasoned extends ReleaseBase {

	public ISFReasoned(Reporter reporter) {
		super(reporter);
	}

	@Override
	public void release() throws Exception {

		OWLOntologyManager man = getManager();
		OWLOntology mergedIsfOntology = man.loadOntologyFromOntologyDocument(new File(RELEASE_DIR,
				"isf.owl"));
		OWLOntology inferredAxiomsOntology = man.createOntology();

		ReasonerFactory rf = new ReasonerFactory();
		OWLReasoner r = rf.createReasoner(mergedIsfOntology);

		InferredEquivalentClassAxiomGenerator eqg = new InferredEquivalentClassAxiomGenerator();
		man.addAxioms(inferredAxiomsOntology, eqg.createAxioms(man, r));

		InferredSubClassAxiomGenerator subg = new InferredSubClassAxiomGenerator();
		man.addAxioms(inferredAxiomsOntology, subg.createAxioms(man, r));

		reporter.setHeading("Generating reasoned ISF:");

		for (OWLAxiom axiom : inferredAxiomsOntology.getAxioms()) {
			if (!mergedIsfOntology.containsAxiom(axiom)) {
				reporter.addLine("Adding: " + axiom.toString());
			}
		}

		man.addAxioms(mergedIsfOntology, inferredAxiomsOntology.getAxioms());

		OWLOntologyID id = new OWLOntologyID(ISFUtil.ISF_REASONED_IRI,
				getVersionedIri(ISFUtil.ISF_REASONED_IRI));

		SetOntologyID ontologyId = new SetOntologyID(mergedIsfOntology, id);

		man.applyChange(ontologyId);

		man.saveOntology(mergedIsfOntology, new FileOutputStream(new File(RELEASE_DIR,
				"isf-reasoned.owl")));

	}

	public static void main(String[] args) throws Exception {

		Reporter reporter = new Reporter(new File(RELEASE_DIR, "isf-reasoned.owl.report.txt"));
		new ISFReasoned(reporter).release();
		reporter.save();
	}

}
