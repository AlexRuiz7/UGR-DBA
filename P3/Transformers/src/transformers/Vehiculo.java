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
    protected int fuelrate ; // cuanto gasto en cada movimiento
    protected int fuel ; // cuanta bateria o fuel me queda
    protected boolean fly ;
    protected Casilla posicion ;
    protected ArrayList<Integer> scanner ;
    protected AgentID id_servidor ;
    protected AgentID id_burocrata ;
    
    
    //Variables de estado del vehículo
    protected boolean ayuda;
    protected boolean refuel;
    protected int estado;
    /**
     * @Nota: Los valores que actualmente tenemos encuenta son:
     *  0 ---> Vehiculo explorando.
     *         (ACTIVO, candidato para ayudar)
     * 
     *  1 ---> El vehículo está explorando en dirección a una coordenadas 
     *          indicadas por el burócrata, percibe el entorno e informa, pero
     *          no es candidato para ser selecionado para ayudar a otro agente.
     *         (ACTIVO, pero ocupado ayudando)
     * 
     *  2 ---> El vehiculo espera instrucciones, ya sea porque acabda de llegar
     *          al destino indicado por el burócrata 
     *          o porque percibe a otro agente en sus percepciones.
     *         (ACTIVO, candidato para ayudar)
     * 
     *  3 ---> Vehículo está en destino.
     *          El vehículo está parado, pero puede ser usado por el burócrata
     *           para ayudar a otro vehículo.
     *          (INACTIVO, pero candidato para ayudar) 
     * 
     *  4 ---> Vehículo pide ayuda.
     *          El vehículo percibe que su siguiente movimiento es opuesto 
     *           al movimiento anterior.
     *           Ello conlleva que no percibirá nada nuevo.
     *          (INACTIVO, pero activable)
     * 
     *  5 ---> Vehículo inactivo.
     *          El vehículo esta parado, no espera ser ayudado,
     *           ni está en destino; este estado lo induce el burócrata
     *           para indicarle que no se mueva porque no hay energía
     *           para todos los vehículos.
     *          (INACTIVO, sin posibilidad lógistica de ser ACTIVADO)
     * 
     *  6 ---> Vehículo CRACHEADO
     *          No se mueve, ni tiene posibilidad de moverse,
     *           pero sigue logueado en el mapa.
     *          (INACTIVO, sin posibilidad de activarse)
     */
    
    /**
     * Variable innecesaria pero útil para reutilizarla en cada comparativa
     * con las performativas a esperar.
     */
    protected boolean performativa_adecuada;
    
    public Vehiculo(AgentID aID, AgentID id_servidor, AgentID id_burocrata, boolean informa) throws Exception {
        super(aID, informa);
        this.id_servidor = id_servidor ;
        this.id_burocrata = id_burocrata ;
        conversationID = "";
        performativa_adecuada = false;
        ayuda = false;
        refuel = false;
        
        /** 
         * 0] Explorando, 1] Esperando instrucciones, 
         * 2] Ayuda,      3] Estoy en destino, 
         * 4] NO tengo energía para continuar, 5] CRACHEADO
         */ 
        estado = 0;

    }
    
    /**
     * @author Alvaro
     */
    @Override
    public void execute() {
        /**
         * @Nota: El vehículo necesita el conversationID para hacer checkin
         * En este momento hay dos opciones.
         *  @Opción1: Preguntar por el conversationID lo que podría provocar un
         *   interbloqueo si el mensaje lo recibe el burócrata mientras está
         *   realizando la suscripción.
         *   ¿Cómo evitar el interbloqueo? Entrando en un bloque de rechazos
         *   por parte del burócrata hasta recibir el conversationID.
         * 
         *  @Opción2: Esperar a recibir el conversationID hasta que el burócrata
         *   termine la comunicación con el controlador. 
         */
        
        /**
         * @PRE: Al optar por la opción2, checkin contiene la espera del
         *  conversationID.
         *  Cuando se realiza correctamente el checkin también recibo
         *  un replyWith, necesario para continuar con la conversacion con el 
         *  controlador.
         */
        checkin();
        
        /**
         * @Nota En este momento tengo el replyWith en el mensaje de entrada
         *  (que es la respuesta del controlador al checkin),
         *  necesario para la siguiente petición con el controlador.
         */
        replyWith = mensajeEntrada.getReplyWith();
        
        /**
         * Muestra el contenido del agente para verificar que se ha realizado 
         * correctamente la asignación de las capabilities
         */
        toStringAtributos();
        
        /**
         * @Nota: Ahora que el vehículo sabe sus capabilities.
         *  Tenemos dos situaciones posibles.
         * 
         *  @Opción1: Esperar a recibir un mensaje del burócrata para enviarle 
         *   la capabilities.
         *   En este momento, podemos sufrir interbloqueo, pues esperamos
         *   un mensaje del controlador para recibir nuestras capabilities 
         *   y esperando un mensaje del burócrata para responderle con nuestras
         *   capabilities por tanto es necesario entrar en un bucle de rechazos
         *   al burócrata hasta obtener las capabilities y poder responderle.
         *  @Opción2: Enviar al burócrata un mensaje con nuestras cababilities
         *   En este momento, el burócrata no debe estar esperando mensajes del 
         *   controlador pues ya no interactua con él hasta realizar el cancel.
         *   Por tanto no hay riesgo de interbloqueo
 
         * En nuestro caso solo el fuelrate, pues de él se puede deducir
         * las demás al ser combinaciones fijas de capabilities.
         */
        System.out.println("\n MUESTRO el contenido del mensaje: "+ mensaje.toString());
        
        /**
         * En el diagrama de comunicadión es el burocrata quien pregunta por ellas
         * por tanto no envio mis capacidades, pero espero a que me pregunte.
         * Pero hay interfoliación con riesgo de inter-bloqueo.
         * 
         * Provisionalmente implemento la opción2.
         */
        mensaje = new JsonObject();
        mensaje.add("fuelrate", fuelrate );
        enviarMensaje(id_burocrata, ACLMessage.INFORM, conversationID);
        
        System.out.println(this.toString());
        
        /**
         * @Nota: En este momento estoy logueado en el mundo y tengo 
         *  el conversationID, por lo que puedo pedir las percepciones.
         *  Por tanto tengo al menos dos opciones.
         *  @Opción1: Empiezo a perdir las percepciones al controlador para 
         *   enviarlas  despues al burócrata.
         * 
         *  @Opción2: Espero a que el burórata me envíe un mensaje
         *   de confirmación diciendome que todo el equipo está logueado.
         * 
         *  En este momento ambas opciones son buenas pues no hay peligro
         *  de interbloqueo.
         * 
         * @IMPORTANTE Para poder comunicarme con en controlador debo 
         * tener almacenado el replyWith para seguir con la conversación
         */
        
        /**
         * Opción2 elegida,
         */
        
        recibirMensaje();
        performativa_adecuada = mensaje.get("result").asString().contains("OK");
        if(informa)
            if(performativa_adecuada){
                System.out.println(
                    " [" + this.getAid().getLocalName() + "] "
                    +"Confirmación, todo el equipo está logueado. ");
            }
            else{
                System.out.println(
                        " Mensaje no esperado: " 
                        + print(mensajeEntrada));
        }
        
        if(performativa_adecuada) 
            pedir_percepciones();
        
        else System.out.println(
                "\n ERROR performativa inesperada: "
                + print(mensajeEntrada));   
        
        /**
         * @Nota: En este momento debe tener el vehículo la primera percepción
         *  del mapa, por tanto debe enviárselas al burócrata para que actualice
         *  el mapa que contiene.
         *  Cómo vehículo ha sido el último en recibir un mensaje,
         *  no hay problemas de interbloqueo porque no espera recibir mensajes
         *  del controlador ni del burócrata, porque tanto burñocrata como el 
         *  controlador está ahora esperando a recibir un mensaje.
         */
        boolean percepciones_enviadas = enviar_percepciones();
        if(percepciones_enviadas) System.out.println(
                " Percepciones enviadas al burócrata ");
        else System.out.println(
                " ERROR en el envio de las percepciones al burócrata");
        
        /**
         * Ahora debo procesar el contenido del mensaje de burócrata 
         * para saber si:
         *  - Explorar
         *  - Dirgirme a algún lugar
         *  - Esperar a recibir nuevo mensaje de actuación.
         *  - Dejar de moverme. Ya sea por CRASHED o porque se me ha denegado
         *    el refuel.
         * Tras saber mi situación actualizo mi estado.
         * y en base a éste decido el movimiento a realizar.
         * y se lo comunico al controlador (en caso de ser moveXX o refuel)
         */
    }
    
    /**
     * Método que encapsula la rutina de checkin
     * @author: Alvaro
     * Método que encapsula la rutina de hacer checkin al servidor/controlador
     * @return Devuelve si ha sido un éxito o no.
     */
    protected boolean checkin() {
        
        int performativa ;
        boolean tenemos_cID = false ;
        boolean resultado = false ;

       
//        enviarMensaje(id_burocrata, ACLMessage.QUERY_REF, conversationID);

// Espera a recibir el conversationID por parte del burócrata
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

            mensaje = new JsonObject();
            mensaje.add("command", "checkin");
            enviarMensaje(id_servidor,ACLMessage.REQUEST, conversationID);
            recibirMensaje();

            conversationID = mensajeEntrada.getConversationId();

            performativa = mensajeEntrada.getPerformativeInt();

            switch(performativa){
                case ACLMessage.INFORM:
                    System.out.println(" Aceptada petición de CHECKIN ");
                    JsonObject capabilities = mensaje.get("capabilities").asObject();
                    rango = capabilities.get("range").asInt();
                    fuelrate = capabilities.get("fuelrate").asInt();
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
        return resultado;

    }
    
    /**
     * @author: Germán
     * Método que encapsula la rutina de pedir al controlador las percepciones.
     * @Pasos:
     *  1º] Envia mensaje al controlador mediante petición QUERY_REF
     *  2º] Espera mensaje de respuesta con performativa  INFORM
     *       o con performativa NOT_UNDERSTOOD
     * @PRE Debe tenerse almacenada el replyWith de la anterior comunicación
     * @POS Debe almacenarse el replyWith de esta comunicación
     */
    protected void pedir_percepciones(){
        mensaje = new JsonObject();
        enviarMensaje(
                id_servidor, ACLMessage.QUERY_REF,
                conversationID, replyWith);
        if(informa) print(mensajeSalida);
        
        recibirMensaje();
        if(informa) print(mensajeEntrada);
        
        replyWith = mensajeEntrada.getReplyWith();
        
    }
    
    private boolean enviar_percepciones(){
        boolean resultado;       
        
        /**
         * Pide ayuda cuando percibe que el movimiento anterior y 
         *  el actual a realizar son opuestos.
         * @IMPORTANTE decidir en este momento el movimiento a realizar ahora.
         */ 
        
        // Estado del vehículo, importante para el burócrata 
        // para tomar sus decisiones. 
        mensaje.add("estado", estado);
        
        // Pide permiso para realizar refuel.
        refuel = mensaje.get("result").asObject().get("battery").asInt() <= fuelrate;
        mensaje.add("refuel", refuel);
        
        enviarMensaje(id_burocrata, ACLMessage.INFORM, conversationID);
        if(informa){
            System.out.println(
                " ["+ this.getAid().getLocalName() + "]"
                + "\n Mensaje de salida: " +print(mensajeSalida));

            System.out.println(
                " ["+ this.getAid().getLocalName() + "]"
                + "\n Esperando confirmación del burócrata:");
        }
        recibirMensaje();
        
        if(informa){
            System.out.println(
                " ["+ this.getAid().getLocalName() + "]"
                + "\n Recibido mensaje tras la actualización del mapa ");
        }
        
        resultado = mensaje.get("result").asString().contains("OK");
        if(informa)
            System.out.println(" Resultado de la Actualización: "+ resultado);
        
        return resultado;
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
        
        boolean resultado = false ;
            
        if (pedir_refuel()) {
            enviarMensaje(id_servidor,ACLMessage.REQUEST, conversationID);
            recibirMensaje();
        
            int performativa = mensajeEntrada.getPerformativeInt();
        
            switch(performativa){
                case ACLMessage.INFORM:
                    System.out.println(" Refuel realizado correctamente ");
                    resultado = true ;
                    fuel = 100 ;
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
 
        }
        
        return resultado ;
        
    }
    
    
    private void toStringAtributos () {
        System.out.println("ultimo mensaje de entrada (checkin)" + mensajeEntrada.toString());
        System.out.println("atributos : " 
                + "fuel :" + fuelrate
                + "rango :" + rango
                + "fly :" + fly);
    }
}
