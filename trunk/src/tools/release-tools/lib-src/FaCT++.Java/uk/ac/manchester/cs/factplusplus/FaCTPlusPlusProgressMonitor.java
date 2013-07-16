package uk.ac.manchester.cs.factplusplus;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: 15-Oct-2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public interface FaCTPlusPlusProgressMonitor {

    public void setClassificationStarted(int classCount);

    public void nextClass();

    public void setFinished();

    public boolean isCancelled();
}
