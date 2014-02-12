package isf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
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
import org.semanticweb.owlapi.reasoner.ReasonerInternalException;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

/**
 * @author Shahim Essaid
 * 
 */
public class ISFUtil {

	/**
	 * The system property that points to the trunk/master (not the parent of
	 * trunk in Google Code SVN) checkout of the ISF repository
	 */
	public static final String ISF_TRUNK_PROPERTY = "isf.trunk";
	public static final String ISF_ONTOLOGY_IRI_PREFIX = "http://purl.obolibrary.org/obo/arg/";

	public static final IRI ISF_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX + "isf.owl");
	public static final IRI ISF_REASONED_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-reasoned.owl");

	public static final IRI ISF_FULL_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX + "isf-full.owl");
	public static final IRI ISF_FULL_REASONED_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX
			+ "isf-full-reasoned.owl");

	public static final IRI ISF_SKOS_IRI = IRI.create(ISF_ONTOLOGY_IRI_PREFIX + "isf-skos.owl");

	public static OWLOntology setupAndLoadIsfOntology(OWLOntologyManager man)
			throws OWLOntologyCreationException {
		setupManagerMapper(man);
		man.loadOntology(ISF_IRI);
		return man.getOntology(ISF_IRI);
	}

	public static OWLReasoner getDefaultReasoner(OWLOntology ontology) {
		return new FaCTPlusPlusReasonerFactory().createReasoner(ontology);
	}

	public static OWLOntology setupAndLoadIsfFullOntology(OWLOntologyManager man)
			throws OWLOntologyCreationException {
		setupManagerMapper(man);
		man.loadOntology(ISF_FULL_IRI);
		return man.getOntology(ISF_FULL_IRI);
	}

	public static Set<OWLAnnotationAssertionAxiom> getAnnotationAssertionAxioms(
			OWLOntology ontology, OWLAnnotationProperty property, boolean includeImports) {
		Set<OWLAnnotationAssertionAxiom> axioms = new HashSet<OWLAnnotationAssertionAxiom>();
		Set<OWLOntology> ontologies;
		if (includeImports) {
			ontologies = ontology.getImportsClosure();
		} else {
			ontologies = Collections.singleton(ontology);
		}
		for (OWLOntology o : ontologies) {
			for (OWLAnnotationAssertionAxiom aaa : o.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
				if (aaa.getProperty().getIRI().equals(property.getIRI())) {
					axioms.add(aaa);
				}
			}
		}
		return axioms;
	}

	public static Set<OWLEntity> getSubs(OWLEntity entity, final boolean closure,
			final OWLReasoner pr) {
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
				entities.add(property);
				entities.addAll(pr.getSubDataProperties(property, closure).getFlattened());

			}

			@Override
			public void visit(OWLObjectProperty property) {
				entities.add(property);
				Set<OWLObjectPropertyExpression> opes = pr
						.getSubObjectProperties(property, closure).getFlattened();
				for (OWLObjectPropertyExpression ope : opes) {
					if (ope instanceof OWLObjectProperty) {
						entities.add((OWLObjectProperty) ope);
					}
				}

			}

			@Override
			public void visit(OWLClass cls) {
				entities.add(cls);
				entities.addAll(pr.getSubClasses(cls, closure).getFlattened());
			}
		});

		return entities;
	}

	public static Set<OWLEntity> getSupers(OWLEntity entity, final boolean closure,
			final OWLReasoner pr) {
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
				entities.add(property);
				entities.addAll(pr.getSuperDataProperties(property, closure).getFlattened());

			}

			@Override
			public void visit(OWLObjectProperty property) {
				entities.add(property);
				Set<OWLObjectPropertyExpression> opes = null;

				// TODO: not sure why Fact++ is erroring out here like:

				// <http://purl.obolibrary.org/obo/ERO_0000558> had error: Role
				// expression expected in getSupRoles()

				// <http://eagle-i.org/ont/app/1.0/has_part_construct_insert>
				// had error: Role expression expected in getSupRoles()

				// <http://eagle-i.org/ont/app/1.0/has_measurement_scale> had
				// error: Role expression expected in getSupRoles()
				try {
					opes = pr.getSuperObjectProperties(property, closure).getFlattened();
				} catch (ReasonerInternalException e) {
					System.err.println(property + " had error: " + e.getMessage());
				}
				if (opes == null) {
					return;
				}
				for (OWLObjectPropertyExpression ope : opes) {
					if (ope instanceof OWLObjectProperty) {
						entities.add((OWLObjectProperty) ope);
					}
				}

			}

			@Override
			public void visit(OWLClass cls) {
				entities.add(cls);
				entities.addAll(pr.getSuperClasses(cls, closure).getFlattened());

			}
		});

		return entities;
	}

	public static Set<OWLAnnotationAssertionAxiom> getSubjectAnnotationAxioms(OWLOntology ontology,
			boolean includeImports, OWLAnnotationSubject subject) {
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

	public static Set<OWLAxiom> getDefiningAxioms(final OWLEntity entity, OWLOntology ontology,
			boolean includeImports) {
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

	// TODO: where is this used?
	public static Set<LabelInfo> getLabels(IRI iri, Set<OWLOntology> ontologies) {
		Set<LabelInfo> infos = new HashSet<ISFUtil.LabelInfo>();

		for (OWLOntology ontology : ontologies) {
			Set<OWLAnnotationAssertionAxiom> axioms = ontology.getAnnotationAssertionAxioms(iri);
			for (OWLAnnotationAssertionAxiom axiom : axioms) {
				if (axiom.getProperty().getIRI().equals(OWLRDFVocabulary.RDFS_LABEL.getIRI())) {
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

	/**
	 * It sets the IRI mapper to recursively find ontologies under src/ontology.
	 * All IRIs under that path should be unique.
	 * 
	 * @param man
	 * @return
	 */
	public static OWLOntologyManager setupManagerMapper(OWLOntologyManager man) {
		AutoIRIMapper mapper = new AutoIRIMapper(new File(getTrunkDirectory(), "src/ontology"),
				true);
		man.clearIRIMappers();
		man.addIRIMapper(mapper);
		return man;
	}

	private static File ISF_TRUNK_DIR = null;

	public static File getTrunkDirectory() {
		if (ISF_TRUNK_DIR == null) {
			throw new IllegalStateException("ISF trunk directory is null");
		}
		return ISF_TRUNK_DIR;
	}

	public static File getDefaultModuleDirectory() {
		return new File(getTrunkDirectory(), "src/ontology/module");
	}

	public static File getGeneratedDirectory() {
		File f;

		try {
			f = new File(getTrunkDirectory(), "../generated").getCanonicalFile();
		} catch (IOException e) {
			throw new IllegalStateException(
					"Failed to get canonical path to ISF generated directory.");
		}

		return f;
	}

	static {
		String isfTrunk = System.getProperty(ISF_TRUNK_PROPERTY);
		if (isfTrunk == null) {
			isfTrunk = System.getenv(ISF_TRUNK_PROPERTY.toUpperCase().replace(".", "_"));
		}
		if (isfTrunk != null) {
			File isfTrunkDir;
			try {
				isfTrunkDir = new File(isfTrunk).getCanonicalFile();
			} catch (IOException e) {
				throw new IllegalStateException("Error determining ISF trunk directory", e);
			}
			if (checkValidTrunkLocation(isfTrunkDir)) {
				ISF_TRUNK_DIR = isfTrunkDir;
			}
		}
	}

	public static void setISFTrunkDirecotry(File isfTrunkDirectory) {
		ISF_TRUNK_DIR = isfTrunkDirectory;
	}

	private static boolean checkValidTrunkLocation(File trunkDir) {
		if (trunkDir != null && trunkDir.isDirectory()) {
			File tools = new File(trunkDir, "src/tools");
			return tools.exists();
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
