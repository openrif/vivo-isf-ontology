package isf.command.cli;

import isf.ISFUtil;
import isf.command.NewModuleCommand;
import isf.command.EroCommand;
import isf.command.GenerateModuleCommand;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Main {

	private static final String PROGRAM_DESC = "This is the ISF Tools program with various "
			+ "(in development) commands and options.\n";

	static {
		ISFUtil.setLoggingLevel("info");
	}

	@Parameter(
			names = "-trunk",
			validateWith = CreateDirectoryValidator.class,
			converter = CanonicalFileConverter.class,
			description = "The directory location of the ISF SVN trunk directory (Git wroking tree) if not"
					+ "specified in some other way (system property, evn, or isf.properties file).")
	public void setISFTrunkDirecotry(File isfTrunkDirectory) {
		ISFUtil.setISFTrunkDirecotry(isfTrunkDirectory);
	}

	public File getISFTrunkDirecotry() {
		return ISFUtil.ISF_TRUNK_DIR;
	}

	@Parameter(names = "-output", validateWith = CreateDirectoryValidator.class,
			converter = CanonicalFileConverter.class,
			description = "The top directory for output. By default it will be \"/generated\" "
					+ "under the trunk directory.")
	public void setOutputDirectory(File outputDirectory) {
		ISFUtil.setGeneratedDirectory(outputDirectory);
	}

	public File getOutputDirectory() {
		return ISFUtil.getGeneratedDirectory();
	}

	@Parameter(names = "-datedOutput", description = "Whether or not to create dated_time "
			+ "sub directories in the output directory.")
	public void setDatedOutput(boolean datedOutput) {
		ISFUtil.datedGenerated = datedOutput;
		ISFUtil.setGeneratedDirectory(null);
	}

	public boolean getDatedOutput() {
		return ISFUtil.datedGenerated;
	}

	@Parameter(names = "-loglevel",
			description = "The logging level. Valid values include warn, info, and debug.")
	public void setLogLevel(String logLevel) {
		ISFUtil.setLoggingLevel(logLevel);
	}

	public String getLogLevel() {
		return ISFUtil.getLoggingLevel();
	}

	private void run(String[] args) {

		JCommander jc = new JCommander();
		jc.setAllowAbbreviatedOptions(true);
		jc.setCaseSensitiveOptions(false);
		jc.setProgramName("java -jar isf-tools-*.jar");

		Main main = new Main();
		jc.addObject(main);

		EroCommand ero = new EroCommand(main);
		jc.addCommand("ero", ero);

		NewModuleCommand newModule = new NewModuleCommand(main);
		jc.addCommand("newModule", newModule);

		GenerateModuleCommand module = new GenerateModuleCommand(main);
		jc.addCommand("module", module);

		if (args.length == 0) {
			System.out.println(PROGRAM_DESC);
			jc.usage();
			return;
		}

		try {
			jc.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			System.out.println(PROGRAM_DESC);
			jc.usage();
		}

		String command = jc.getParsedCommand();
		if (command.equalsIgnoreCase("newModule")) {
			newModule.run();
		} else if (command.equalsIgnoreCase("module")) {
			module.run();
		} else if (command.equalsIgnoreCase("ero")) {
			ero.run();
		}

	}

	public static void main(String[] args) {
		new Main().run(args);
	}

}
