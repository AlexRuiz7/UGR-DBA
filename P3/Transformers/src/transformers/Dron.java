package transformers;

import es.upv.dsic.gti_ia.core.AgentID;

/**
 *
 * @author Germán
 */
public class Dron extends Vehiculo{
    
    
    /**
     * 
     * @param aID
     * @param id_servidor
     * @param id_burocrata
     * @param informa
     * @throws Exception 
     */
    public Dron(AgentID aID, AgentID id_servidor, AgentID id_burocrata, boolean informa) throws Exception {
        super(aID, id_servidor, id_burocrata, informa);
    }
    
}
