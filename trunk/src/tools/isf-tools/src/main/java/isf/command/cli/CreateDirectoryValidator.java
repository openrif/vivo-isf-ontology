package isf.command.cli;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.IParameterValidator2;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

public class CreateDirectoryValidator implements IParameterValidator2 {

	@Override
	public void validate(String name, String value) throws ParameterException {
		validate(name, value, null);

	}

	@Override
	public void validate(String name, String value, ParameterDescription pd)
			throws ParameterException {
		File directory;
		try {
			directory = new File(value).getCanonicalFile();
		} catch (IOException e) {
			throw new ParameterException(e);
		}

		if (!directory.mkdirs()) {
			throw new ParameterException("Was not able to create directory for parameter " + name
					+ " with value " + value);
		}

	}

}
