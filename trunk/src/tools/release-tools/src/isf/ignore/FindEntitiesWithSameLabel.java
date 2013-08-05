package isf.ignore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import isf.ISFUtil;
import isf.ISFUtil.LabelInfo;

public class FindEntitiesWithSameLabel {

	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	OWLOntology isfOntology;
	Map<String, Set<LabelInfo>> labelMap = new HashMap<String, Set<LabelInfo>>();

	private void run() throws OWLOntologyCreationException {

		isfOntology = ISFUtil.setupAndLoadIsfOntology(man);

		for (OWLEntity entity : isfOntology.getSignature(true)) {
			for (LabelInfo info : ISFUtil.getLabels(entity.getIRI(),
					isfOntology.getImportsClosure())) {
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
		findMultipleLabelsPerEntityPerOntology();

	}

	private void findMultipleLabelsPerEntityPerOntology() {
		// TODO Auto-generated method stub

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
						System.out.println("\""+entry.getKey() + "\" is used by "
								+ info.axiom.getSubject() + " AND "
								+ otherInfo.axiom.getSubject());
					}
				}

			}
		}

	}

	public static void main(String[] args) throws OWLOntologyCreationException {
		new FindEntitiesWithSameLabel().run();

	}

}
