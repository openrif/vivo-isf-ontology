package isf.command;

import static isf.command.EroCommand.Action.addlegacy;
import static isf.command.EroCommand.Action.cleanlegacy;
import static isf.command.EroCommand.Action.generate;
import isf.command.cli.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames = { "ero" }, commandDescription = "Creates the ERO modules.")
public class EroCommand extends AbstractCommand {

	@Parameter(
			names = "-cleanLegacy",
			description = "Will clean the legacy ERO files from any axioms the module is generating.")
	boolean cleanLegacy = false;

	@Parameter(names = "-addLegacy",
			description = "Will add the legacy ERO content to the generated module.")
	boolean addLegacy = false;

	public EroCommand(Main main) {
		super(main);
	}


	
	@Override
	public void run() {
		for (String action : getAllActions()) {
			switch (Action.valueOf(action.toLowerCase())) {
			case generate:
				generate.execute(this);
				break;
			case cleanlegacy:
				generate.execute(this);
				break;
			case addlegacy:
				generate.execute(this);
				break;
			case somthing:
				break;
			default:
				break;
			}
		}

	}

	@Override
	protected List<String> getDefaultActions() {

		return new ArrayList<String>(Arrays.asList(generate.name(), cleanlegacy.name(),
				addlegacy.name()));
	}

	enum Action {
		generate {
			@Override
			public void execute(EroCommand command) {

			}
		},
		addlegacy {
			@Override
			public void execute(EroCommand command) {

			}
		},
		cleanlegacy {
			@Override
			public void execute(EroCommand command) {

			}
		},
		somthing {
			@Override
			public void execute(EroCommand command) {
				// TODO Auto-generated method stub

			}
		};

		public abstract void execute(EroCommand command);
	}

}
