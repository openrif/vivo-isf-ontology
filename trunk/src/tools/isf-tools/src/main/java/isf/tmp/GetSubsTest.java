package isf.tmp;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import isf.ISFUtil;

public class GetSubsTest {

	public static void main(String[] args) throws OWLOntologyCreationException {
		// TODO Auto-generated method stub

		OWLOntologyManager man = ISFUtil.getIsfManagerSingleton();
		OWLOntology isf = ISFUtil.setupAndLoadIsfOntology(man);
		OWLReasoner r = ISFUtil.getDefaultReasoner(isf);
		OWLClass c = man.getOWLDataFactory().getOWLClass(IRI.create("http://purl.obolibrary.org/obo/ERO_0000285"));
		NodeSet<OWLClass> cs = r.getSubClasses(c, false);
		System.out.println(cs.getFlattened());
	}

}
