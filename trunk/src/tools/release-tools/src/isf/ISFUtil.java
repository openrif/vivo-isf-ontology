package isf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEntityVisitor;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class ISFUtil {

	/**
	 * The system property name for the root directory (the parent of trunk) of
	 * the local SVN checkout. This is used by scripts to know where to load
	 * from and create files.
	 */
	public static final String ISF_SVN_ROOT_DIR_PROPERTY = "isf.svn.root.dir";
	public static final String ISF_ONTOLOGY_IRI_PREFIX = "http://purl.obolibrary.org/obo/arg/";

	public static final IRI ISF_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX
			+ "isf.owl");
	public static final IRI ISF_REASONED_IRI = IRI
			.create(ISF_ONTOLOGY_IRI_PREFIX + "isf-reasoned.owl");
	
	public static final IRI ISF_INCLUDE_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-include.owl");
	public static final IRI ISF_EXCLUDE_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-exclude.owl");
	
	public static final IRI ISF_FULL_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-full.owl");
	public static final IRI ISF_FULL_REASONED_IRI = IRI
			.create(ISF_ONTOLOGY_IRI_PREFIX + "isf-full-reasoned.owl");
	
	public static final IRI ISF_SKOS_IRI = IRI
			.create(ISF_ONTOLOGY_IRI_PREFIX + "isf-skos.owl");

	public static File getSvnRootDir() {
		if (ISF_SVN_ROOT_DIR != null) {
			return ISF_SVN_ROOT_DIR;
		} else {
			throw new IllegalStateException(
					"isf.svn.root.dir system property not set or not valid.");
		}
	}

	public static OWLOntology setupAndLoadIsfOntology(OWLOntologyManager man)
			throws OWLOntologyCreationException {
		setupManager(man);
		man.loadOntology(ISF_IRI);
		man.loadOntology(ISF_EXCLUDE_IRI);
		man.loadOntology(ISF_INCLUDE_IRI);
		return man.getOntology(ISF_IRI);
	}

	public static OWLOntology setupAndLoadIsfFullOntology(OWLOntologyManager man)
			throws OWLOntologyCreationException {
		setupManager(man);
		man.loadOntology(ISF_FULL_IRI);
		man.loadOntology(ISF_EXCLUDE_IRI);
		man.loadOntology(ISF_INCLUDE_IRI);
		return man.getOntology(ISF_FULL_IRI);
	}

	public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
			OWLOntology ontology, OWLAnnotationProperty property,
			boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = new HashSet<OWLAnnotationAssertionAxiom>();
		Set<OWLOntology> ontologies;
		if (includeImports) {
			ontologies = ontology.getImportsClosure();
		} else {
			ontologies = Collections.singleton(ontology);
		}
		for (OWLOntology o : ontologies) {
			for (OWLAnnotationAssertionAxiom aaa : o
					.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
				if (aaa.getProperty().getIRI().equals(property.getIRI())) {
					axioms.add(aaa);
				}
			}
		}
		return axioms;
	}

	public static Set<OWLEntity> getSubsClosure(OWLEntity entity,
			final OWLOntology ontology, final OWLReasoner pr) {
		final Set<OWLEntity> entities = new HashSet<OWLEntity>();
		entities.add(entity);

		entity.accept(new OWLEntityVisitor() {

			@Override
			public void visit(OWLAnnotationProperty property) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLDatatype datatype) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLNamedIndividual individual) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLDataProperty property) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLObjectProperty property) {
				entities.add(property);
				Set<OWLObjectPropertyExpression> opes = pr
						.getSubObjectProperties(property, false).getFlattened();
				for (OWLObjectPropertyExpression ope : opes) {
					if (ope instanceof OWLObjectProperty) {
						entities.add((OWLObjectProperty) ope);
					}
				}

			}

			@Override
			public void visit(OWLClass cls) {
				entities.add(cls);
				entities.addAll(pr.getSubClasses(cls, false).getFlattened());
			}
		});

		return entities;
	}

	public static Set<OWLEntity> getSupersClosure(OWLEntity entity,
			final OWLOntology ontology, final OWLReasoner pr) {
		final Set<OWLEntity> entities = new HashSet<OWLEntity>();
		entities.add(entity);

		entity.accept(new OWLEntityVisitor() {

			@Override
			public void visit(OWLAnnotationProperty property) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLDatatype datatype) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLNamedIndividual individual) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLDataProperty property) {
				// TODO Auto-generated method stub

			}

			@Override
			public void visit(OWLObjectProperty property) {
				entities.add(property);
				Set<OWLObjectPropertyExpression> opes = pr
						.getSuperObjectProperties(property, false)
						.getFlattened();
				for (OWLObjectPropertyExpression ope : opes) {
					if (ope instanceof OWLObjectProperty) {
						entities.add((OWLObjectProperty) ope);
					}
				}

			}

			@Override
			public void visit(OWLClass cls) {
				entities.add(cls);
				entities.addAll(pr.getSuperClasses(cls, false).getFlattened());

			}
		});

		return entities;
	}

	public static Set<OWLAnnotationAssertionAxiom> getAnnotationAxioms(
			OWLOntology ontology, boolean includeImports,
			OWLAnnotationSubject subject) {
		Set<OWLAnnotationAssertionAxiom> axioms = new HashSet<OWLAnnotationAssertionAxiom>();
		Set<OWLOntology> ontologies;
		if (includeImports) {
			ontologies = ontology.getImportsClosure();
		} else {
			ontologies = Collections.singleton(ontology);
		}
		for (OWLOntology o : ontologies) {
			axioms.addAll(o.getAnnotationAssertionAxioms(subject));
		}
		return axioms;

	}

	public static Set<OWLAxiom> getDefiningAxioms(final OWLEntity entity,
			OWLOntology ontology, boolean includeImports) {
		final Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		Set<OWLOntology> ontologies;
		if (includeImports) {
			ontologies = ontology.getImportsClosure();
		} else {
			ontologies = Collections.singleton(ontology);
		}

		for (final OWLOntology o : ontologies) {
			entity.accept(new OWLEntityVisitor() {

				@Override
				public void visit(OWLAnnotationProperty property) {
					axioms.addAll(o.getAxioms(property));
				}

				@Override
				public void visit(OWLDatatype datatype) {
					axioms.addAll(o.getAxioms(datatype));

				}

				@Override
				public void visit(OWLNamedIndividual individual) {
					axioms.addAll(o.getAxioms(individual));

				}

				@Override
				public void visit(OWLDataProperty property) {
					axioms.addAll(o.getAxioms(property));

				}

				@Override
				public void visit(OWLObjectProperty property) {
					axioms.addAll(o.getAxioms(property));

				}

				@Override
				public void visit(OWLClass cls) {
					axioms.addAll(o.getAxioms(cls));

				}
			});
		}

		return axioms;
	}

	public static Set<LabelInfo> getLabels(IRI iri, Set<OWLOntology> ontologies) {
		Set<LabelInfo> infos = new HashSet<ISFUtil.LabelInfo>();

		for (OWLOntology ontology : ontologies) {
			Set<OWLAnnotationAssertionAxiom> axioms = ontology
					.getAnnotationAssertionAxioms(iri);
			for (OWLAnnotationAssertionAxiom axiom : axioms) {
				if (axiom.getProperty().getIRI()
						.equals(OWLRDFVocabulary.RDFS_LABEL.getIRI())) {
					infos.add(new LabelInfo(ontology, axiom));
				}
			}
		}

		
		return infos;
	}

	public static List<OWLEntity> getEntitiesSortedByIri(OWLOntology ontology,
			boolean includeImports) {
		ArrayList<OWLEntity> entities = new ArrayList<OWLEntity>(
				ontology.getSignature(includeImports));
		Collections.sort(entities, new Comparator<OWLEntity>() {

			@Override
			public int compare(OWLEntity o1, OWLEntity o2) {

				return o1.getIRI().compareTo(o2.getIRI());
			}
		});
		return entities;

	}

	public static void setupManager(OWLOntologyManager man) {
		AutoIRIMapper mapper = new AutoIRIMapper(new File(getSvnRootDir(),
				"trunk/src/ontology"), true);
		man.clearIRIMappers();
		man.addIRIMapper(mapper);
	}

	private static File ISF_SVN_ROOT_DIR = null;

	static {
		String svnRoot = System.getProperty(ISF_SVN_ROOT_DIR_PROPERTY);
		if (svnRoot != null) {
			File svnRootDir = new File(svnRoot).getAbsoluteFile();
			if (checkValidSvnLocation(svnRootDir)) {
				ISF_SVN_ROOT_DIR = svnRootDir;
			}
		}
	}

	private static boolean checkValidSvnLocation(File svnRootDir) {
		if (svnRootDir.isDirectory()) {
			File tools = new File(svnRootDir, "trunk/src/tools");
			if (tools.exists()) {
				return true;
			}
		}
		return false;
	}

	public static class LabelInfo {
		public final OWLAnnotationAssertionAxiom axiom;
		public final OWLOntology ontology;

		public LabelInfo(OWLOntology ontology, OWLAnnotationAssertionAxiom axiom) {
			this.ontology = ontology;
			this.axiom = axiom;
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "Ontology: " + ontology.getOntologyID() + " has label: "
					+ axiom.getValue().toString();
		}
	}

}
