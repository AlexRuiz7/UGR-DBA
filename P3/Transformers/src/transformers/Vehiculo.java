/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.ArrayList;

/**
 *
 * @author Germán
 */
public class Vehiculo extends Agente{
    
    protected int rango ;
    protected int fuel ;
    protected boolean fly ;
    protected Casilla posicion ;
    protected ArrayList<Integer> scanner ;
    protected AgentID id_servidor ;
    protected AgentID id_burocrata ;
    protected String conversationID ;
    
    public Vehiculo(AgentID aID, AgentID id_servidor, AgentID id_burocrata, boolean informa) throws Exception {
        super(aID, informa);
        this.id_servidor = id_servidor ;
        this.id_burocrata = id_burocrata ;
        conversationID = "";

    }
    
    /**
     * @author Alvaro
     */
    @Override
    public void execute() {
        /* VOID - to be OVERRIDE */
        checkin();
        toStringAtributos();
    }
    
    /**
     * Método que encapsula la rutina de checkin
     * @author: Alvaro
     * Método que encapsula la rutina de hacer checkin al servidor/controlador
     */
    protected boolean checkin() {
        
        int performativa ;
        boolean tenemos_cID = false ;
        boolean resultado = false ;

        
        enviarMensaje(id_burocrata, ACLMessage.QUERY_REF, conversationID);
        recibirMensaje();
        
        performativa = mensajeEntrada.getPerformativeInt();
        
        switch(performativa){
            case ACLMessage.INFORM:
                System.out.println(" Voy a recibir el conversationID por parte de Burócrata ");
                conversationID = mensajeEntrada.getConversationId();
                tenemos_cID = true ;
                break;
                
            case ACLMessage.NOT_UNDERSTOOD:
                System.out.println(" No entiendo lo que quieres. ");
                System.out.println(" Detalles: " + mensaje.get("details"));
                break;
                
            default:
                System.out.println(
                        " Se ha recibido la performativa: " 
                        + performativa);
                break;    
        }
        
        if (tenemos_cID) { // ZONA DE CHECKIN SI TENEMOS EL CID DEL SERVIDOR
            enviarMensaje(id_servidor,ACLMessage.REQUEST, conversationID);
            recibirMensaje();

            conversationID = mensajeEntrada.getConversationId();

            performativa = mensajeEntrada.getPerformativeInt();

            switch(performativa){
                case ACLMessage.INFORM:
                    System.out.println(" Aceptada petición de CHECKIN ");
                    JsonObject capabilities = mensaje.get("capabilities").asObject();
                    rango = capabilities.get("range").asInt();
                    fuel = capabilities.get("fuelrate").asInt();
                    fly = capabilities.get("fly").asBoolean();
                    resultado = true ;
                    break;

                case ACLMessage.NOT_UNDERSTOOD:
                    System.out.println(" No entiendo lo que quieres. ");
                    System.out.println(" Detalles: " + mensaje.get("details"));
                    break;

                default:
                    System.out.println(
                            " Se ha recibido la performativa: " 
                            + performativa);
                    break;    
            }

        }
        return resultado ;

    }
    
    protected void explorar() {}
    
    /**
     * Método que encapsula la rutina de pedir refuel al servidor
     * @author: Alvaro
     * Método que encapsula la rutina de pedir refuel al servidor
     */
    
    protected boolean pedir_refuel() {
    
        enviarMensaje(id_burocrata,ACLMessage.REQUEST, conversationID);
        recibirMensaje();
        
        boolean resultado = false ;
        int performativa = mensajeEntrada.getPerformativeInt();
        
        switch(performativa){
            case ACLMessage.INFORM:
                System.out.println(" Aceptada petición de hacer refuel ");
                resultado = true ;               
                break;
                
            case ACLMessage.NOT_UNDERSTOOD:
                System.out.println(" No entiendo lo que quieres. ");
                System.out.println(" Detalles: " + mensaje.get("details"));
                break;
                
            case ACLMessage.REFUSE:
                System.out.println(" No entiendo lo que quieres. ");
                System.out.println(" Detalles: " + mensaje.get("details"));
                break;
                
            default:
                System.out.println(
                        " Se ha recibido la performativa: " 
                        + performativa);
                break;    
        }
        
        return resultado ;

    }
    
    private boolean refuel() {
        return true;
    }
    
    
    private void toStringAtributos () {
        System.out.println("ultimo mensaje de entrada (checkin)" + mensajeEntrada.toString());
        System.out.println("atributos : " 
                + "fuel :" + fuel
                + "rango :" + rango
                + "fly :" + fly);
    }
    
}
