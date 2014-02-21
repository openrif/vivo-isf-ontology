package isf.tmp;

import isf.ISFUtil;

import org.slf4j.Logger;

public class TryLog {

	public static void main(String[] args) {
		Logger logger = ISFUtil.getLogger("TestLogger");
		logger.info("INFO test");
		logger.debug("debug message.");
	}
}
