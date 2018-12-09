/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 *
 * @author Germán
 */
public class Burocrata extends Agente {
    
    private String conversationID_controlador ;
    private String[] conversationID_vehiculos ;
    private String[] estados_vehiculos ;
    private int vehiculos_activos ;
    private int tamanio_mapa ;
    private int[] mapa ;
        
    private void suscribe(){}
        
    private void cancel(){}
        
    private void getTraza(){}
        
    private void getTrazaAnterior(){}
        
    private void dirigeteA(Casilla c) {}
        
        
    public Burocrata(AgentID aID) throws Exception {
        super(aID);
        vehiculos_activos = 4 ;
                
        System.out.println("\n Agente "+this.getAid().getLocalName()+" creado");
    }
    
    
    @Override
    public void execute() {
        /* VOID - to be OVERRIDE */
    }
}
