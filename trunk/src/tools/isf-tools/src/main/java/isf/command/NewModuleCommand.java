package isf.command;

import isf.ISFUtil;
import isf.command.cli.CanonicalFileConverter;
import isf.command.cli.IriConverter;
import isf.command.cli.Main;
import static isf.command.NewModuleCommand.Action.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames = "newModule", commandDescription = "The command to create a new module.")
public class NewModuleCommand extends AbstractCommand {

	@Parameter(
			names = "-name",
			description = "The module name. This will be used to create default IRIs, files, and folders.")
	public String moduleName = "default-new-module";

	@Parameter(
			names = "-sourceIris",
			converter = IriConverter.class,
			description = "The source IRIs that will be used for this module. The OWL documents for the "
					+ "IRIs will first be looked for under the ISF trunk/src/ontology directory and then "
					+ "an attempt will be made to resolve online.")
	public List<IRI> sourceIris = getDefaultSources();

	@Parameter(names = "-iri", description = "The generated module's IRI",
			converter = IriConverter.class)
	public IRI iri;

	@Parameter(names = "-directory", converter = CanonicalFileConverter.class,
			description = "The location where the module will be created.")
	public File directory = getDefaultDirectory();

	@Parameter(names = "-legacy",
			description = "If this option is set, template OWL files will be created "
					+ "to work with legacy OWL files.")
	public boolean legacy = false;

	public NewModuleCommand(Main main) {
		super(main);
	}

	private File getDefaultDirectory() {
		return new File(ISFUtil.getTrunkDirectory(), "src/ontology/module/default-new-module");
	}

	private List<IRI> getDefaultSources() {
		List<IRI> iris = new ArrayList<IRI>();
		iris.add(ISFUtil.ISF_DEV_IRI);
		return iris;
	}

	@Override
	protected List<String> getDefaultActions() {
		List<String> actions = new ArrayList<String>();
		actions.add(Action.create.name());
		return actions;
	}

	public OWLOntologyManager man;

	public OWLOntologyManager getManager() {
		if (man == null) {
			man = OWLManager.createOWLOntologyManager();
		}
		return man;
	}

	@Override
	public void run() {
		for (String action : getAllActions()) {
			switch (Action.valueOf(action)) {
			case create:
				create.execute(this);
				break;
			default:
				break;

			}
		}

	}

	public enum Action {
		create {
			@Override
			public void execute(NewModuleCommand command) {
				// TODO Auto-generated method stub

			}
		};

		public abstract void execute(NewModuleCommand command);
	}

}
