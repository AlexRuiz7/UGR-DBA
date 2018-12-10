package transformers;


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
 * @author Germán
 */
public class Agente extends SingleAgent {
    //Evita creación de objetos Json durante la ejecución del agente 
    protected JsonObject mensaje;
    
    //Evita crear ACLMessage durante la ejecución del agente, reutiliza objeto. 
    protected ACLMessage mensajeSalida;
    protected ACLMessage mensajeEntrada;
    
    //Variable necesaria si se quiere visualizar el intercambio de mensajes.
    boolean informa;
        
    /**
     * Método que inicializa los atributos 
     * @author Germán
     * @param aID        Identificador del agente
     * @param informa    Muestra información relevante de cada mensaje
     *                   recibido y enviado
     * @throws Exception
     * 
     * @atributos
     *  mensajeContenido Almacena el contenido del mensaje de entrada o salida
     *  mensajeEntrada   Almacena el mensaje se entrada.
     *  mensajeSalida    Almacena el mensaje de salida.
     */
    public void inicializar(AgentID aID, boolean informa) throws Exception {
        
        mensaje = new JsonObject();              
        mensajeSalida     = new ACLMessage();
        mensajeEntrada    = new ACLMessage();
        
        this.informa = informa;
        
        System.out.println("\n Agente "+this.getAid().getLocalName()+" creado");
    }
    
    /**
     * @author: Germán
     * Constructor donde no se explicita querer ser informado 
     * de toda la comunicación del agente con su entorno.
     * @param aID
     * @throws Exception 
     */
    public Agente(AgentID aID) throws Exception{
        super(aID);
        inicializar(aID, false);
    }
    
    /**
     * @author: Germán
     * Constructor donde se indica explicitamente el deseo de ser informado 
     * de toda la comunicación de este agente.
     * @param aID
     * @throws Exception 
     */
    public Agente(AgentID aID, boolean informa) throws Exception{
        super(aID);
        inicializar(aID, informa);
    }
    
    
    /**
     * @author Germán
     * Método vacio que indica la necesidad de implementar las acciones
     * que debe realizar el agente en las clases hijas.
     */
    @Override
    public void execute() {
        /* VOID - to be OVERRIDE */
    }
    
    
    /**
     * @author: Germán
     * recibirMensaje encapsula la rutina de recibir y dar formato al contenido
     * guardándolo en la variable mensajeContenido.
     * @return Devuelve éxito o no si se ha realizado correctamente 
     * 
     * @Nota: Útil para mostrar información relevante en el seguimiento 
     * de la comunicación entre los agentes.
     * Solo visible si se ha indicado en el constructor. 
     */
    protected boolean recibirMensaje(){
        try{
            
            mensajeEntrada = this.receiveACLMessage();
            
            if(informa){
                System.out.println("Recibiendo mensaje");
                System.out.println("["
                    + mensajeEntrada.getReceiver().getLocalName()
                    + "]"
                    + "\t mensaje recibido de "
                    + mensajeEntrada.getSender().getLocalName()
                    + "\t\t contenido: "
                    + mensajeEntrada.getContent()
                    + "\n\t\t\t\t\t\t\t performativa: "
                    + mensajeEntrada.getPerformative()
                    + "\n\t\t\t\t\t\t\t conversationID: "
                    + mensajeEntrada.getConversationId()
                    + "\n\t\t\t\t\t\t\t replay with: "
                    + mensajeEntrada.getReplyWith());
            }
            
        }
        catch(InterruptedException ex){
            System.err.println(this.getName() + " Error en la recepción del mensaje. ");
            return false;
        }
        
        mensaje = Json.parse(mensajeEntrada.getContent()).asObject();
        //mensajeContenido = mensajeEntrada.getContent();
        return true;
    }
    
    
    /**
     * @author: Germán
     * enviarMensaje encapsula la rutina de dar formato a la variable 
     * mensajeSalida con el contenido de "mensajeContenido",
     * añadir el remitente, el destinatario, la performativa, ConversationID
     * 
     * @param destinatario: Identificador del agente destinatario
     * @param performativa: Verbo de la acción
     * @param id:           Identificador de la conversación (InversationID)
     * @param replayWith:   Para distinguir el momento de la conversación.
     * 
     * @Nota: Útil para mostrar información relevante en el seguimiento 
     * de la comunicación entre los agentes.
     */
    protected void enviarMensaje(
            AgentID destinatario, 
            int performativa,
            String id, String replayWith){
        mensajeSalida = new ACLMessage();              
        mensajeSalida.setSender(this.getAid());        // Emisor
        mensajeSalida.setReceiver(destinatario);       // Receptor
        mensajeSalida.setContent(mensaje.toString());  // Contenido
        mensajeSalida.setConversationId(id);
        mensajeSalida.setInReplyTo(replayWith);
        mensajeSalida.setPerformative(performativa);
        this.send(mensajeSalida);                      // Enviando el mensaje.
        
        if(informa){
            System.out.println("["
                + mensajeSalida.getSender().getLocalName()
                + "]\t mensaje enviado a "
                + mensajeSalida.getReceiver().getLocalName()
                + "\t\t contenido: "
                + mensajeSalida.getContent()
                + "\n\t\t\t\t\t\t\t performativa: "
                + mensajeSalida.getPerformative()
                + "\n\t\t\t\t\t\t\t conversationID: "
                + mensajeSalida.getConversationId()
                + "\n\t\t\t\t\t\t\t In repaly to: "
                + mensajeSalida.getInReplyTo());
        }
    }
    
    /**
     * @author: Germán
     * Todas las conversaciones no tienen hilo conductor y para evitar 
     * añadir un campo que no es de interés en algunos momentos he preferido 
     * sobrecargar el método.
     * @param destinatario: Identificador del agente destinatario
     * @param performativa: Verbo de la acción
     * @param id:           Identificador de la conversación (InversationID)
     */
    protected void enviarMensaje(
            AgentID destinatario,
            int performativa,
            String id){
        enviarMensaje(destinatario, performativa, id, "");
    }
    
}
