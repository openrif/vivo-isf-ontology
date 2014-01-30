package isf.mapping;

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
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

/**
 * Takes a mapping file that has two values per line (space or tab separated)
 * that represent an IRI mapping. The values are not enclosed in <>. This class
 * can then do a right to left, or left to right conversion of the IRIs in the
 * passed ontology or ontologies.
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
public class Mapper {

	private Map<IRI, IRI> leftToRightMappings;
	private Map<IRI, IRI> rightToLeftMappings;

	public void loadMappingFile(File mappingFile) throws IOException {
		leftToRightMappings = new HashMap<IRI, IRI>();
		rightToLeftMappings = new HashMap<IRI, IRI>();

		BufferedReader br = new BufferedReader(new FileReader(mappingFile));

		String line = null;
		int counter = 0;
		while ((line = br.readLine()) != null) {
			++counter;
			String[] mapping = line.split("[ \t]");

			if (mapping.length != 2) {
				System.err.println("Warning: mapping line " + counter + " "
						+ line + ", does ");
			}

			IRI[] iriMapping = new IRI[2];
			iriMapping[0] = IRI.create(mapping[0]);
			iriMapping[1] = IRI.create(mapping[1]);

			if (leftToRightMappings.get(iriMapping[0]) != null) {
				System.err.println("Warning: IRI " + iriMapping[0]
						+ " already has left-to-right mapping "
						+ leftToRightMappings.get(iriMapping[0])
						+ ", will ignore " + iriMapping[1]);
			}

			if (rightToLeftMappings.get(iriMapping[1]) != null) {
				System.err.println("Warning: IRI " + iriMapping[1]
						+ " already has right-to-left mapping "
						+ rightToLeftMappings.get(iriMapping[1])
						+ ", will ignore " + iriMapping[0]);
			}

			leftToRightMappings.put(iriMapping[0], iriMapping[1]);
			rightToLeftMappings.put(iriMapping[1], iriMapping[0]);
		}
		br.close();

		System.out.println(counter + "mappings loaded.");

	}

	public void leftToRightMapOntology(OWLOntology ontology) {
		OWLEntityRenamer renamer = new OWLEntityRenamer(
				ontology.getOWLOntologyManager(),
				Collections.singleton(ontology));

		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

		for (Entry<IRI, IRI> mapping : leftToRightMappings.entrySet()) {
			changes.addAll(renamer.changeIRI(mapping.getKey(),
					mapping.getValue()));
		}

		ontology.getOWLOntologyManager().applyChanges(changes);

	}

	public void leftToRightMapOntologies(Set<OWLOntology> ontologies) {

		for (OWLOntology o : ontologies) {
			leftToRightMapOntology(o);
		}
	}

	public void rightToLeftMapOntology(OWLOntology ontology) {
		OWLEntityRenamer renamer = new OWLEntityRenamer(
				ontology.getOWLOntologyManager(),
				Collections.singleton(ontology));

		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

		for (Entry<IRI, IRI> mapping : rightToLeftMappings.entrySet()) {
			changes.addAll(renamer.changeIRI(mapping.getKey(),
					mapping.getValue()));
		}

		ontology.getOWLOntologyManager().applyChanges(changes);

	}

	public void rightToLeftMapOntologies(Set<OWLOntology> ontologies) {

		for (OWLOntology o : ontologies) {
			rightToLeftMapOntology(o);
		}
	}

}
