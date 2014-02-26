package isf.command;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import isf.command.cli.Main;

public abstract class AbstractCommand {

	public Main main;

	public AbstractCommand(Main main) {
		this.main = main;
	}

	public List<String> actions = getDefaultActions();

	public List<String> getActions() {
		return actions;
	}

	@Parameter(names = "-actions",
			description = "The exact sub-actions to execute the command. If this is "
					+ "specified, the default execution actions of the command are "
					+ "overridden " + "with the specified actions. An action might require some "
					+ "options and each command should document this.")
	public void setActions(List<String> actions) {
		this.actions = actions;
	}

	@Parameter(names = "-preActions",
			description = "Additional actions that could be taken before the execution "
					+ "of the command .")
	public List<String> preActions = new ArrayList<String>();

	@Parameter(names = "-postActions",
			description = "Additional actions that could be taken after the execution of "
					+ "the command.")
	public List<String> postActions = new ArrayList<String>();

	/**
	 * Returns a list of string values that represent the execution steps with
	 * any required input in the form of param1=value1. Warning, this method is
	 * called before the constructor is finished, be careful.
	 * 
	 * @return
	 */
	protected abstract List<String> getDefaultActions();

	public abstract void run();

	protected List<String> getAllActions() {
		List<String> allActions = new ArrayList<String>();
		allActions.addAll(preActions);
		allActions.addAll(actions);
		allActions.addAll(postActions);
		return allActions;
	}

}
