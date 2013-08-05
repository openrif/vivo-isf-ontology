package isf.release.action;

import isf.ISFUtil;
import isf.ISFUtil.LabelInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class DuplicateLabelCheck extends Action {

	Map<String, Set<LabelInfo>> labelMap = new HashMap<String, Set<LabelInfo>>();

	@Override
	public void doAction(OWLOntology ontology, OWLOntologyManager man,
			Reporter reporter) {
		super.doAction(ontology, man, reporter);

		reporter.setHeading("Duplicate label check:");

		for (OWLEntity entity : ontology.getSignature(true)) {

			Set<LabelInfo> infos = ISFUtil.getLabels(entity.getIRI(),
					ontology.getImportsClosure());

//			if(entity.getIRI().toString().endsWith("creator")){
//				System.out.println("Debug");
//			}
			
			if (infos.size() == 0) {
				int i = entity.getIRI().toString().lastIndexOf('/');
				String key = entity.getIRI().toString().substring(i + 1)
						.toLowerCase();
				Set<LabelInfo> value = labelMap.get(key);
				if (value == null) {
					value = new HashSet<LabelInfo>();
					labelMap.put(key, value);
				}
				value.add(new LabelInfo(
						ontology,
						man.getOWLDataFactory()
								.getOWLAnnotationAssertionAxiom(
										entity.getIRI(),
										man.getOWLDataFactory()
												.getOWLAnnotation(
														man.getOWLDataFactory()
																.getOWLAnnotationProperty(
																		IRI.create("http://no-property")),
														man.getOWLDataFactory()
																.getOWLLiteral(
																		key)))));
			}

			for (LabelInfo info : ISFUtil.getLabels(entity.getIRI(),
					ontology.getImportsClosure())) {
				String key = ((OWLLiteral) info.axiom.getValue()).getLiteral()
						.toLowerCase();
				Set<LabelInfo> value = labelMap.get(key);
				if (value == null) {
					value = new HashSet<LabelInfo>();
					labelMap.put(key, value);
				}
				value.add(info);
			}
		}
		
		findSimilarLabelForDifferentEntities();

	}

	private void findSimilarLabelForDifferentEntities() {

		for (Entry<String, Set<LabelInfo>> entry : labelMap.entrySet()) {
			Set<LabelInfo> infos = entry.getValue();
			Iterator<LabelInfo> i = infos.iterator();
			while (i.hasNext()) {
				LabelInfo info = i.next();
				i.remove();
				for (LabelInfo otherInfo : infos) {
					if (!otherInfo.axiom.getSubject().equals(
							info.axiom.getSubject())) {
						reporter.addLine("Label: "
								+ entry.getKey()
								+ " is used by: "
								+ reporter.renderOWLObject(info.axiom
										.getSubject())
								+ " and by "
								+ reporter.renderOWLObject(otherInfo.axiom
										.getSubject()));
					}
				}

			}
		}

	}
}
