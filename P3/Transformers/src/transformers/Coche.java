/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import es.upv.dsic.gti_ia.core.AgentID;

/**
 *
 * @author Germán
 */
public class Coche extends Vehiculo{
    
    public Coche(AgentID aID, AgentID id_servidor, AgentID id_burocrata, boolean informa) throws Exception {
        super(aID, id_servidor, id_burocrata, informa);
    }
}
