package isf.release;

import isf.ISFUtil;
import isf.release.action.Reporter;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class ReleaseBase {

	public static String RELEASE_STRING = "dev";
	public static File RELEASE_DIR = new File(ISFUtil.getTrunkDirectory(),
			"generated/release/local");
	static {
		RELEASE_DIR.mkdirs();
	}

	public static void setReleaseDirectory(File directory) {
		directory.mkdirs();
		RELEASE_DIR = directory;
	}

	public ReleaseBase(Reporter reporter) {
		this.reporter = reporter;
	}

	Reporter reporter;

	public void release() throws Exception {
	}

	static OWLOntologyManager getManager() {
		return OWLManager.createOWLOntologyManager();
	}

	OWLDataFactory getDataFactory() {
		return OWLManager.getOWLDataFactory();
	}

	IRI getVersionedIri(IRI iri) {
		String iriSuffix = iri.toString().substring(ISFUtil.ISF_ONTOLOGY_IRI_PREFIX.length());
		String versioned = ISFUtil.ISF_ONTOLOGY_IRI_PREFIX + "release/" + RELEASE_STRING + "/"
				+ iriSuffix;

		return IRI.create(versioned);
	}

}
