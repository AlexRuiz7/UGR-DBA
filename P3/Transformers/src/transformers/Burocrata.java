/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 *
 * @author Germán
 */
public class Burocrata extends Agente{

    String conversationID;
    AgentID controlador;
    
    Burocrata(AgentID aID, String nombreServidor) throws Exception {
        super(aID);  
        this.controlador = new AgentID(nombreServidor);
        conversationID = "";
    }
    
    
    public void execute(){
        
    }
    
    
    
    
    
}
