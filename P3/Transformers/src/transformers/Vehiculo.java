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
    protected String cID_servidor ;
    protected String cID_burocrata ;
    protected AgentID id_servidor ;
    protected AgentID id_burocrata ;
    //private String conversationID ;
    
    public Vehiculo(AgentID aID, AgentID id_servidor, AgentID id_burocrata, boolean informa) throws Exception {
        super(aID, informa);
        this.id_servidor = id_servidor ;
        this.id_burocrata = id_burocrata ;
    //    conversationID = "";
        cID_servidor = "";
        cID_burocrata = "";
    }
    
    /**
     * @author Alvaro
     */
    @Override
    public void execute() {
        /* VOID - to be OVERRIDE */
        checkin();
    }
    
    /**
     * Método que encapsula la rutina de checkin
     * @author: Alvaro
     * Método que encapsula la rutina de hacer checkin al servidor/controlador
     */
    protected boolean checkin() {
        
        enviarMensaje(id_servidor,ACLMessage.REQUEST, cID_servidor);
        recibirMensaje();
        
        cID_servidor = mensajeEntrada.getConversationId();

        boolean resultado = mensajeEntrada.getPerformativeInt() == ACLMessage.INFORM;
        int performativa = mensajeEntrada.getPerformativeInt();
        
        switch(performativa){
            case ACLMessage.INFORM:
                System.out.println(" Aceptada petición de CHECKIN ");
                // aceptamos las capabilities y las almacenamos
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
        
        return resultado ;
    }
    
    protected void explorar() {}
    
    /**
     * Método que encapsula la rutina de pedir refuel al servidor
     * @author: Alvaro
     * Método que encapsula la rutina de pedir refuel al servidor
     */
    
    protected boolean refuel() {
    
        enviarMensaje(id_burocrata,ACLMessage.REQUEST, cID_servidor);
        recibirMensaje();
        
        boolean resultado = mensajeEntrada.getPerformativeInt() == ACLMessage.INFORM;
        int performativa = mensajeEntrada.getPerformativeInt();
        
        switch(performativa){
            case ACLMessage.INFORM:
                System.out.println(" Aceptada petición de CHECKIN ");
                // Aquí que se hace
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
     
}
