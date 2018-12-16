package transformers;


import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 * @author Alex
 */
public class Agente_v2 extends SingleAgent {
    
    private ACLMessage mensajeACL;
       
    protected boolean informa;
    
    // Variable necesaria para continual una conversación. 
    protected String replyWith;
    protected String conversationID;
    
    /**
     * 
     * @param aID
     * @param informa
     * @throws Exception 
     */
    public Agente_v2(AgentID aID, boolean informa) throws Exception{
        super(aID);
        mensajeACL = new ACLMessage();
    }
    
    
    /**
     * 
     * @return 
     */
    protected JsonObject recibirMensaje(){
        try{
            mensajeACL = this.receiveACLMessage();
        }
        catch(InterruptedException ex){
            System.err.println(this.getName() + " Error en la recepción del mensaje. ");
        }
        
        return Json.parse(mensajeACL.getContent()).asObject();
    }
     
    
    /**
     * 
     * @param destinatario
     * @param performativa
     * @param contenido
     * @param conversationID
     * @param replayWith 
     */
    protected void enviarMensaje(AgentID destinatario, int performativa, String contenido,
        String conversationID, String replayWith){
         
        mensajeACL = new ACLMessage();              
        mensajeACL.setSender(this.getAid());
        mensajeACL.setReceiver(destinatario);
        mensajeACL.setPerformative(performativa);
        mensajeACL.setContent(contenido);
        mensajeACL.setConversationId(conversationID);
        mensajeACL.setInReplyTo(replayWith); 
        
        this.send(mensajeACL);
    }
}