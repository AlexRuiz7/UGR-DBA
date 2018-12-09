

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Germán
 */
public class Agente extends SingleAgent {
    //Evita creación de objetos Json durante la ejecución del agente 
    protected JsonObject mensajeContenido;
    
    //Evita crear ACLMessage durante la ejecución del agente, reutiliza objeto. 
    protected ACLMessage mensajeSalida;
    protected ACLMessage mensajeEntrada;
        
    /**
     * 
     * @author Germán
     * @param aID        Identificador del agente
     * @throws Exception
     * 
     * @atributos
     *  mensajeContenido Almacena el contenido del mensaje de entrada o salida
     *  mensajeEntrada   Almacena el mensaje se entrada.
     *  mensajeSalida    Almacena el mensaje de salida.
     */
    public Agente(AgentID aID) throws Exception {
        super(aID);
        
        mensajeContenido = new JsonObject();              
        mensajeSalida       = new ACLMessage();
        mensajeEntrada    = new ACLMessage();
        
        System.out.println("\n Agente "+this.getAid().getLocalName()+" creado");
    }
    
    
    @Override
    public void execute() {
        /* VOID - to be OVERRIDE */
    }
    
    
    /**
     * recibirMensaje encapsula la rutina de recibir y dar formato al contenido
     * guardándolo en la variable mensajeContenido.
     * @author: Germán
     * @return Devuelve éxito o no si se ha realizado correctamente 
     * 
     * @Nota: Útil para mostrar información relevante en el seguimiento 
     * de la comunicación entre los agentes.
     */
    protected boolean recibirMensaje(){
        try{
//            System.out.println("Recibiendo mensaje");
            mensajeEntrada = this.receiveACLMessage();
/*            System.out.println("["
                +mensajeEntrada.getReceiver().getLocalName()
                +"]"
                +"\t mensaje recibido de "
                +mensajeEntrada.getSender().getLocalName()
                +"\t\t contenido: "
                + mensaje_respuesta.getContent());
*/
        }
        catch(InterruptedException ex){
            System.err.println(this.getName() + " Error en la recepción del mensaje. ");
            return false;
        }
        
        mensajeContenido = Json.parse(mensajeEntrada.getContent()).asObject();
        //mensajeContenido = mensajeEntrada.getContent();
        return true;
    }
    
    
    /**
     * enviarMensaje encapsula la rutina de dar formato a la variable 
     * mensajeSalida con el contenido de "mensajeContenido",
     * añadir el remitente, el destinatario, la performativa, ConversationID
     * 
     * @author: Germán
     * @param destinatario: Identificador de l agente destinatario
     * @param performativa: Verbo de la acción
     * @param id:           Identificador de la conversación (InversationID)
     * 
     * @Nota: Útil para mostrar información relevante en el seguimiento 
     * de la comunicación entre los agentes.
     */
    protected void enviarMensaje(
            AgentID destinatario, 
            int performativa,
            String id,
            String conversationID,
            String reply){
        mensajeSalida = new ACLMessage();              
        mensajeSalida.setSender(this.getAid());                 // Emisor
        mensajeSalida.setReceiver(destinatario);                // Receptor
        mensajeSalida.setContent(mensajeContenido.toString());  // Contenido
        mensajeSalida.setConversationId(id);
//        mensajeSalida.setInReplyTo(reply);
        mensajeSalida.setPerformative(performativa);
        mensajeSalida.setConversationId(conversationID);
        
        this.send(mensajeSalida);                      // Enviando el mensaje.
        
/*        System.out.println("["
                +mensaje_salida.getSender().getLocalName()
                +"]\t mensaje enviado a "
                + mensajeSalida.getReceiver().getLocalName()
                + "\t\t contenido: "
                + mensajeSalida.getContent());
*/
    }
    
}
