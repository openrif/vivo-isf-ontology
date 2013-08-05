package isf.release.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;

public class Reporter {

	StringBuilder sb;
	String heading = "";
	private File file;
	private ManchesterOWLSyntaxObjectRenderer or;
	List<OWLAnnotationProperty> labelProperties = new ArrayList<OWLAnnotationProperty>();
	StringWriter sw = new StringWriter();

	public Reporter(File file) {
		this.file = file;
		sb = new StringBuilder();
		sb.append("Report file name: " + file.getName()).append("\n\n");
		labelProperties
				.add(OWLManager.getOWLDataFactory().getOWLAnnotationProperty(
						OWLRDFVocabulary.RDFS_LABEL.getIRI()));
	}

	public void setHeading(String heading) {
		this.heading = heading;
		sb.append("\n" + heading + "\n");
	}

	public void addLine(String line) {
		sb.append("\t" + line).append("\n");
	}

	public void save() throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		pw.print(sb.toString());
		pw.close();
	}

	public String renderOWLObject(OWLObject object ){
		object.accept(or);
		String rendered = sw.toString();
		sw.getBuffer().setLength(0);
		return rendered;
	}
	public void setManaagerForRenderer(OWLOntologyManager man) {
		AnnotationValueShortFormProvider sfp = new AnnotationValueShortFormProvider(
				labelProperties,
				new HashMap<OWLAnnotationProperty, List<String>>(), man);
		or = new ManchesterOWLSyntaxObjectRenderer(sw, sfp);
	}
}
