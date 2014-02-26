package isf.command.cli;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class CanonicalFileConverter implements IStringConverter<File> {

	@Override
	public File convert(String value) {

		File canonicalFile = null;

		try {
			canonicalFile = new File(value).getCanonicalFile();
		} catch (IOException e) {
			throw new ParameterException(e);
		}

		return canonicalFile;
	}

}
