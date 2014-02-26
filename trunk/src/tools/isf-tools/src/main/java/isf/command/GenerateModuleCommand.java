package isf.command;

import java.util.List;

import static isf.command.GenerateModuleCommand.Action.*;

import com.beust.jcommander.Parameters;

import isf.command.cli.Main;

@Parameters(commandNames = "module",
		commandDescription = "Generate the named module. The module has to be already created.")
public class GenerateModuleCommand extends AbstractCommand {

	public GenerateModuleCommand(Main main) {
		super(main);
	}

	@Override
	public void run() {
		for (String action : getAllActions()) {
			switch (Action.valueOf(action)) {
			case generate:
				generate.execute(this);
				break;
			default:
				break;

			}
		}

	}

	@Override
	protected List<String> getDefaultActions() {
		// TODO Auto-generated method stub
		return null;
	}

	enum Action {
		generate {
			@Override
			public void execute(GenerateModuleCommand command) {
				// TODO Auto-generated method stub

			}
		};

		public abstract void execute(GenerateModuleCommand command);
	}

}
