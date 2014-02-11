package isf.ignore;

import isf.ISFUtil;
import isf.module.Util;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class TestLoadOntology {

	public static void main(String[] args) throws OWLOntologyCreationException {
		OWLManager.createOWLOntologyManager().loadOntology(
				IRI.create(new File("/no/ontology/iri.owl").toURI()));

//		OWLManager.createOWLOntologyManager().loadOntology(IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + ""
//				+ Util.ANNOTATION_IRI_SUFFIX));

	}

}
