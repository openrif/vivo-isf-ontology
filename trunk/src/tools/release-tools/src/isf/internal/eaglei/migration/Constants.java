package isf.internal.eaglei.migration;

public class Constants {

	// public static String DROPBOX_ROOT =
	// "C:/s/Dropbox/_shared/eaglei-mirgration";
	// public static String EAGLEI_ROOT = "C:/s/svns/eaglei-isf-migration";
	// public static String ISF_ROOT = "C:/s/svns/isf-new-layout";
	// public static String EAGLEI_RELEASE_UNREASONED_PATH =
	// "releases/2013-08-02/ero-unreasoned.owl";
	// public static String ISF_RELEASE_UNREASONED_PATH =
	// "release/2013-07-31/isf.owl";

	private static String EAGLEI_SVN_TRUNK_PATH = null;

	static {
		EAGLEI_SVN_TRUNK_PATH = System.getProperty("eaglei.svn.trunk.dir");

	}

	public static String getTrunkPath() {
		return EAGLEI_SVN_TRUNK_PATH;
	}

}
