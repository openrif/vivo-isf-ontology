package isf.release;

import isf.ISFUtil;

import java.io.File;

public class _DoRelease {

	public _DoRelease() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {

		ReleaseBase.RELEASE_STRING = "2013-07-31";
		ReleaseBase.setReleaseDirectory(new File(ISFUtil.getSvnRootDir(),
				"release/local"));

		System.out.println("Generating unreasoned ISF file: isf.owl");
		ISF.main(null);
		System.out.println("Generating reasoned ISF file: isf-reasoned.owl");
		ISFReasoned.main(null);
		System.out.println("Generating module files");
		ISFModules.main(null);
		System.out.println("Generating SKOS file");
		ISFSkos.main(null);
	}

}
