package isf.internal.eaglei.migration;

import isf.ISFUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * This script packages the ero modules for delivery to the ero svn or eagle-i
 * team. It is commented to understand the logic and to give enough explanation
 * to understand the code.
 * 
 * @author essaids
 * 
 */
public class PackageEroModules {

	public static void main(String[] args) throws OWLOntologyCreationException,
			OWLOntologyStorageException, FileNotFoundException {
		new PackageEroModules().run();

	}

	private void run() throws OWLOntologyCreationException, OWLOntologyStorageException,
			FileNotFoundException {

		// The following is the workflow we will use until we cleanup the
		// "hacky" ero module approach.

		// We have two files in the module directory (*-include-in-isf.owl and
		// *-include-not-in-isf.owl) for each of the "core" and "extended" ERO
		// versions. (There are also "app" versions of these files/modules.)
		// These
		// two files have the content of the corresponding ERO
		// files in the ERO SVN. Until these files are reviewed, emptied, and
		// then deleted, the following is the recommended workflow:

		// We first run the module generation script to generate the modules the
		// ISF way. The generated modules will only have ISF based axioms
		// according to the annotation file and any manual excludes and
		// includes.

		// We then check the *-include-not-in-isf.owl file against the ISF to
		// find
		// any axioms that now match ISF axioms (due to manual curation ,iri
		// mapping, etc.) and move these axioms to the *-include-in-isf.owl
		// because they are now in the ISF.

		// We then check the *-include-in-isf.owl file for axioms that are now
		// being added to the module by the proper ISF module generation
		// process. i.e. there are annotations that are bringing in the axioms
		// from the ISF into the module. These axioms can now be removed from
		// the *-include-in-isf.owl

		// the gradual removal of axioms from *-include-not-in-isf.owl and
		// *-include-in-isf.owl will shrink these files until everything is
		// either matched or added with the ISF axioms. For any remaining
		// axioms, they will go into the proper *-module-include.owl files so
		// that they are added during the proper module generation and this
		// script will not be needed anymore.

		// we then create the ontologies with original ERO IRIs and save them in
		// identical folders so that we can copy the top folder to the ERO SVN.

		File rootDirectory = new File("ero-package");
		rootDirectory.renameTo(new File("ero-package-old"));

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();

		AutoIRIMapper mapper = new AutoIRIMapper(new File(ISFUtil.getTrunkDirectory(),
				"src/ontology"), true);
		man.clearIRIMappers();
		man.addIRIMapper(mapper);

		OWLOntology isfOntology = man.loadOntology(ISFUtil.ISF_DEV_IRI);

		OWLOntology notInIsfOntology = man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/arg/eaglei-module-include-not-in-isf.owl"));
		OWLOntology inIsfOntology = man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/arg/eaglei-module-include-in-isf.owl"));

		OWLOntology notInIsfExtendedOntology = man
				.loadOntology(IRI
						.create("http://purl.obolibrary.org/obo/arg/eaglei-extended-module-include-not-in-isf.owl"));
		OWLOntology inIsfExtendedOntology = man
				.loadOntology(IRI
						.create("http://purl.obolibrary.org/obo/arg/eaglei-extended-module-include-in-isf.owl"));

		OWLOntology eroModuleOntology = man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/arg/eaglei-module.owl"));
		OWLOntology eroExtendedModuleOntology = man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/arg/eaglei-extended-module.owl"));

		OWLOntology eroAppModuleOntology = man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/arg/eaglei-app-module.owl"));
		OWLOntology eroExtendedAppModuleOntology = man.loadOntology(IRI
				.create("http://purl.obolibrary.org/obo/arg/eaglei-app-extended-module.owl"));

		// remove axioms from not-in-isf to in-isf
		Set<OWLAxiom> axioms = SplitEroToInNotInIsf.getAllAxioms(isfOntology);
		// the following is the intersection of ISF and notInIsfOntology, the
		// axioms that are in both.
		axioms.retainAll(notInIsfOntology.getAxioms());
		man.removeAxioms(notInIsfOntology, axioms);
		man.saveOntology(notInIsfOntology); // we are finished changing this
											// ontology

		man.addAxioms(inIsfOntology, axioms);
		axioms = eroModuleOntology.getAxioms();
		axioms.retainAll(inIsfOntology.getAxioms());
		man.removeAxioms(inIsfOntology, axioms);
		man.saveOntology(inIsfOntology); // we are finished changing this
											// ontology

		// do the same for the extended files
		axioms = SplitEroToInNotInIsf.getAllAxioms(isfOntology);
		axioms.retainAll(notInIsfExtendedOntology.getAxioms());
		man.removeAxioms(notInIsfExtendedOntology, axioms);
		man.saveOntology(notInIsfExtendedOntology); // we are finished changing
													// this ontology

		man.addAxioms(inIsfExtendedOntology, axioms);
		axioms = eroExtendedModuleOntology.getAxioms();
		axioms.retainAll(inIsfExtendedOntology.getAxioms());
		man.removeAxioms(inIsfExtendedOntology, axioms);
		man.saveOntology(inIsfExtendedOntology); // we are finished changing
													// this ontology

		// create and setup the ontologies we want to save as final packaged
		// files.

		// save the files in the proper directory layout
		File dirs = new File("ero-package/src/ontology/core");
		dirs.mkdirs();
		dirs = new File("ero-package/src/ontology/application-specific-files");
		dirs.mkdir();

		// the ero.owl file
		OWLOntology eroPackagedOntology = man.createOntology(IRI
				.create("http://purl.obolibrary.org/obo/ero.owl"));
		man.addAxioms(eroPackagedOntology, eroModuleOntology.getAxioms());
		man.addAxioms(eroPackagedOntology, inIsfOntology.getAxioms());
		man.addAxioms(eroPackagedOntology, notInIsfOntology.getAxioms());
		man.saveOntology(eroPackagedOntology, new FileOutputStream(
				"ero-package/src/ontology/core/ero.owl"));

		// the ero-extended.owl file
		OWLOntology eroExtendedPackagedOntology = man.createOntology(IRI
				.create("http://purl.obolibrary.org/obo/ero/ero-extended.owl"));
		AddImport ontologyImport = new AddImport(eroExtendedPackagedOntology,
				df.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/ero.owl")));
		man.applyChange(ontologyImport);
		man.addAxioms(eroExtendedPackagedOntology, eroExtendedModuleOntology.getAxioms());
		man.addAxioms(eroExtendedPackagedOntology, inIsfExtendedOntology.getAxioms());
		man.addAxioms(eroExtendedPackagedOntology, notInIsfExtendedOntology.getAxioms());
		man.saveOntology(eroExtendedPackagedOntology, new FileOutputStream(
				"ero-package/src/ontology/core/ero-extended.owl"));

		// ero app file
		OWLOntology eroAppPackagedOntology = man.createOntology(IRI
				.create("http://eagle-i.org/ont/app/1.0/eagle-i-core-app.owl"));
		ontologyImport = new AddImport(eroAppPackagedOntology, df.getOWLImportsDeclaration(IRI
				.create("http://purl.obolibrary.org/obo/ero.owl")));
		man.applyChange(ontologyImport);
		man.addAxioms(eroAppPackagedOntology, eroAppModuleOntology.getAxioms());
		man.saveOntology(eroAppPackagedOntology, new FileOutputStream(
				"ero-package/src/ontology/application-specific-files/eagle-i-core-app.owl"));

		// ero app extended file
		OWLOntology eroExtendedAppPackagedOntology = man.createOntology(IRI
				.create("http://eagle-i.org/ont/app/1.0/eagle-i-extended-app.owl"));
		ontologyImport = new AddImport(eroExtendedAppPackagedOntology,
				df.getOWLImportsDeclaration(IRI
						.create("http://eagle-i.org/ont/app/1.0/eagle-i-core-app.owl")));
		man.applyChange(ontologyImport);
		ontologyImport = new AddImport(eroExtendedAppPackagedOntology,
				df.getOWLImportsDeclaration(IRI
						.create("http://purl.obolibrary.org/obo/ero/ero-extended.owl")));
		man.applyChange(ontologyImport);
		man.addAxioms(eroExtendedAppPackagedOntology, eroExtendedAppModuleOntology.getAxioms());

		man.saveOntology(eroExtendedAppPackagedOntology, new FileOutputStream(
				"ero-package/src/ontology/application-specific-files/eagle-i-extended-app.owl"));
	}

}
