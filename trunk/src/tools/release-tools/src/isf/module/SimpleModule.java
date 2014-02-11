package isf.module;

import java.io.FileNotFoundException;

import isf.ISFUtil;
import isf.module.internal.SimpleModuleBuilder;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

public class SimpleModule extends AbstractModule {

	private OWLOntologyManager moduleManager;

	private OWLOntology annotationOntology;

	private OWLOntology includeOntology;

	private OWLOntology excludeOntology;

	private OWLOntology moduleOntology;

	private OWLOntology sourceOntology;

	private OWLOntologyManager sourceManager;

	private final SimpleModuleBuilder builder;

	private IRI moduleIri;

	private IRI includeIri;

	public IRI getModuleIri() {
		return moduleIri;
	}

	public IRI getIncludeIri() {
		return includeIri;
	}

	public IRI getExcludeIri() {
		return excludeIri;
	}

	public IRI getAnnotationIri() {
		return annotationIri;
	}

	private IRI excludeIri;

	private IRI annotationIri;

	/**
	 * @param moduleName
	 * @param sourceOntology
	 *            The source ontology for generating the module ontology. Any
	 *            needed ontologies should either be already loaded, or
	 *            accessible from the manager of this ontology (i.e. proper IRI
	 *            mapping is already setup in the manager).
	 * @param trunkPath
	 * @param outputPath
	 */
	public SimpleModule(String moduleName, OWLOntology sourceOntology, String trunkPath,
			String outputPath) {
		super(moduleName, trunkPath, outputPath);

		this.sourceOntology = sourceOntology;
		this.sourceManager = sourceOntology.getOWLOntologyManager();
		init();
		this.builder = new SimpleModuleBuilder(this);

	}

	private void init() {
		moduleManager = ISFUtil.setupManagerMapper(OWLManager.createOWLOntologyManager());
		annotationIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ Util.ANNOTATION_IRI_SUFFIX);
		moduleIri = IRI
				.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName() + Util.MODULE_IRI_SUFFIX);
		includeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ Util.MODULE_INCLUDE_IRI_SUFFIX);
		excludeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ Util.MODULE_EXCLUDE_IRI_SUFFIX);

		// annotation
		try {
			annotationOntology = moduleManager.loadOntology(annotationIri);
		} catch (OWLOntologyCreationException e1) {
			if (e1.getCause() instanceof FileNotFoundException) {
				System.out
						.println("Warning: SimpleModule didn't find the module annotation file for "
								+ getName());
			}
			throw new IllegalStateException("SimpleModule: failed to load annotation ontology.", e1);
		}

		// include
		try {
			includeOntology = moduleManager.loadOntology(includeIri);
		} catch (OWLOntologyCreationException e1) {
			if (e1.getCause() instanceof FileNotFoundException) {
				System.out.println("Warning: SimpleModule didn't find the module include file for "
						+ getName());
			}
			throw new IllegalStateException("SimpleModule: failed to load include ontology.", e1);
		}

		// exclude
		try {
			excludeOntology = moduleManager.loadOntology(excludeIri);
		} catch (OWLOntologyCreationException e1) {
			if (e1.getCause() instanceof FileNotFoundException) {
				System.out.println("Warning: SimpleModule didn't find the module exclude file for "
						+ getName());
			}
			throw new IllegalStateException("SimpleModule: failed to load exclude ontology.", e1);
		}

	}

	@Override
	public void generateModule() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	public OWLOntology getAnnotationOntology() {
		return annotationOntology;
	}

	public OWLOntology getExcludeOntology() {
		return excludeOntology;
	}

	public OWLOntology getIncludeOntology() {
		return includeOntology;
	}

	public OWLOntologyManager getModuleManager() {
		return moduleManager;
	}

	public OWLOntology getModuleOntology() {

		return null;
	}

	public OWLOntology getSourceOntology() {
		return sourceOntology;
	}

	public void setAnnotationOntology(OWLOntology annotationOntology) {
		this.annotationOntology = annotationOntology;
	}

	public void setExcludeOntology(OWLOntology excludeOntology) {
		this.excludeOntology = excludeOntology;
	}

	public void setIncludeOntology(OWLOntology includeOntology) {
		this.includeOntology = includeOntology;
	}

	public void setModuleManager(OWLOntologyManager moduleManager) {
		this.moduleManager = moduleManager;
	}

	public void setModuleOntology(OWLOntology moduleOntology) {
		this.moduleOntology = moduleOntology;
	}

	@Override
	public OWLReasoner getReasoner() {
		// TODO Auto-generated method stub
		OWLReasoner r = super.getReasoner();
		if (r == null) {
			r = new FaCTPlusPlusReasonerFactory().createReasoner(getSourceOntology());
		}

		return r;
	}
}
