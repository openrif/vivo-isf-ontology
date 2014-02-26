package isf.command.cli;

import java.io.File;

import com.beust.jcommander.IParameterValidator2;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

public class DirectoryExistsValidator implements IParameterValidator2 {

	@Override
	public void validate(String name, String value) throws ParameterException {
		validate(name, value, null);

	}

	@Override
	public void validate(String name, String value, ParameterDescription pd)
			throws ParameterException {
		File directory = new File(value);

		if (!directory.isDirectory()) {
			throw new ParameterException("Parameter " + name
					+ " is not set to a valid directory. Value: " + value);
		}

	}

}
