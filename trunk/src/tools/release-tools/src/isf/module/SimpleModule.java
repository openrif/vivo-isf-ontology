package isf.module;

import isf.ISFUtil;
import isf.module.internal.SimpleModuleBuilder;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OntologyIRIMappingNotFoundException;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class SimpleModule extends AbstractModule {

	private OWLOntology annotationOntology;

	private OWLOntology includeOntology;

	private OWLOntology excludeOntology;

	private OWLOntology ontology;

	private OWLOntology sourceOntology;

	private final SimpleModuleBuilder builder;

	private IRI moduleIri;

	private IRI includeIri;

	public IRI getIri() {
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

	private OWLOntologyChangeListener changeListener;

	/**
	 * @param moduleName
	 * @param moduleTrunkRelativePath
	 *            Can be null to use the default: src/ontology/module/moduleName
	 * @param sourceOntology
	 *            The source ontology for generating the module ontology. Any
	 *            needed ontologies should either be already loaded, or
	 *            accessible from the manager of this ontology (i.e. proper IRI
	 *            mapping is already setup in the manager).
	 * @param trunkPath
	 *            can be null if the environment variable ISF_TRUNK is set or if
	 *            the system property isf.trunk is set.
	 * @param outputPath
	 *            Can be null to use the default output of
	 *            trunk/../generated/module/moduleName
	 */
	public SimpleModule(String moduleName, String moduleTrunkRelativePath,
			OWLOntology sourceOntology, String trunkPath, String outputPath) {
		super(moduleName, moduleTrunkRelativePath, trunkPath, outputPath);

		this.sourceOntology = sourceOntology;
		init();
		this.builder = new SimpleModuleBuilder(this);

	}

	private final Set<OWLOntology> changedOntologies = new HashSet<OWLOntology>();

	private void init() {
		this.changeListener = new OWLOntologyChangeListener() {

			@Override
			public void ontologiesChanged(List<? extends OWLOntologyChange> changes)
					throws OWLException {
				for (OWLOntologyChange change : changes) {
					changedOntologies.add(change.getOntology());
				}

			}
		};

		ISFUtil.setupManagerMapper(getManager());
		getManager().addOntologyChangeListener(changeListener, null);

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
			annotationOntology = getManager().loadOntology(annotationIri);
		} catch (OWLOntologyCreationException e1) {
			if (e1.getCause() instanceof FileNotFoundException
					|| e1 instanceof OntologyIRIMappingNotFoundException) {
				System.err
						.println("Warning: SimpleModule didn't find the module annotation file for "
								+ getName() + ". Creating a new one.");
				annotationOntology = createOntology(annotationIri, getDirectory());

				// add the exclude import
				AddImport ai = new AddImport(annotationOntology, getDataFactory()
						.getOWLImportsDeclaration(excludeIri));
				getManager().applyChange(ai);

				// add the include import
				ai = new AddImport(annotationOntology, getDataFactory().getOWLImportsDeclaration(
						includeIri));
				getManager().applyChange(ai);

				// add the ISF import
				ai = new AddImport(annotationOntology, getDataFactory().getOWLImportsDeclaration(
						ISFUtil.ISF_IRI));
				getManager().applyChange(ai);

				try {
					getManager().saveOntology(annotationOntology);
				} catch (OWLOntologyStorageException e) {
					throw new RuntimeException(
							"Failed to save initial module annotation ontology.", e);
				}
			}

			else {
				throw new IllegalStateException(
						"Failed to create initial module annotation ontology", e1);
			}

		}

		// include
		try {
			includeOntology = getManager().loadOntology(includeIri);
		} catch (OWLOntologyCreationException e1) {
			if (e1.getCause() instanceof FileNotFoundException
					|| e1 instanceof OntologyIRIMappingNotFoundException) {
				System.err.println("Warning: SimpleModule didn't find the module include file for "
						+ getName() + ". Creating a new one.");
				includeOntology = createOntology(includeIri, getDirectory());
				try {
					getManager().saveOntology(includeOntology);
				} catch (OWLOntologyStorageException e) {
					throw new RuntimeException("Failed to save initial module include ontology.", e);
				}
			}

			else {
				throw new RuntimeException("Failed to create initial module include ontology", e1);
			}
		}

		// exclude
		try {
			excludeOntology = getManager().loadOntology(excludeIri);
		} catch (OWLOntologyCreationException e1) {
			if (e1.getCause() instanceof FileNotFoundException
					|| e1 instanceof OntologyIRIMappingNotFoundException) {
				System.err.println("Warning: SimpleModule didn't find the module exclude file for "
						+ getName() + ". Creating a new one.");
				excludeOntology = createOntology(excludeIri, getDirectory());
				try {
					getManager().saveOntology(excludeOntology);
				} catch (OWLOntologyStorageException e) {
					throw new RuntimeException("Failed to save initial module exclude ontology.", e);
				}
			}

			else {
				throw new RuntimeException("Failed to create initial module exclude ontology", e1);
			}
		}

		ontology = createOntology(moduleIri, getOutputDirectory());
//		try {
//			getManager().saveOntology(ontology);
//		} catch (OWLOntologyStorageException e) {
//			throw new RuntimeException("Failed to save module ontology.", e);
//		}

	}

	@Override
	public void generateModule() throws Exception {
		builder.run();

	}

	@Override
	public void generateModuleTransitive() throws Exception {
		for (Module module : getImports()) {
			module.generateModuleTransitive();
		}
		generateModule();
	}

	@Override
	public void addImport(Module module) {
		OWLImportsDeclaration id = getDataFactory().getOWLImportsDeclaration(module.getIri());
		AddImport i = new AddImport(getOntology(), id);
		getManager().applyChange(i);
		super.addImport(module);
	}

	@Override
	public void remoteImport(Module module) {

		OWLImportsDeclaration id = getDataFactory().getOWLImportsDeclaration(module.getIri());
		RemoveImport i = new RemoveImport(getOntology(), id);
		getManager().applyChange(i);
		super.remoteImport(module);
	}

	@Override
	public void saveGeneratedModule() throws OWLOntologyStorageException {
		getManager().saveOntology(ontology);

	}

	@Override
	public void saveGeneratedModuleTransitive() throws OWLOntologyStorageException {
		for (Module module : getImports()) {
			module.saveGeneratedModuleTransitive();
		}
		saveGeneratedModule();

	}

	@Override
	public void saveModuleDefinitionFiles() throws OWLOntologyStorageException {
		if (changedOntologies.remove(annotationOntology)) {
			getManager().saveOntology(annotationOntology);
		}
		if (changedOntologies.remove(includeOntology)) {
			getManager().saveOntology(includeOntology);
		}
		if (changedOntologies.remove(excludeOntology)) {
			getManager().saveOntology(excludeOntology);
		}
		saveModuleDefinitionFiles();
	}

	@Override
	public void saveModuleDefinitionFilesTransitive() throws OWLOntologyStorageException {
		for (Module module : getImports()) {
			module.saveModuleDefinitionFilesTransitive();
		}

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

	public OWLOntology getOntology() {

		return ontology;
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

	public void setOntology(OWLOntology moduleOntology) {
		this.ontology = moduleOntology;
	}

	@Override
	public OWLReasoner getReasoner() {
		OWLReasoner r = super.getReasoner();
		if (r == null) {
			r = ISFUtil.getDefaultReasoner(getSourceOntology());
		}

		return r;
	}

	public static void main(String[] args) throws Exception {
		String moduleName = args[0];
		String trunkPath = null;
		if (args.length > 1) {
			trunkPath = args[1];
		}
		String outputPath = null;
		if (args.length > 2) {
			outputPath = args[2];
		}

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = ISFUtil.setupAndLoadIsfOntology(man);
		SimpleModule module = new SimpleModule(moduleName, null, sourceOntology, trunkPath,
				outputPath);
		module.generateModule();
		module.saveGeneratedModule();
		module.saveModuleDefinitionFiles();
	}
}
