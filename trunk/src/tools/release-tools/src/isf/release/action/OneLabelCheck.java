package isf.release.action;

import isf.ISFUtil;
import isf.ISFUtil.LabelInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class OneLabelCheck extends Action {

	@Override
	public void doAction(OWLOntology ontology, OWLOntologyManager man,
			Reporter reporter) {
		super.doAction(ontology, man, reporter);

		reporter.setHeading("One label check:");

		List<OWLEntity> entities = ISFUtil.getEntitiesSortedByIri(ontology,
				true);

		for (OWLEntity entity : entities) {
			Set<LabelInfo> labelInfos = ISFUtil.getLabels(entity.getIRI(),
					Collections.singleton(ontology));

			if (labelInfos.size() == 0) {
				reporter.addLine("Missing label for "
						+ reporter.renderOWLObject(entity) + " IRI: "
						+ entity.getIRI());
			} else if (labelInfos.size() > 1) {
				reporter.addLine("Multiple labels for "
						+ reporter.renderOWLObject(entity) + " IRI: "
						+ entity.getIRI() + " " + labelInfos);
			}
		}
	}

}
