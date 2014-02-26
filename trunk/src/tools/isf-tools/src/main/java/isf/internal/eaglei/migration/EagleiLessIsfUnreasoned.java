package isf.internal.eaglei.migration;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.coode.owlapi.rdf.rdfxml.RDFXMLRenderer;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;

public class EagleiLessIsfUnreasoned {

	OWLOntologyManager eagleiMan;
	OWLDataFactory df;
	OWLOntology eagleiOntology;
	OWLOntology isfOntology;
	OWLOntology ignoreOntology;
	private StringWriter xmlRendererWriter;
	private RDFXMLRenderer xmlRenderer;
	private OWLOntologyManager isfManager;
	private StringWriter manchesterWriter;
	private ManchesterOWLSyntaxObjectRenderer manchesterRenderer;
	private RDFXMLOntologyFormat of;
	private DocumentBuilderFactory dbf;
	private Transformer t;

	public void run() throws Exception {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		eagleiMan = Util.getNewManager();
		Util.prepareEagleiTrunkManager(eagleiMan);
		isfManager = Util.getNewManager();
		Util.prepareIsfTrunkManager(isfManager);

		df = eagleiMan.getOWLDataFactory();

		eagleiOntology = Util.getEagleiExtendedTrunk(eagleiMan);
		isfOntology = Util.getIsfOntology(isfManager);
		ignoreOntology = Util.getIgnoreOntology(isfManager);

		createManchesterObjectRenderer();
		createXmlRenderer();
		of = new RDFXMLOntologyFormat();
		of.setAddMissingTypes(false);

		List<OWLAxiom> missingAxioms = new ArrayList<OWLAxiom>();
		List<OWLAxiom> changedAxioms = new ArrayList<OWLAxiom>();

		Set<OWLAxiom> eagleiAxioms = new HashSet<>();
		for (OWLOntology o : eagleiOntology.getImportsClosure()) {
			eagleiAxioms.addAll(o.getAxioms());
		}

		int inCounter = 0;
		for (OWLAxiom axiom : eagleiAxioms) {
			if (!isfOntology.containsAxiom(axiom, true) && !ignoreOntology.containsAxiom(axiom)) {
				if (axiom instanceof OWLAnnotationAssertionAxiom) {
					OWLAnnotationAssertionAxiom aaa = (OWLAnnotationAssertionAxiom) axiom;
					if (aaa.getValue() instanceof OWLLiteral) {
						OWLLiteral literal = (OWLLiteral) aaa.getValue();
						OWLAnnotationAssertionAxiom newAaa = df
								.getOWLAnnotationAssertionAxiom(aaa.getSubject(), df
										.getOWLAnnotation(aaa.getProperty(), df.getOWLLiteral(
												literal.getLiteral().toLowerCase(), "en")));

						if (!isfOntology.containsAxiom(newAaa, true)) {
							missingAxioms.add(axiom);
						} else {
							changedAxioms.add(axiom);
						}
					}
				} else {
					missingAxioms.add(axiom);
				}
			} else {
				// in isf
				++inCounter;
			}

		}

		Collections.sort(changedAxioms);
		Collections.sort(missingAxioms);

		System.out.println("Missing axioms count: " + missingAxioms.size() + " same count: "
				+ inCounter);

		for (OWLAxiom axiom : missingAxioms) {
			System.out.println(axiom);
			// manchester
			axiom.accept(manchesterRenderer);
			String s = manchesterWriter.toString();
			System.out.println(clearIri(s));
			manchesterWriter.getBuffer().setLength(0);
			// xml
			StringWriter sw = new StringWriter();
			OWLOntology o = isfManager.createOntology();
			isfManager.addAxiom(o, axiom);
			RDFXMLRenderer r = new RDFXMLRenderer(o, sw, of);
			r.render();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(new ByteArrayInputStream(sw.toString().getBytes("UTF-8")));
			Node node = document.getFirstChild();
			NodeList nodes = node.getChildNodes();
			for (int i = 0; i < nodes.getLength(); ++i) {
				Node child = nodes.item(i);
				child.getNodeType();
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					StreamResult sr = new StreamResult(new StringWriter());
					DOMSource ds = new DOMSource(child);
					t.transform(ds, sr);
					System.out.println(sr.getWriter().toString());
				}
			}

			System.out.println();
		}

		OWLOntology missingOntology = isfManager.createOntology();
		isfManager.addAxioms(missingOntology, new HashSet<OWLAxiom>(missingAxioms));
		isfManager.saveOntology(missingOntology, new FileOutputStream("missingOntology.owl"));

		OWLOntology changedOntology = isfManager.createOntology();
		isfManager.addAxioms(changedOntology, new HashSet<OWLAxiom>(changedAxioms));
		isfManager.saveOntology(changedOntology, new FileOutputStream("changedOntology.owl"));

		System.out.println("Changed annotation axioms count: " + changedAxioms.size());

		System.exit(0);
		for (OWLAxiom axiom : changedAxioms) {
			System.out.println(axiom);

		}

	}

	Pattern patter = Pattern.compile("^<(.*?)>");

	private String clearIri(String s) {
		if (s.startsWith("<http")) {
			Matcher m = patter.matcher(s);
			if (m.find()) {
				int last = m.end() + 1;
				String iriString = m.group(1);
				IRI iri = IRI.create(iriString);

				outer: for (OWLOntology o : eagleiOntology.getImportsClosure()) {
					for (OWLAnnotationAssertionAxiom aaa : o.getAnnotationAssertionAxioms(iri)) {
						if (aaa.getProperty().getIRI().equals(OWLRDFVocabulary.RDFS_LABEL.getIRI())) {
							String label = ((OWLLiteral) aaa.getValue()).getLiteral();
							s = label + " " + s.substring(last);
							break outer;
						}
					}
				}
			}
		}
		return s;
	}

	void createXmlRenderer() {
		RDFXMLOntologyFormat of = new RDFXMLOntologyFormat();
		of.setAddMissingTypes(false);
		xmlRendererWriter = new StringWriter();
		xmlRenderer = new RDFXMLRenderer(eagleiOntology, xmlRendererWriter, of);
	}

	void xmlRender(OWLEntity entity) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = RDFXMLRenderer.class.getMethod("renderEntity", OWLEntity.class);
		method.setAccessible(true);
		method.invoke(xmlRenderer, entity);
	}

	void createManchesterObjectRenderer() {
		manchesterWriter = new StringWriter();
		manchesterRenderer = new ManchesterOWLSyntaxObjectRenderer(manchesterWriter, getShortForm());
	}

	AnnotationValueShortFormProvider getShortForm() {

		OWLAnnotationProperty ap = df
				.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		AnnotationValueShortFormProvider sf = new AnnotationValueShortFormProvider(
				Collections.singletonList(ap), new HashMap<OWLAnnotationProperty, List<String>>(),
				eagleiMan);

		return sf;
	}

	public static void main(String[] args) throws Exception {

		new EagleiLessIsfUnreasoned().run();

	}

}
