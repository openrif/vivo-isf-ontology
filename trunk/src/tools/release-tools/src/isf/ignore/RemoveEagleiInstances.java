package isf.ignore;

import isf.ISFUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class RemoveEagleiInstances {

	public static void main(String[] args) throws OWLOntologyCreationException,
			OWLOntologyStorageException {
		new RemoveEagleiInstances().run();

	}

	private void run() throws OWLOntologyCreationException, OWLOntologyStorageException {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology isfOntology = ISFUtil.setupAndLoadIsfFullOntology(man);
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		Set<OWLOntology> changed = new HashSet<OWLOntology>();

		for (OWLOntology o : isfOntology.getImportsClosure()) {
			Set<OWLEntity> entities = o.getSignature();
			for (OWLEntity entity : entities) {
				if (entity.getIRI().toString().startsWith("http://global.eagle-i.net/i/")) {

					List<OWLOntologyChange> c = man.removeAxioms(o, o.getReferencingAxioms(entity));
					if (c.size() > 0) {
						changed.add(o);
						changes.addAll(c);
					}
				}
			}
		}

		for (OWLOntologyChange change : changes) {
			System.out.println(change);
		}

		for (OWLOntology o : changed) {
			man.saveOntology(o);
		}

	}

}
