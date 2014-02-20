package isf.release.action;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Action {

	OWLOntologyManager man;
	Reporter reporter;
	OWLOntology ontology;

	public void doAction(OWLOntology ontology, OWLOntologyManager man, Reporter reporter) {
		this.ontology = ontology;
		this.man = man;
		this.reporter = reporter;
	}

}
