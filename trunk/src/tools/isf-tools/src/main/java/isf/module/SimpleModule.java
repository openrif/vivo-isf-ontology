package isf.module;

import isf.ISFUtil;
import isf.module.internal.SimpleModuleBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.io.OntologyIRIMappingNotFoundException;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleModule extends AbstractModule {

	private static final Logger logger = LoggerFactory.getLogger("SimpleModule");

	private OWLOntology annotationOntology;

	private OWLOntology includeOntology;

	private OWLOntology excludeOntology;

	private OWLOntology legacyOntology;

	private OWLOntology ontology;

	// private OWLOntology sourceOntology;

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

	private final Set<OWLOntology> changedOntologies = new HashSet<OWLOntology>();

	private IRI legacyIri;

	private IRI legacyRemovedIri;

	private OWLOntology legacyRemovedOntology;

	private HashSet<OWLOntology> legacyOntologies;

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
	public SimpleModule(String moduleName, OWLOntologyManager mananger, File directory,
			File outputDirectory) {
		super(moduleName, mananger, directory, outputDirectory);
		this.builder = new SimpleModuleBuilder(this);

		annotationIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ ISFUtil.ANNOTATION_IRI_SUFFIX);
		moduleIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ ISFUtil.MODULE_IRI_SUFFIX);
		includeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ ISFUtil.MODULE_INCLUDE_IRI_SUFFIX);
		excludeIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ ISFUtil.MODULE_EXCLUDE_IRI_SUFFIX);
		legacyIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ ISFUtil.MODULE_LEGACY_IRI_SUFFIX);
		legacyRemovedIri = IRI.create(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + getName()
				+ ISFUtil.MODULE_LEGACY_REMOVED_IRI_SUFFIX);

		this.changeListener = new OWLOntologyChangeListener() {

			@Override
			public void ontologiesChanged(List<? extends OWLOntologyChange> changes)
					throws OWLException {
				Map<OWLOntology, Set<OWLOntologyChange>> changeMap = new HashMap<OWLOntology, Set<OWLOntologyChange>>(
						changes.size());
				for (OWLOntologyChange change : changes) {
					changedOntologies.add(change.getOntology());
					Set<OWLOntologyChange> ontologyChanges = changeMap.get(change.getOntology());
					if (ontologyChanges == null) {
						ontologyChanges = new HashSet<OWLOntologyChange>();
						changeMap.put(change.getOntology(), ontologyChanges);
					}
					ontologyChanges.add(change);
				}

				for (Entry<OWLOntology, Set<OWLOntologyChange>> entry : changeMap.entrySet()) {
					logger.info("Ontology changed: " + entry.getKey().getOntologyID());
					for (OWLOntologyChange change : entry.getValue()) {
						logger.debug("\t" + change.toString());
					}

				}
			}
		};

		getManager().addOntologyChangeListener(changeListener);
		ontology = createOntology(moduleIri, getOutputDirectory());

	}

	public boolean exists() {
		OWLOntology ontology = getManager().getOntology(annotationIri);
		if (ontology == null) {
			try {
				getManager().loadOntology(annotationIri);
				return true;
			} catch (OntologyIRIMappingNotFoundException e) {
				return false;
			} catch (OWLOntologyCreationException e) {
				throw new RuntimeException(
						"Error while checking or the existence of a SimpleModule with IRI "
								+ annotationIri + " and directory: " + getDirectory(), e);
			}
		} else {
			return true;
		}
	}

	public void create(IRI generatedFinalIri, Set<IRI> sourceIris, boolean legacy) {
		if (!exists()) {

			// annotation ontology
			annotationOntology = createOntology(annotationIri, getDirectory());
			// add the exclude import
			AddImport ai = new AddImport(annotationOntology, getDataFactory()
					.getOWLImportsDeclaration(excludeIri));
			getManager().applyChange(ai);
			// add the include import
			ai = new AddImport(annotationOntology, getDataFactory().getOWLImportsDeclaration(
					includeIri));
			getManager().applyChange(ai);
			// add the source imports
			for (IRI iri : sourceIris) {
				ai = new AddImport(annotationOntology, getDataFactory().getOWLImportsDeclaration(
						iri));
				getManager().applyChange(ai);
				// save the sources as ontology annotations
				OWLLiteral source = getDataFactory().getOWLLiteral(iri.toString());
				OWLAnnotation a = getDataFactory().getOWLAnnotation(
						getDataFactory().getOWLAnnotationProperty(
								IRI.create(ISFUtil.MODULE_SOURCE_ANNOTATION_IRI)), source);
				AddOntologyAnnotation aoa = new AddOntologyAnnotation(annotationOntology, a);
				getManager().applyChange(aoa);
			}
			// add the final IRI annotation
			OWLLiteral finalIriLiteral = getDataFactory().getOWLLiteral(
					generatedFinalIri.toString());
			OWLAnnotation a = getDataFactory().getOWLAnnotation(
					getDataFactory().getOWLAnnotationProperty(
							IRI.create(ISFUtil.MODULE_FINAL_IRI_ANNOTATION_IRI)), finalIriLiteral);
			AddOntologyAnnotation aoa = new AddOntologyAnnotation(annotationOntology, a);
			getManager().applyChange(aoa);

			// the include ontology
			includeOntology = createOntology(includeIri, getDirectory());
			// the exclude ontology
			excludeOntology = createOntology(excludeIri, getDirectory());

			try {
				saveOntology(annotationOntology);
				saveOntology(includeOntology);
				saveOntology(excludeOntology);
			} catch (OWLOntologyStorageException e) {
				throw new RuntimeException(
						"Failed to save ontologies for newly created SimpleModule", e);
			}

			if (legacy) {
				legacyOntology = createOntology(legacyIri, getDirectory());
				legacyRemovedOntology = createOntology(legacyRemovedIri, getDirectory());
				try {
					saveOntology(legacyOntology);
					saveOntology(legacyRemovedOntology);
				} catch (OWLOntologyStorageException e) {
					throw new RuntimeException(
							"Failed to save legacy ontologies for newly created SimpleModule", e);
				}
			}

		} else {
			throw new IllegalStateException(
					"Attempting to create an already existing SimpleModule named: " + getName());
		}
	}

	public void load(boolean legacy) {
		if (exists()) {
			annotationOntology = ISFUtil.getOrLoadOntology(annotationIri, getManager());
			includeOntology = ISFUtil.getOrLoadOntology(includeIri, getManager());
			excludeOntology = ISFUtil.getOrLoadOntology(excludeIri, getManager());
			if (legacy) {
				legacyOntology = ISFUtil.getOrLoadOntology(legacyIri, getManager());
				legacyRemovedOntology = ISFUtil.getOrLoadOntology(legacyRemovedIri, getManager());
			}
		} else {
			throw new IllegalStateException(
					"Attempting to load a non-existing SimpleModule named: " + getName());

		}
	}

	@Override
	public void close() {
		// TODO do real cleanup.

	}

	@Override
	public void generateModule() throws Exception {
		logger.info("Generating module: " + getName());
		builder.run();

	}

	public Set<OWLOntology> getSources() {
		Set<OWLOntology> sources = new HashSet<OWLOntology>(annotationOntology.getImports());
		sources.remove(includeOntology);
		sources.remove(excludeOntology);
		return sources;
	}

	@Override
	public void addLegacyOntologies() {
		if (legacyOntology != null) {
			Set<OWLAxiom> axioms = ISFUtil.getAxioms(legacyOntology, true);
			getManager().addAxioms(ontology, axioms);
			logger.info("Added legacy axioms for " + getName() + ", axiom count: " + axioms.size());
			// TODO do the debug logging
		}
	}

	@Override
	public void addLegacyOntologiesTransitive() {
		logger.info("Adding transitive legacy axioms for " + getName());
		for (Module module : getImports()) {
			module.addLegacyOntologiesTransitive();
		}
		addLegacyOntologies();
	}

	@Override
	public void cleanLegacyOntologies() {
		if (legacyOntology != null) {
			for (OWLOntology o : legacyOntology.getImportsClosure()) {
				Set<OWLAxiom> axioms = ontology.getAxioms();
				List<OWLOntologyChange> changes = getManager().removeAxioms(o, axioms);
				logger.info("Cleaned legacy ontology: " + o.getOntologyID() + ", change count: "
						+ changes.size());

				for (OWLOntologyChange change : changes) {
					getManager().addAxiom(legacyRemovedOntology, change.getAxiom());
				}
			}
		}
	}

	@Override
	public void cleanLegacyOntologiesTransitive() {
		logger.info("Cleaning transitive legacy axioms for " + getName());
		for (Module module : getImports()) {
			module.cleanLegacyOntologiesTransitive();
		}
		cleanLegacyOntologies();
	}

	@Override
	public void generateModuleTransitive() throws Exception {
		logger.info("Generating module transitive for: " + getName());
		for (Module module : getImports()) {
			module.generateModuleTransitive();
		}
		generateModule();
	}

	@Override
	public void addImport(Module module) {
		OWLImportsDeclaration id = getDataFactory().getOWLImportsDeclaration(module.getIri());
		AddImport i = new AddImport(getOntology(), id);
		logger.info("Adding module import for module: " + module.getName() + " imported into: "
				+ getName());
		getManager().applyChange(i);
		super.addImport(module);
	}

	@Override
	public void removeImport(Module module) {

		OWLImportsDeclaration id = getDataFactory().getOWLImportsDeclaration(module.getIri());
		RemoveImport i = new RemoveImport(getOntology(), id);
		logger.info("Removing module import for module: " + module.getName()
				+ " was imported into: " + getName());
		getManager().applyChange(i);
		super.removeImport(module);
	}

	@Override
	public void saveGeneratedModule() throws OWLOntologyStorageException {
		logger.info("Saving module: " + getName() + " into ontology: " + ontology.getOntologyID());
		saveOntology(ontology);

	}

	@Override
	public void saveGeneratedModuleTransitive() throws OWLOntologyStorageException {
		logger.info("Saving module " + getName() + " transitively");
		for (Module module : getImports()) {
			module.saveGeneratedModuleTransitive();
		}
		saveGeneratedModule();

	}

	@Override
	public void saveModuleDefinitionFiles() throws OWLOntologyStorageException {
		if (changedOntologies.remove(annotationOntology)) {
			logger.info("Saving annotation ontology for module: " + getName() + " into ontology: "
					+ annotationOntology.getOntologyID());
			saveOntology(annotationOntology);
		}
		if (changedOntologies.remove(includeOntology)) {
			logger.info("Saving include ontology for module: " + getName() + " into ontology: "
					+ annotationOntology.getOntologyID());
			saveOntology(includeOntology);
		}
		if (changedOntologies.remove(excludeOntology)) {
			logger.info("Saving exclude ontology for module: " + getName() + " into ontology: "
					+ annotationOntology.getOntologyID());
			saveOntology(excludeOntology);
		}
	}

	@Override
	public void saveModuleDefinitionFilesTransitive() throws OWLOntologyStorageException {
		logger.info("Saving definition files transitively for: " + getName());
		for (Module module : getImports()) {
			module.saveModuleDefinitionFilesTransitive();
		}
		saveModuleDefinitionFiles();
	}

	@Override
	public void saveLegacyOntologies() throws OWLOntologyStorageException {
		for (OWLOntology o : legacyOntologies) {
			if (changedOntologies.remove(o)) {
				logger.info("Module: " + getName() + " is saving legacy ontology: "
						+ o.getOntologyID());
				saveOntology(o);
			}
		}
	}

	@Override
	public void saveLegacyOntologiesTransitive() throws OWLOntologyStorageException {
		logger.info("Saving legacy ontologies transitively for module: " + getName());
		for (Module module : getImports()) {
			module.saveLegacyOntologies();
		}
		saveLegacyOntologies();
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

	// public OWLOntology getSourceOntology() {
	// return sourceOntology;
	// }

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
		return super.getReasoner();

	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return moduleIri.toString() + ontology;
	}
}
