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
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

public class BuildModule {

	private OWLOntologyManager isfMan = OWLManager.createOWLOntologyManager();
	private OWLDataFactory df = isfMan.getOWLDataFactory();
	private OWLOntology isfOntology;

	private OWLOntology moduleOntology;
	private OWLOntology moduleOntologyInclude;
	private OWLOntology moduleOntologyExclude;
	private OWLOntology moduleOntologyGenerated;
	private String[] args;
	private Set<OWLOntology> changedOntologies = new HashSet<OWLOntology>();
	private OWLReasoner reasoner;

	private void run(String[] args) throws OWLOntologyCreationException,
			OWLOntologyStorageException {
		this.args = args;
		inti();
		
		System.out.println("Script args: "+ args);
		System.out.println("ISF ontology with import count: "+ isfOntology.getImportsClosure().size());
		System.out.println("SVN location: " + ISFUtil.getSvnRootDir());

		FaCTPlusPlusReasonerFactory prf = new FaCTPlusPlusReasonerFactory();
		System.out.println("Creating reasoner.");
		reasoner = prf.createReasoner(isfOntology);
		
		
		
		if (reasoner.getUnsatisfiableClasses().getEntities().size() > 0) {
			System.out.println("Unsatisfieds: "
					+ reasoner.getUnsatisfiableClasses().getEntities());
		}

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
			addAxioms(ISFUtil.getDefiningAxioms(e, isfOntology, true));
		}

	}

	public void addIncludeSubs() {
		Set<OWLEntity> entities = ModuleUtil.getIncludeSubsEntities(
				moduleOntology, true);
		Set<OWLEntity> closureEntities = new HashSet<OWLEntity>();

		for (OWLEntity e : entities) {
			closureEntities.addAll(ISFUtil.getSubsClosure(e, isfOntology,
					reasoner));
		}
		for (OWLEntity e : closureEntities) {
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, isfOntology, true));
		}
	}

	public void removeExcludes() {
		// TODO, try to move lower classes up
		Set<OWLEntity> entities = ModuleUtil.getExcludeEntities(moduleOntology,
				true);
		for (OWLEntity entity : entities) {

			removeAxiom(df.getOWLDeclarationAxiom(entity));
			removeAxioms(ISFUtil.getDefiningAxioms(entity, isfOntology, true));

			if (entity instanceof OWLClass) {
				OWLClass c = (OWLClass) entity;
				Set<OWLClass> subs = reasoner.getSubClasses(c, true)
						.getFlattened();
				for (OWLClass sub : subs) {
					OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(sub,
							c);
					if (moduleOntologyGenerated.containsAxiom(subAxiom)) {
						removeAxiom(subAxiom);
						;
						for (OWLClass supr : reasoner.getSuperClasses(c, true)
								.getFlattened()) {
							if (moduleOntologyGenerated
									.containsClassInSignature(supr.getIRI())) {
								addAxiom(df.getOWLSubClassOfAxiom(sub, supr));
							}
						}
					}
				}

			}
		}

	}

	public void removeExcludeSubs() {
		Set<OWLEntity> entities = ModuleUtil.getExcludeSubsEntities(
				moduleOntology, true);
		System.out.println("Excluding class: " + entities);
		Set<OWLEntity> entityiesClosure = new HashSet<OWLEntity>();
		for (OWLEntity entity : entities) {
			entityiesClosure.addAll(ISFUtil.getSubsClosure(entity, isfOntology,
					reasoner));
		}
		System.out.println("Excluding class closure: " + entityiesClosure);
		for (OWLEntity entity : entityiesClosure) {
			removeAxiom(df.getOWLDeclarationAxiom(entity));
			removeAxioms(ISFUtil.getDefiningAxioms(entity, isfOntology, true));
		}

	}

	public void mergeModuleInclude() {
		addAxioms(moduleOntologyInclude.getAxioms());
	}

	public void addClosureToBfo() {
		for (OWLEntity entity : moduleOntologyGenerated.getSignature()) {
			Set<OWLEntity> supers = ISFUtil.getSupersClosure(entity,
					isfOntology, reasoner);
			for (final OWLEntity supr : supers) {
				if (!supr.getIRI().toString().contains("BFO_")) {
					Set<OWLAxiom> axioms = ISFUtil.getDefiningAxioms(supr,
							isfOntology, true);
					for (OWLAxiom axiom : axioms) {
						axiom.accept(new OWLAxiomVisitorAdapter() {
							@Override
							public void visit(OWLSubClassOfAxiom axiom) {
								if (axiom.getSubClass() instanceof OWLClass
										&& axiom.getSubClass().asOWLClass()
												.getIRI().equals(supr.getIRI())) {

									if (axiom.getSuperClass() instanceof OWLClass) {
										addAxiom(axiom);
									}
								}
							}
							// TODO the other types of entities
						});
					}
				}
			}
		}

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
						.getAnnotationAxioms(isfOntology, true, entity.getIRI());
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
		if (axiom instanceof OWLDeclarationAxiom) {
			OWLDeclarationAxiom da = (OWLDeclarationAxiom) axiom;
			if (da.getEntity().getIRI()
					.equals(OWLRDFVocabulary.OWL_NOTHING.getIRI())) {
				return;
			}
		}
		if (!moduleOntologyExclude.containsAxiom(axiom)
				&& !moduleOntologyInclude.containsAxiom(axiom) && !removedAxioms.contains(axiom)) {
			isfMan.addAxiom(moduleOntologyGenerated, axiom);
		}
	}

	private void removeAxioms(Set<? extends OWLAxiom> axioms) {
		for (OWLAxiom axiom : axioms) {
			removeAxiom(axiom);
		}
	}

	Set<OWLAxiom> removedAxioms = new HashSet<OWLAxiom>();
	
	private void removeAxiom(OWLAxiom axiom) {
		isfMan.removeAxiom(moduleOntologyGenerated, axiom);
		removedAxioms.add(axiom);

	}

	private void save() throws OWLOntologyStorageException {
		RDFXMLOntologyFormat format = new RDFXMLOntologyFormat();
		format.setAddMissingTypes(true);
		for (OWLOntology ontology : changedOntologies) {
			System.out.println("Saving ontology: " + ontology.getOntologyID());
			isfMan.saveOntology(ontology, format);
		}

	}

	private void inti() throws OWLOntologyCreationException {
		isfOntology = ISFUtil.setupAndLoadIsfOntology(isfMan);
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
		isfMan.setOntologyDocumentIRI(
				moduleOntologyGenerated,
				IRI.create(getDocumentFile(
						new File(ISFUtil.getSvnRootDir(), "_tmp/local"),
						moduleGeneratedIri).toURI()));

	}

	private OWLOntology getLoadCreateOntology(IRI iri)
			throws OWLOntologyCreationException {
		OWLOntology ontology = isfMan.getOntology(iri);
		if (ontology == null) {
			try {
				ontology = isfMan.loadOntology(iri);
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
		OWLOntology ontology = isfMan.createOntology(iri);
		// int i = iri.toString().lastIndexOf('/');
		// String fileName = iri.toString().substring(i + 1);
		// File documentFile = new File(ISFUtil.getSvnRootDir(),
		// "trunk/src/ontology/module/" + fileName);
		isfMan.setOntologyDocumentIRI(ontology,
				IRI.create(getDocumentFile(
						new File(ISFUtil.getSvnRootDir(),
								"/trunk/src/ontology/module"), iri).toURI()));
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
