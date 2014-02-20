package isf.module.internal;

import isf.ISFUtil;
import isf.module.SimpleModule;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class SimpleModuleBuilder {
	// private OWLReasoner reasoner;
	private SimpleModule module;
	private OWLDataFactory df;

	public SimpleModuleBuilder(SimpleModule simeplModule) {
		this.module = simeplModule;
		df = module.getDataFactory();
	}

	public void run() throws Exception {

		module.builderPhase("Generating module: " + module.getName());

		if (module.getReasoner().getUnsatisfiableClasses().getEntities().size() > 0) {
			System.out.println("Unsatisfieds: "
					+ module.getReasoner().getUnsatisfiableClasses().getEntities());
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

	}

	public void addIncludes() {
		Set<OWLEntity> entities = Util.getIncludeEntities(module.getAnnotationOntology(), true);

		for (OWLEntity e : entities) {
			module.builderMessage("Add: " + e.getEntityType() + " - " + e);
			addAxiom(module.getDataFactory().getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, module.getSourceOntology(), true));
		}

	}

	public void addIncludeSubs() {
		Set<OWLEntity> entities = Util.getIncludeSubsEntities(module.getAnnotationOntology(), true);
		// System.out.println("Found sub annotations for: " + entities);
		Set<OWLEntity> closureEntities = new HashSet<OWLEntity>();

		for (OWLEntity e : entities) {
			module.builderMessage("Add subs: " + e.getEntityType() + " - " + e);
			closureEntities.addAll(ISFUtil.getSubs(e, true, module.getReasoner()));
		}
		for (OWLEntity e : closureEntities) {
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, module.getSourceOntology(), true));
		}
	}

	private void addIncludeInstances() {
		Set<OWLEntity> entities = Util.getIncludeInstances(module.getAnnotationOntology(), true);

		for (OWLEntity e : entities) {
			module.builderMessage("Add instance: " + e.getEntityType() + " - " + e);
			addAxiom(df.getOWLDeclarationAxiom(e));
			addAxioms(ISFUtil.getDefiningAxioms(e, module.getSourceOntology(), true));
		}

	}

	public void removeExcludes() {
		Set<OWLEntity> entities = Util.getExcludeEntities(module.getAnnotationOntology(), true);
		for (OWLEntity entity : entities) {
			module.builderMessage("Remove: " + entity.getEntityType() + " - " + entity);
			removeAxiom(df.getOWLDeclarationAxiom(entity));
			removeAxioms(ISFUtil.getDefiningAxioms(entity, module.getSourceOntology(), true));

			if (entity instanceof OWLClass) {
				OWLClass c = (OWLClass) entity;
				Set<OWLClass> subs = module.getReasoner().getSubClasses(c, true).getFlattened();
				for (OWLClass sub : subs) {
					OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(sub, c);
					if (module.getOntology().containsAxiom(subAxiom)) {
						removeAxiom(subAxiom);
						;
						for (OWLClass supr : module.getReasoner().getSuperClasses(c, true)
								.getFlattened()) {
							if (module.getOntology().containsClassInSignature(supr.getIRI())) {
								addAxiom(df.getOWLSubClassOfAxiom(sub, supr));
							}
						}
					}
				}

			}
		}

	}

	public void removeExcludeSubs() {
		Set<OWLEntity> entities = Util.getExcludeSubsEntities(module.getAnnotationOntology(), true);
		// System.out.println("Excluding class: " + entities);
		Set<OWLEntity> entityiesClosure = new HashSet<OWLEntity>();
		for (OWLEntity entity : entities) {
			module.builderMessage("Remove subs: " + entity.getEntityType() + " - " + entity);
			entityiesClosure.addAll(ISFUtil.getSubs(entity, true, module.getReasoner()));
		}
		// System.out.println("Excluding class closure: " + entityiesClosure);
		for (OWLEntity entity : entityiesClosure) {
			removeAxiom(df.getOWLDeclarationAxiom(entity));
			removeAxioms(ISFUtil.getDefiningAxioms(entity, module.getSourceOntology(), true));
		}

	}

	public void mergeModuleInclude() {
		// we have to do this manually but first exclude
		// addAxioms(moduleOntologyInclude.getAxioms());
		Set<OWLAxiom> axioms = module.getIncludeOntology().getAxioms();
		axioms.removeAll(module.getExcludeOntology().getAxioms());
		module.getManager().addAxioms(module.getOntology(), axioms);

		// add any ontology annotations from the annotation ontology
		for (OWLAnnotation a : module.getAnnotationOntology().getAnnotations()) {
			AddOntologyAnnotation oa = new AddOntologyAnnotation(module.getOntology(), a);
			module.getManager().applyChange(oa);
		}
	}

	public void addClosureToBfo() {
		for (OWLEntity entity : module.getOntology().getSignature()) {
			Set<OWLEntity> supers = ISFUtil.getSupers(entity, true, module.getReasoner());
			for (final OWLEntity supr : supers) {
				if (!supr.getIRI().toString().contains("BFO_")) {
					Set<OWLAxiom> axioms = ISFUtil.getDefiningAxioms(supr,
							module.getSourceOntology(), true);
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
		entitiesToAnnotate.addAll(module.getOntology().getSignature());

		Set<OWLEntity> annotatedEntities = new HashSet<OWLEntity>();

		while (entitiesToAnnotate.size() > 0) {
			Set<OWLEntity> newEntities = new HashSet<OWLEntity>();
			Iterator<OWLEntity> i = entitiesToAnnotate.iterator();
			while (i.hasNext()) {
				OWLEntity entity = i.next();
				i.remove();
				annotatedEntities.add(entity);
				Set<OWLAnnotationAssertionAxiom> axioms = ISFUtil.getSubjectAnnotationAxioms(
						module.getSourceOntology(), true, entity.getIRI());
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
		for (OWLEntity e : module.getOntology().getSignature()) {
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
		if (!module.getExcludeOntology().containsAxiom(axiom)
		// && !moduleOntologyInclude.containsAxiom(axiom) // TODO: check
		// if commenting this out will cause problems. It was preventing
		// the
		// includes.
				&& !removedAxioms.contains(axiom) && !module.getOntology().containsAxiom(axiom)) {
			// System.out.println("\t" + axiom.toString());
			module.getManager().addAxiom(module.getOntology(), axiom);
		}
	}

	private void removeAxioms(Set<? extends OWLAxiom> axioms) {
		for (OWLAxiom axiom : axioms) {
			removeAxiom(axiom);
		}
	}

	Set<OWLAxiom> removedAxioms = new HashSet<OWLAxiom>();

	private void removeAxiom(OWLAxiom axiom) {
		module.getManager().removeAxiom(module.getOntology(), axiom);
		removedAxioms.add(axiom);
	}
}
