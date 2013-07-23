package isf.misc;

import isf.ISFUtil;

import java.io.FileOutputStream;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class MergeISFTon3 {

	public static void main(String[] args) throws Exception {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		ISFUtil.setupAndLoadIsfOntology(man);
		OWLOntology isfOntology = man.getOntology(ISFUtil.ISF_IRI);
		
		OWLOntology merged = man.createOntology();
		for(OWLOntology o : isfOntology.getImportsClosure()){
			man.addAxioms(merged, o.getAxioms());
		}
		
		TurtleOntologyFormat tf = new TurtleOntologyFormat();
		tf.setAddMissingTypes(false);
		
		man.saveOntology(merged, tf, new FileOutputStream("turtle.n3"));
	}

}
