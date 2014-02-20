package isf.action;

import isf.ISFUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

/**
 * Takes a mapping file that has two values per line (space or tab separated)
 * that represent an IRI mapping. The values are not enclosed in <>. This action
 * can then do a right to left, or left to right conversion of the IRIs in the
 * passed ontology or ontologies and return a copy of the results. Results are
 * reset with each use.
 * 
 * A simple check is done on the mapping file to make sure it has the correct
 * syntax and that there are no duplicate mappings. The problematic
 * lines/mappings are ignored but a warning is printed to the console.
 * 
 * Not tested yet.
 * 
 * @author essaids
 * 
 */
public class IriMappingAction extends AbstractAction {

	private String mappingName;
	private boolean isLeftToRight;
	private Map<IRI, IRI> mappings;

	IriMappingAction(String mappingName, boolean isLeftToRight) throws IOException,
			OWLOntologyCreationException {
		this.mappingName = mappingName;
		this.isLeftToRight = isLeftToRight;
		this.mappings = ISFUtil.getMappings(mappingName, isLeftToRight);
		// loadMappingFile();
	}

	// private Map<IRI, IRI> leftToRightMappings;
	// private Map<IRI, IRI> rightToLeftMappings;
	//
	// public void loadMappingFile() throws IOException {
	// leftToRightMappings = new HashMap<IRI, IRI>();
	// rightToLeftMappings = new HashMap<IRI, IRI>();
	//
	// BufferedReader br = new BufferedReader(new FileReader(mappingFile));
	//
	// String line = null;
	// int counter = 0;
	// while ((line = br.readLine()) != null) {
	// ++counter;
	// String[] mapping = line.split("[ \t]");
	//
	// if (mapping.length != 2) {
	// System.err.println("Warning: mapping line " + counter + " " + line +
	// ", does ");
	// }
	//
	// IRI[] iriMapping = new IRI[2];
	// iriMapping[0] = IRI.create(mapping[0].trim());
	// iriMapping[1] = IRI.create(mapping[1].trim());
	//
	// if (leftToRightMappings.get(iriMapping[0]) != null) {
	// System.err
	// .println("Warning: IRI " + iriMapping[0]
	// + " already has left-to-right mapping "
	// + leftToRightMappings.get(iriMapping[0]) + ", will ignore "
	// + iriMapping[1]);
	// }
	//
	// if (rightToLeftMappings.get(iriMapping[1]) != null) {
	// System.err
	// .println("Warning: IRI " + iriMapping[1]
	// + " already has right-to-left mapping "
	// + rightToLeftMappings.get(iriMapping[1]) + ", will ignore "
	// + iriMapping[0]);
	// }
	//
	// leftToRightMappings.put(iriMapping[0], iriMapping[1]);
	// rightToLeftMappings.put(iriMapping[1], iriMapping[0]);
	// }
	// br.close();
	//
	// System.out.println(counter + "mappings loaded.");
	//
	// }

	public List<OWLOntologyChange> mapOntology(OWLOntology ontology) {
		OWLEntityRenamer renamer = new OWLEntityRenamer(ontology.getOWLOntologyManager(),
				Collections.singleton(ontology));
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

		for (Entry<IRI, IRI> mapping : mappings.entrySet()) {
			changes.addAll(renamer.changeIRI(mapping.getKey(), mapping.getValue()));
		}

		return ontology.getOWLOntologyManager().applyChanges(changes);
	}

	private Map<OWLOntology, List<OWLOntologyChange>> changes;

	public Map<OWLOntology, List<OWLOntologyChange>> actOnOntology(OWLOntology ontology,
			boolean actTransitively) {
		changes = new HashMap<OWLOntology, List<OWLOntologyChange>>();

		changes.put(ontology, mapOntology(ontology));
		if (actTransitively) {
			for (OWLOntology o : ontology.getImports()) {
				changes.put(o, mapOntology(o));
			}
		}

		return getResults();
	}

	public Map<OWLOntology, List<OWLOntologyChange>> getResults() {

		Map<OWLOntology, List<OWLOntologyChange>> result = new HashMap<OWLOntology, List<OWLOntologyChange>>();
		for (Entry<OWLOntology, List<OWLOntologyChange>> entry : changes.entrySet()) {
			result.put(entry.getKey(), new ArrayList<OWLOntologyChange>(entry.getValue()));
		}
		return result;
	}

}
