package isf.module;

import isf.ISFUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class BuildModule {

	private OWLOntologyManager isfFullMan = OWLManager
			.createOWLOntologyManager();
	private OWLDataFactory df = isfFullMan.getOWLDataFactory();
	private OWLOntology isfFullOntology;

	private OWLOntology moduleOntology;
	private OWLOntology moduleOntologyInclude;
	private OWLOntology moduleOntologyExclude;
	private OWLOntology moduleOntologyGenerated;
	private String[] args;
	private Set<OWLOntology> changedOntologies = new HashSet<OWLOntology>();

	private void run(String[] args) throws OWLOntologyCreationException,
			OWLOntologyStorageException {
		this.args = args;
		inti();

		addIncludes();
		addIncludeSubs();

		removeExcludes();
		removeExcludeSubs();

		mergeModuleInclude();

		addClosureToBfo();

		addAnnotations();
		typeAllEntities();

		save();
	}

	public void addIncludes() {
		Set<OWLEntity> entities = ModuleUtil.getIncludeEntities(moduleOntology,
				true);

		for (OWLEntity e : entities) {
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, isfFullOntology, true));
		}

	}

	public void addIncludeSubs() {
		Set<OWLEntity> entities = ModuleUtil.getIncludeSubsEntities(
				moduleOntology, true);
		Set<OWLEntity> closureEntities = new HashSet<OWLEntity>();

		for (OWLEntity e : entities) {
			closureEntities.addAll(ISFUtil.getSubsClosure(e, isfFullOntology,
					true));
		}
		for (OWLEntity e : closureEntities) {
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, isfFullOntology, true));
		}
	}

	public void removeExcludeSubs() {
		// TODO Auto-generated method stub

	}

	public void removeExcludes() {
		// TODO Auto-generated method stub

	}

	public void mergeModuleInclude() {
		addAxioms(moduleOntologyInclude.getAxioms());
	}

	public void addClosureToBfo() {
		// TODO Auto-generated method stub

	}

	public void addAnnotations() {
		Set<OWLEntity> entitiesToAnnotate = new HashSet<OWLEntity>();
		entitiesToAnnotate.addAll(moduleOntologyGenerated.getSignature());

		Set<OWLEntity> annotatedEntities = new HashSet<OWLEntity>();

		while (entitiesToAnnotate.size() > 0) {
			Set<OWLEntity> newEntities = new HashSet<OWLEntity>();
			Iterator<OWLEntity> i = entitiesToAnnotate.iterator();
			while (i.hasNext()) {
				OWLEntity entity = i.next();
				i.remove();
				annotatedEntities.add(entity);
				Set<OWLAnnotationAssertionAxiom> axioms = ISFUtil
						.getAnnotationAxioms(isfFullOntology, true,
								entity.getIRI());
				addAxioms(axioms);
				for (OWLAnnotationAssertionAxiom a : axioms) {
					Set<OWLEntity> signature = a.getSignature();
					signature.removeAll(annotatedEntities);
					newEntities.addAll(signature);
				}
			}

			entitiesToAnnotate.addAll(newEntities);
		}

	}

	public void typeAllEntities() {
		for (OWLEntity e : moduleOntologyGenerated.getSignature()) {
			addAxiom(df.getOWLDeclarationAxiom(e));
		}

	}

	private void addAxioms(Set<? extends OWLAxiom> axioms) {
		for (OWLAxiom axiom : axioms) {
			addAxiom(axiom);
		}
	}

	private void addAxiom(OWLAxiom axiom) {
		if (!moduleOntologyExclude.containsAxiom(axiom)
				&& !moduleOntologyInclude.containsAxiom(axiom)) {
			isfFullMan.addAxiom(moduleOntologyGenerated, axiom);
		}
	}

	private void save() throws OWLOntologyStorageException {
		RDFXMLOntologyFormat format = new RDFXMLOntologyFormat();
		format.setAddMissingTypes(true);
		for (OWLOntology ontology : changedOntologies) {
			System.out.println("Saving ontology: " + ontology.getOntologyID());
			isfFullMan.saveOntology(ontology, format);
		}

	}

	private void inti() throws OWLOntologyCreationException {
		isfFullOntology = ISFUtil.setupAndLoadIsfFullOntology(isfFullMan);
		// System.out.println(isfFullMan.getOntologies().size());
		// for(OWLOntology o : isfFullMan.getOntologies()){
		// System.out.println(o.getOntologyID() +
		// " -> "+isfFullMan.getOntologyDocumentIRI(o));
		// }
		String moduleName = args[0];
		// load module ontology
		IRI moduleIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + moduleName
				+ "-module.owl");
		moduleOntology = getLoadCreateOntology(moduleIri);

		IRI moduleIncludeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
				+ moduleName + "-module-include.owl");
		moduleOntologyInclude = getLoadCreateOntology(moduleIncludeIri);

		IRI moduleExcludeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
				+ moduleName + "-module-exclude.owl");
		moduleOntologyExclude = getLoadCreateOntology(moduleExcludeIri);

		// always create a new one and save it to the local folder
		IRI moduleGeneratedIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX
				+ moduleName + "-module-generated.owl");
		moduleOntologyGenerated = createOntology(moduleGeneratedIri);
		isfFullMan.setOntologyDocumentIRI(
				moduleOntologyGenerated,
				IRI.create(getDocumentFile(
						new File(ISFUtil.getSvnRootDir(), "_tmp/local"),
						moduleGeneratedIri).toURI()));

	}

	private OWLOntology getLoadCreateOntology(IRI iri)
			throws OWLOntologyCreationException {
		OWLOntology ontology = isfFullMan.getOntology(iri);
		if (ontology == null) {
			try {
				ontology = isfFullMan.loadOntology(iri);
			} catch (OWLOntologyCreationException e) {
				System.out.println(e.getMessage());
			}
		}
		if (ontology == null) {
			ontology = createOntology(iri);
		}

		return ontology;
	}

	private OWLOntology createOntology(IRI iri)
			throws OWLOntologyCreationException {
		OWLOntology ontology = isfFullMan.createOntology(iri);
		// int i = iri.toString().lastIndexOf('/');
		// String fileName = iri.toString().substring(i + 1);
		// File documentFile = new File(ISFUtil.getSvnRootDir(),
		// "trunk/src/ontology/module/" + fileName);
		isfFullMan.setOntologyDocumentIRI(ontology, IRI.create(getDocumentFile(
				new File(ISFUtil.getSvnRootDir(), "/trunk/src/ontology/module"),
				iri).toURI()));
		changedOntologies.add(ontology);
		return ontology;
	}

	private File getDocumentFile(File dir, IRI iri) {
		int i = iri.toString().lastIndexOf('/');
		String fileName = iri.toString().substring(i + 1);
		return new File(dir, fileName);
	}

	public static void main(String[] args) throws OWLOntologyCreationException,
			OWLOntologyStorageException {

		new BuildModule().run(args);

	}

}
