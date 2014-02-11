package isf.module.internal;

import isf.ISFUtil;
import isf.release.action.Reporter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

public class BuildModule {

	private OWLOntologyManager isfMan; // =
										// OWLManager.createOWLOntologyManager();
	private OWLDataFactory df;
	private OWLOntology isfOntology;

	private OWLOntology moduleOntologyAnnotation;
	private OWLOntology moduleOntologyInclude;
	private OWLOntology moduleOntologyExclude;
	public OWLOntology moduleOntologyGenerated;
	private Set<OWLOntology> changedOntologies = new HashSet<OWLOntology>();
	private OWLReasoner reasoner;

	public void run() throws Exception {
		inti();

		reporter.setHeading("Generating module: " + moduleName);

		FaCTPlusPlusReasonerFactory prf = new FaCTPlusPlusReasonerFactory();
		System.out.println("Creating reasoner.");
		reasoner = prf.createReasoner(isfOntology);

		if (reasoner.getUnsatisfiableClasses().getEntities().size() > 0) {
			System.out.println("Unsatisfieds: " + reasoner.getUnsatisfiableClasses().getEntities());
		}

		System.out.println("Doing includes: ");
		addIncludes();
		System.out.println("Doing include subs: ");
		addIncludeSubs();

		System.out.println("Doing include instances");
		addIncludeInstances();

		System.out.println("Doing excludes: ");
		removeExcludes();
		System.out.println("Doing exclude subs: ");
		removeExcludeSubs();

		System.out.println("Merging in by hand: ");
		mergeModuleInclude();

		System.out.println("Adding parents to BFO: ");
		addClosureToBfo();

		System.out.println("Adding annotations: ");
		addAnnotations();

		System.out.println("Typing all entities: ");
		typeAllEntities();

		System.out.println("Saving modified ontologies: " + changedOntologies.toString());
		save();
	}

	public void addIncludes() {
		Set<OWLEntity> entities = ModuleUtil.getIncludeEntities(moduleOntologyAnnotation, true);

		for (OWLEntity e : entities) {
			reporter.addLine("Add: " + e.getEntityType() + " - " + e);
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, isfOntology, true));
		}

	}

	public void addIncludeSubs() {
		Set<OWLEntity> entities = ModuleUtil.getIncludeSubsEntities(moduleOntologyAnnotation, true);
		// System.out.println("Found sub annotations for: " + entities);
		Set<OWLEntity> closureEntities = new HashSet<OWLEntity>();

		for (OWLEntity e : entities) {
			reporter.addLine("Add subs: " + e.getEntityType() + " - " + e);
			closureEntities.addAll(ISFUtil.getSubs(e, true, reasoner));
		}
		for (OWLEntity e : closureEntities) {
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, isfOntology, true));
		}
	}

	private void addIncludeInstances() {
		Set<OWLEntity> entities = ModuleUtil.getIncludeInstances(moduleOntologyAnnotation, true);

		for (OWLEntity e : entities) {
			reporter.addLine("Add instance: " + e.getEntityType() + " - " + e);
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, isfOntology, true));
		}

	}

	public void removeExcludes() {
		Set<OWLEntity> entities = ModuleUtil.getExcludeEntities(moduleOntologyAnnotation, true);
		for (OWLEntity entity : entities) {
			reporter.addLine("Remove: " + entity.getEntityType() + " - " + entity);
			removeAxiom(df.getOWLDeclarationAxiom(entity));
			removeAxioms(ISFUtil.getDefiningAxioms(entity, isfOntology, true));

			if (entity instanceof OWLClass) {
				OWLClass c = (OWLClass) entity;
				Set<OWLClass> subs = reasoner.getSubClasses(c, true).getFlattened();
				for (OWLClass sub : subs) {
					OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(sub, c);
					if (moduleOntologyGenerated.containsAxiom(subAxiom)) {
						removeAxiom(subAxiom);
						;
						for (OWLClass supr : reasoner.getSuperClasses(c, true).getFlattened()) {
							if (moduleOntologyGenerated.containsClassInSignature(supr.getIRI())) {
								addAxiom(df.getOWLSubClassOfAxiom(sub, supr));
							}
						}
					}
				}

			}
		}

	}

	public void removeExcludeSubs() {
		Set<OWLEntity> entities = ModuleUtil.getExcludeSubsEntities(moduleOntologyAnnotation, true);
		// System.out.println("Excluding class: " + entities);
		Set<OWLEntity> entityiesClosure = new HashSet<OWLEntity>();
		for (OWLEntity entity : entities) {
			reporter.addLine("Remove subs: " + entity.getEntityType() + " - " + entity);
			entityiesClosure.addAll(ISFUtil.getSubs(entity, true, reasoner));
		}
		// System.out.println("Excluding class closure: " + entityiesClosure);
		for (OWLEntity entity : entityiesClosure) {
			removeAxiom(df.getOWLDeclarationAxiom(entity));
			removeAxioms(ISFUtil.getDefiningAxioms(entity, isfOntology, true));
		}

	}

	public void mergeModuleInclude() {
		// we have to do this manually but first exclude
		// addAxioms(moduleOntologyInclude.getAxioms());
		Set<OWLAxiom> axioms = moduleOntologyInclude.getAxioms();
		axioms.removeAll(moduleOntologyExclude.getAxioms());
		isfMan.addAxioms(moduleOntologyGenerated, axioms);

		// add any ontology annotations from the annotation ontology
		for (OWLAnnotation a : moduleOntologyAnnotation.getAnnotations()) {
			AddOntologyAnnotation oa = new AddOntologyAnnotation(moduleOntologyGenerated, a);
			isfMan.applyChange(oa);
		}
	}

	public void addClosureToBfo() {
		for (OWLEntity entity : moduleOntologyGenerated.getSignature()) {
			Set<OWLEntity> supers = ISFUtil.getSupers(entity, true, reasoner);
			for (final OWLEntity supr : supers) {
				if (!supr.getIRI().toString().contains("BFO_")) {
					Set<OWLAxiom> axioms = ISFUtil.getDefiningAxioms(supr, isfOntology, true);
					for (OWLAxiom axiom : axioms) {
						axiom.accept(new OWLAxiomVisitorAdapter() {
							@Override
							public void visit(OWLSubClassOfAxiom axiom) {
								if (axiom.getSubClass() instanceof OWLClass
										&& axiom.getSubClass().asOWLClass().getIRI()
												.equals(supr.getIRI())) {

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
				Set<OWLAnnotationAssertionAxiom> axioms = ISFUtil.getSubjectAnnotationAxioms(
						isfOntology, true, entity.getIRI());
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
			if (da.getEntity().getIRI().equals(OWLRDFVocabulary.OWL_NOTHING.getIRI())) {
				return;
			}
		}
		if (!moduleOntologyExclude.containsAxiom(axiom)
		// && !moduleOntologyInclude.containsAxiom(axiom) // TODO: check
		// if commenting this out will cause problems. It was preventing the
		// includes.
				&& !removedAxioms.contains(axiom) && !moduleOntologyGenerated.containsAxiom(axiom)) {
			// System.out.println("\t" + axiom.toString());
			isfMan.addAxiom(moduleOntologyGenerated, axiom);
		}
	}

	private void removeAxioms(Set<? extends OWLAxiom> axioms) {
		for (OWLAxiom axiom : axioms) {
			removeAxiom(axiom);
		}
	}

	Set<OWLAxiom> removedAxioms = new HashSet<OWLAxiom>();
	private String moduleName;
	private File moduleDirectory;
	private Reporter reporter;
	private boolean isReleaseRun;

	private void removeAxiom(OWLAxiom axiom) {
		isfMan.removeAxiom(moduleOntologyGenerated, axiom);
		removedAxioms.add(axiom);

	}

	private void save() throws Exception {
		RDFXMLOntologyFormat format = new RDFXMLOntologyFormat();
		format.setAddMissingTypes(true);
		for (OWLOntology ontology : changedOntologies) {
			System.out.println("Saving ontology: " + ontology.getOntologyID());
			isfMan.saveOntology(ontology, format);
		}

		if (!isReleaseRun) {
			File file = new File(moduleDirectory, moduleName + "-module.owl");
			System.out.println("Saving to: " + file.getAbsolutePath());
			isfMan.saveOntology(moduleOntologyGenerated, new FileOutputStream(file));
		}

	}

	public void setIsfOntologyAndMan(OWLOntology ontology, OWLOntologyManager man) {
		isfMan = man;
		isfOntology = ontology;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public void setIsRelease(boolean isReleaseRun) {
		this.isReleaseRun = isReleaseRun;
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public void setModuleDirectory(File directory) {
		this.moduleDirectory = directory;
	}

	private void inti() throws OWLOntologyCreationException {
		df = isfMan.getOWLDataFactory();

		// load module annotations ontology
		IRI moduleIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + moduleName
				+ "-module-annotation.owl");
		moduleOntologyAnnotation = getLoadCreateOntology(moduleIri);

		// load module include ontology
		IRI moduleIncludeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + moduleName
				+ "-module-include.owl");
		moduleOntologyInclude = getLoadCreateOntology(moduleIncludeIri);

		// load module exclude ontology
		IRI moduleExcludeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + moduleName
				+ "-module-exclude.owl");
		moduleOntologyExclude = getLoadCreateOntology(moduleExcludeIri);

		// add the exclude file import
		AddImport ai = new AddImport(moduleOntologyAnnotation,
				df.getOWLImportsDeclaration(moduleExcludeIri));

		List<OWLOntologyChange> changes = isfMan.applyChange(ai);
		if (changes.size() > 0) {
			changedOntologies.add(moduleOntologyAnnotation);
		}

		// add the include file import
		ai = new AddImport(moduleOntologyAnnotation, df.getOWLImportsDeclaration(moduleIncludeIri));
		changes = isfMan.applyChange(ai);
		if (changes.size() > 0) {
			changedOntologies.add(moduleOntologyAnnotation);
		}

		// add the isf import
		ai = new AddImport(moduleOntologyAnnotation, df.getOWLImportsDeclaration(ISFUtil.ISF_IRI));
		changes = isfMan.applyChange(ai);
		if (changes.size() > 0) {
			changedOntologies.add(moduleOntologyAnnotation);
		}

		// always create a new one and save it to the local folder
		IRI moduleGeneratedIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + moduleName
				+ "-module.owl");
		moduleOntologyGenerated = createOntology(moduleGeneratedIri);
		changedOntologies.remove(moduleOntologyGenerated);

	}

	private OWLOntology getLoadCreateOntology(IRI iri) throws OWLOntologyCreationException {
		OWLOntology ontology = isfMan.getOntology(iri);
		if (ontology == null) {
			try {
				ontology = isfMan.loadOntology(iri);
			} catch (OWLOntologyCreationException e) {
				System.err.println(e.getMessage());
			}
		}
		if (ontology == null) {
			ontology = createOntology(iri);
		}

		return ontology;
	}

	private OWLOntology createOntology(IRI iri) throws OWLOntologyCreationException {
		OWLOntology ontology = isfMan.createOntology(iri);

		isfMan.setOntologyDocumentIRI(ontology,
				IRI.create(getDocumentFile(moduleDirectory, iri).toURI()));
		changedOntologies.add(ontology);
		return ontology;
	}

	private File getDocumentFile(File dir, IRI iri) {
		int i = iri.toString().lastIndexOf('/');
		String fileName = iri.toString().substring(i + 1);
		return new File(dir, fileName);
	}

	public static void main(String[] args) throws Exception, OWLOntologyStorageException {

		BuildModule module = new BuildModule();
		module.setModuleName(args[0]);
		module.setModuleDirectory(new File(ISFUtil.getTrunkDirectory(), "src/ontology/module"));

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		ISFUtil.setupAndLoadIsfOntology(man);
		module.setIsfOntologyAndMan(man.getOntology(ISFUtil.ISF_IRI), man);

		Reporter reporter = new Reporter(new File(ISFUtil.getTrunkDirectory(),
				"src/ontology/module/" + args[0] + "-module-report.txt"));
		module.setReporter(reporter);

		module.run();

		reporter.save();

	}

}
