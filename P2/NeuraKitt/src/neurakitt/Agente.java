package neurakitt;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 *
 * @author Alvaro
 * 
 * @author Alejandro
 * @custom.FechaModificacion 05/11/2018
 * @custom.Motivo cambiada privacidad de mensaje_respuesta y mensaje_salido a privada
 */
public class Agente extends SingleAgent {
    // Evita creación de objetos Json durante la ejecución del agente 
    protected JsonObject mensaje;
    
    // Evita crear ACLMessage durante la ejecución del agente, reutiliza objeto. 
    private ACLMessage mensaje_respuesta;
    private ACLMessage mensaje_salida;
    
    protected final boolean DEBUG = true;
    
    
    // Control de iteraciones
    protected int iteracionActual;
    protected int iteracionesTope;
    protected int antiguedad;
    
    /**
     * 
     * @author Alvaro
     * @param aID
     * @throws Exception
     * 
     * @author Alejandro
     * @custom.FechaModificacion 04/11/2018
     * @custom.Motivo añadida inicialización de variables
     */
    public Agente(AgentID aID) throws Exception {
        super(aID);
        
        mensaje = new JsonObject();
        mensaje_respuesta = mensaje_salida = new ACLMessage();
        
        iteracionesTope = 0;
        iteracionActual = 0;
        antiguedad      = 0;
        
        if(DEBUG)
            System.out.println("Agente "+this.getAid().getLocalName()+" creado ");
    }
    
    
    @Override
    /**
     * @author Alvaro
     */
    public void execute() {
        /* VOID - to be OVERRIDE */
    }
    
    
    /**
     * @author: Germán, Alvaro
     * @return Devuelve éxito o no si se ha realizado corectamente 
     * 
     * @author Alejandro
     * @custom.FechaModificacion 01/11/2018
     * @custom.Motivo Las funciones y métodos deben ser siempre verbos. Cambio 
     * ReciboYDecodificoMensaje() por recibirMensaje()
     */
    protected boolean recibirMensaje(){
        try{
            mensaje_respuesta = this.receiveACLMessage();
        }
        catch(InterruptedException ex){
            System.err.println(this.getName() + " Error en la recepción del mensaje. ");
            return false;
        }
        
        mensaje = Json.parse(mensaje_respuesta.getContent()).asObject();
        
        if(DEBUG)
            System.out.println("["+this.getAid().getLocalName()+"]"
                                + "Mensaje recibido " + mensaje.toString());
        
        return true;
    }
    
    
    /**
     * Crea un mensaje y se lo envía al destinatario que recibe como parámetro
     * 
     * @author: Germán, Alvaro
     * @param destinatario 
     * 
     * @author Alejandro
     * @custom.FechaModificacion 01/11/2018
     * @custom.Motivo Las funciones y métodos deben ser siempre verbos. Cambio 
     * DecodificoYEnvioMensaje() por enviarMensaje(). Cambiado valor de retorno 
     * a void
     */
    protected void enviarMensaje(AgentID destinatario){
        mensaje_salida = new ACLMessage();              // Limpia
        mensaje_salida.setSender(this.getAid());        // Emisor (objeto que invoca)
        mensaje_salida.setReceiver(destinatario);       // Receptor (obejto dado como parámetro)
        mensaje_salida.setContent(mensaje.toString());  // Contenido del mensaje
        this.send(mensaje_salida);  
    
        if(DEBUG)
           System.out.println("["+this.getAid().getLocalName() + "]"
                +"] Mensaje enviado " + mensaje.toString());
    }
}
