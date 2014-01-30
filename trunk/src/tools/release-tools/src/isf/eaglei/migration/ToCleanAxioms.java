package isf.eaglei.migration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * This script looks at the axioms in "mireot/toCleanAxioms.owl" and moves the
 * ones that are not in the ISF to the newAxioms.owl. For the ones that are in
 * the ISF in some other ISF files, the axiom is removed from those files and is
 * kept in the toCleanAxioms.owl file. The idea is that this script will find
 * the merged in (in the ISF) mireot axioms and removes them so that we can
 * reestablish the mireots in the ISF. The eagle-i mireot files are used to help
 * with this.
 * 
 * @author essaids
 * 
 */
public class ToCleanAxioms {

	private OWLOntologyManager man;
	private OWLOntology isfOntology;
	private OWLOntology toCleanAxioms;
	private OWLOntology newAxioms;
	private OWLOntology oldAxioms;

	private void run() throws Exception {
		man = Util.getNewManager();
		Util.prepareIsfTrunkManager(man);
		isfOntology = Util.getIsfOntology(man);
		toCleanAxioms = man.loadOntologyFromOntologyDocument(new File(
				"mireot/toCleanAxioms.owl"));
		newAxioms = man.loadOntologyFromOntologyDocument(new File(
				"mireot/newAxioms.owl"));
		oldAxioms = man.loadOntologyFromOntologyDocument(new File(
				"mireot/oldAxioms.owl"));
		Set<OWLOntology> changedOntologies = new HashSet<>();

		// in the first pass, for each entity in toCleanAxioms.owl, remove all
		// annotations in ISF files
		// Any needed annotations should be in the mireots
		// Doing this to work around the reformatting of some annotations in the
		// ISF (so they will not be found in the second pass)
		System.err.println("Inpute match string:");
		Scanner input = new Scanner(System.in);
		String iriMatch = input.nextLine();
		input.close();
		System.out.println("Ok.");

		System.out.println("Stripping all annotations from the ISF\n");
		for (OWLEntity entity : toCleanAxioms.getSignature()) {
			// only consider the entities with a specific IRI pattern to avoid
			// the supporting entities
			// if (entity.getIRI().toString().endsWith("GO_0018958")) {
			// System.out.println("Debug");
			// }
			if (entity.getIRI().toString().contains(iriMatch)) {

				if (entity.getIRI().toString().endsWith("IAO_0000114")) {
					System.out.println("Debug");
				}

				Set<OWLAnnotationAssertionAxiom> eagleiAnnotations = entity
						.getAnnotationAssertionAxioms(toCleanAxioms);

				Set<OWLAnnotationAssertionAxiom> isfAnnotations = new HashSet<OWLAnnotationAssertionAxiom>();
				for (OWLOntology o : isfOntology.getImportsClosure()) {
					isfAnnotations.addAll(entity
							.getAnnotationAssertionAxioms(o));
				}

				Set<OWLAnnotationAssertionAxiom> oldAnnotations = new HashSet<OWLAnnotationAssertionAxiom>(
						isfAnnotations);
				oldAnnotations.removeAll(eagleiAnnotations);

				Set<OWLAnnotationAssertionAxiom> newAnnotations = new HashSet<OWLAnnotationAssertionAxiom>(
						eagleiAnnotations);
				newAnnotations.removeAll(isfAnnotations);

				man.removeAxioms(toCleanAxioms, newAnnotations);
				man.addAxioms(newAxioms, newAnnotations);
				man.addAxioms(oldAxioms, oldAnnotations);

				for (OWLOntology o : isfOntology.getImportsClosure()) {

					List<OWLOntologyChange> changes = man.removeAxioms(o,
							eagleiAnnotations);
					changes.addAll(man.removeAxioms(o, oldAnnotations));
					if (changes.size() > 0) {
						changedOntologies.add(o);

					}
				}

				// for (OWLOntology o : isfOntology.getImportsClosure()) {
				// List<OWLOntologyChange> changes = man.removeAxioms(o,
				// entity.getAnnotationAssertionAxioms(o));
				// // keep track of owl files/ontologies that actually changed
				// // because of this so that we don't rewrite files
				// // unncessarly
				// if (!changes.isEmpty()) {
				// System.out.println("Processing annotations for: "
				// + o.getOntologyID());
				// for (OWLOntologyChange change : changes) {
				// if (!toCleanAxioms.containsAxiom(change.getAxiom())) {
				// man.addAxiom(oldAxioms, change.getAxiom());
				// System.out.println("Old annotation: "
				// + change.getAxiom());
				// }
				//
				// }
				//
				// changedOntologies.add(o);
				// }
				//
				// }

			}
			System.out.println();
		}

		Set<OWLDeclarationAxiom> declarations = new HashSet<>();

		System.err.println("Processing axioms\n");
		for (OWLAxiom axiom : toCleanAxioms.getAxioms()) {

			if (!(axiom instanceof OWLLogicalAxiom)) {
				continue;
			}

			System.out.println("Axiom: " + axiom);
			// for (OWLEntity e : axiom.getSignature()) {
			// if (e.getIRI().toString().endsWith("GO_0018958")) {
			// System.out.println("Debug");
			// }
			//
			// }
			// man.removeAxiom(toCleanAxioms, axiom);

			// if just a declaration in the ISF, don't remove but keep track for
			// later
			if (axiom instanceof OWLDeclarationAxiom
					&& ((OWLDeclarationAxiom) axiom).getEntity().getIRI()
							.toString().contains(iriMatch)) {
				declarations.add((OWLDeclarationAxiom) axiom);
				continue;
			}
			// remove the other types of axioms
			if (isfOntology.containsAxiom(axiom, true)) {
				for (OWLOntology o : isfOntology.getImportsClosure()) {
					List<OWLOntologyChange> changes = man.removeAxiom(o, axiom);
					if (!changes.isEmpty()) {
						changedOntologies.add(o);
						System.out
								.println("Removed from: " + o.getOntologyID());
					}
				}
			} else {
				// if the axiom was in eagle-i but not in ISF, put it in a new
				// file for later curation, and also remove it from the mireot.
				if (!oldAxioms.containsAxiom(axiom)) {
					man.addAxiom(newAxioms, axiom);
					man.removeAxiom(toCleanAxioms, axiom);
					System.out.println("New axiom.");
				}

			}
		}

		System.out.println();
		System.err
				.println("Trying to find any related axioms in the ISF that should be removed");
		// now see why we have the declarations, check each ISF import
		for (OWLDeclarationAxiom da : declarations) {
			OWLEntity entity = da.getEntity();
			if (entity.getIRI().toString().endsWith("IAO_0000114")) {
				System.out.println("Debug");
			}

			for (OWLOntology o : isfOntology.getImportsClosure()) {
				List<OWLAxiom> referringAxioms = new ArrayList<OWLAxiom>(
						o.getReferencingAxioms(entity));
				if (referringAxioms.size() == 1
						&& referringAxioms.get(0).isOfType(
								AxiomType.DECLARATION)) {
					// if the declaration is the only axiom, we don't need it in
					// the file.
					man.removeAxiom(o, da);
					changedOntologies.add(o);
					System.out.println("Removing declaration only from: "
							+ o.getOntologyID());

				} else {
					// we have other referencing axioms but we only care about
					// the defining ones.
					Set<OWLAxiom> definingAxioms = new HashSet<>();

					if (entity instanceof OWLClass) {
						definingAxioms.addAll(o.getAxioms((OWLClass) entity));
					} else if (entity instanceof OWLObjectProperty) {
						definingAxioms.addAll(o
								.getAxioms((OWLObjectProperty) entity));
					} else if (entity instanceof OWLDataProperty) {
						definingAxioms.addAll(o
								.getAxioms((OWLDataProperty) entity));
					} else if (entity instanceof OWLAnnotationProperty) {
						definingAxioms.addAll(o
								.getAxioms((OWLAnnotationProperty) entity));
					}

					// if there are remaining logical axioms that explain why
					// the
					// declaration is there they need to be removed from the iSF
					// and manually reviewed before putting them back in.
					if (definingAxioms.size() > 0) {
						changedOntologies.add(o);
						man.removeAxioms(o, definingAxioms);
						man.addAxioms(oldAxioms, definingAxioms);
						System.out.println("Ontology: " + o.getOntologyID());
						for (OWLAxiom a : definingAxioms) {
							System.out.println("Old axiom: " + a);
						}

					}
				}
			}
		}

		// i think we still need one more pass to find any unnecessary
		// declarations.
		for (OWLOntology o : isfOntology.getImportsClosure()) {

			for (OWLDeclarationAxiom da : declarations) {

				OWLEntity e = da.getEntity();

				List<OWLAxiom> axioms = new ArrayList<OWLAxiom>(
						e.getReferencingAxioms(o));

				if (axioms.size() == 1
						&& axioms.get(0) instanceof OWLDeclarationAxiom) {
					man.removeAxiom(o, axioms.get(0));
					changedOntologies.add(o);
				}
			}

		}

		for (OWLOntology o : changedOntologies) {
			man.saveOntology(o);
		}
		man.saveOntology(newAxioms);
		man.saveOntology(oldAxioms);
		man.saveOntology(toCleanAxioms);
	}

	public static void main(String[] args) throws Exception {
		new ToCleanAxioms().run();

	}

}
