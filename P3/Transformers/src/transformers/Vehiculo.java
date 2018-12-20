package transformers;

import transformers.utils.Casilla;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.ArrayList;
import org.apache.xmlbeans.impl.xb.xsdschema.AllDocument;

/**
 *
 * @author Germán, Alex
 */
public class Vehiculo extends Agente {
    
    // Capacidades del vehiculo
    protected int rango;
    protected int consumo;
    protected boolean vuelo;
    
    // Identificadores
    protected AgentID id_servidor ;
    protected AgentID id_burocrata ;
    
    // Variables de estado del vehículo
    protected boolean ayuda;
    protected boolean refuel;
    protected int estado;
    protected Comportamiento comportamiento;
    protected JsonArray sensor;
    protected String accion;
    
    // Lista para almacenar los vehiculos detectados en el radar
    protected ArrayList<Casilla> camaradas;
    
    // Variables del vehiculo
    protected int bateria;
    protected Casilla posicion;
    protected ArrayList<Integer> radar;
    protected boolean objetivo;
    
    /**
     * @Nota: Los valores que actualmente tenemos encuenta son:
     *  0 ---> Vehiculo explorando.
     *         (ACTIVO, candidato para ayudar)
     * 
     *  * ---> El vehículo está explorando en dirección a una coordenadas 
     *          indicadas por el burócrata para ayudar,
     *          percibe el entorno e informa, pero
     *         (ACTIVO, candidato para ayudar)
     *         (Nueva versión: si hay más de un vehículo pidiendo ayuda
     *          se le darán las coordenadas del más cero para que lo ayude
     *          en cada iteración)
     * 
     *  * ---> El vehiculo espera instrucciones, ya sea porque acaba de llegar
     *          al destino indicado por el burócrata 
     *          o porque percibe a otro agente en sus percepciones.
     *         @Nota: Podemos tomar como protocolo que un vehículo al llegar
     *          vislumbrar a las coordenadas indicadas por dirigete_a,
     *          éste toma el estado 'en destino' (si se ha llegado a destino)
     *          o 'explorando'
     *         Eso permite quitar este estado.
     *         (INCATIVO, candidato para ayudar)
     * 
     *  1 ---> Vehículo está en destino.
     *          El vehículo está parado, pero puede ser usado por el burócrata
     *           para ayudar a otro vehículo.
     *          (INACTIVO, pero candidato para ayudar) 
     *          (En esta versión no se contempla usarlo para ayudar)
     * 
     *  2 ---> Vehículo pide ayuda.
     *          El vehículo percibe que su siguiente movimiento es opuesto 
     *           al movimiento anterior.
     *           Ello conlleva que no percibirá nada nuevo.
     *          (INACTIVO, pero activable)
     * 
     *  3 ---> Vehículo inactivo.
     *          El vehículo esta parado, no espera ser ayudado,
     *           ni está en destino; este estado lo induce el burócrata
     *           para indicarle que no se mueva porque no hay energía
     *           para todos los vehículos.
     *          (INACTIVO, sin posibilidad lógistica de ser ACTIVADO)
     * 
     *  4 ---> Vehículo CRACHEADO
     *          No se mueve, ni tiene posibilidad de moverse,
     *           pero sigue logueado en el mapa.
     *          (INACTIVO, sin posibilidad de activarse)
     */
    
    /**
     * Variable innecesaria pero útil para reutilizarla en cada comparativa
     * con las performativas a esperar.
     */
    protected boolean performativa_adecuada;
    
    
    /**
     * Constructor
     * 
     * @param aID
     * @param id_servidor
     * @param id_burocrata
     * @param informa
     * @throws Exception 
     */
    public Vehiculo(AgentID aID, AgentID id_servidor, AgentID id_burocrata, boolean informa) throws Exception {
        super(aID, informa);
        this.id_servidor = id_servidor ;
        this.id_burocrata = id_burocrata ;
        conversationID = "";
        performativa_adecuada = false;
        ayuda = false;
        refuel = false;
        
        /** 
         * 0] Explorando, 1] Estoy en destino,
         * 2] Ayuda,       
         * 3] NO tengo energía para continuar, 4] CRACHEADO
         */ 
        estado = 0;
        comportamiento = new Comportamiento(informa);
        accion = "";
        sensor = new JsonArray();
        
        
        camaradas = new ArrayList();
        bateria = 0;
        posicion = new Casilla(0, 0, '?');
        radar = new ArrayList();
        objetivo = false;
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
//        replyWith = mensajeEntrada.getReplyWith();
        
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
 
         * En nuestro caso solo el consumo, pues de él se puede deducir
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
        mensaje.add("consumo", consumo );
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
                System.out.println(" [" + this.getAid().getLocalName() + "] "
                    +"Confirmación, todo el equipo está logueado. ");
            }
            else{
                System.out.println(" Mensaje no esperado: " + print(mensajeEntrada));
        }

        boolean cancel = false;
        while(!cancel){
            
            pedir_percepciones();
            
            /**
             * Deben guardarse las percepciones para que no se pierda
             *  entre los envios de mensajes
             */
        
        /**
         * @Nota: En este momento debe tener el vehículo la primera percepción
         *  del mapa, por tanto debe enviárselas al burócrata para que actualice
         *  el mapa que contiene.
         *  Cómo vehículo ha sido el último en recibir un mensaje,
         *  no hay problemas de interbloqueo porque no espera recibir mensajes
         *  del controlador ni del burócrata, porque tanto burñocrata como el 
         *  controlador está ahora esperando a recibir un mensaje.
         */
        System.out.println(" Percepciones enviadas al burócrata ");
        boolean percepciones_enviadas = 
                enviar_percepciones();
        if(percepciones_enviadas) 
            System.out.println("["+this.getAid().getLocalName()+"]"
                    +" recibida respuesta de "+ id_burocrata.getLocalName());
        else System.out.println(
                " ERROR en el envio de las percepciones al burócrata");
        
            System.out.println(
                    "["+this.getAid().getLocalName()+"]"
                    + " voy a procesar lo que me ha mandado"
                    +"["+ mensajeEntrada.getSender().getLocalName()+"]:\n"
                    + mensaje.toString());
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
        JsonObject directrices = new JsonObject(mensaje);
        //int x = mensaje.get("x").asInt();
        //int y = mensaje.get("y").asInt();
        
        // Primero realizo refuel, de caso de haberlo pedido
        if(directrices.toString().contains("refuel")){
            if(directrices.get("refuel").asBoolean()){        
                mensaje = new JsonObject();       
                mensaje.add("command", "refuel");       
                enviarMensaje(id_servidor, ACLMessage.REQUEST,
                    conversationID, replyWith);        
                recibirMensaje();
                replyWith = mensajeEntrada.getReplyWith();
                
                if(mensajeEntrada.getPerformativeInt() == ACLMessage.REFUSE){
                    estado = 3;
                    System.out.println(this.getAid().getLocalName()
                            +" refuel denegado ");
                }
                else
                    System.out.println(this.getAid().getLocalName()
                            +" refuel aceptado ");
                
            }
        }
        
        /**
         * Creo conveniente volver a pedir las percepciones,
         *  por si he sido un vehiculo que ha estado esperando ayuda,
         *  pues ahora mismo no percibo al vehículo que se me ha acercado.
         */
        if(directrices.toString().contains("dirigete_a")){   
            if(directrices.get("dirigete_a").asBoolean()){
                System.out.println(this.getAid().getLocalName()
                        + " Mi estado actual es: "+ estado);
                //pedir_percepciones();
                System.out.println(this.getAid().getLocalName()
                        + " Mi estado actual es: "+ estado);
            }  
        }
        
        /**
         * Teniendo las percepciones actualizadas (en caso de haber estado
         *  aletargado esperando ayuda)
         *  Debo decidir el movimiento a realizar.
         * 
         * @Nota: por verificar a comunicación pongo un movimiento cualquiera
         *  y proceso el resultado
         */
        
       
        
        mensaje = new JsonObject();
        
//        accion = "moveE";
        accion = comportamiento.explorar(sensor);
           System.out.println(" [" + this.getAid().getLocalName() +"]"
                +" He decidido moverme a: " + accion);
           
        mensaje.add("command", accion);
        enviarMensaje(id_servidor, ACLMessage.REQUEST, conversationID, replyWith);
        
        recibirMensaje();
        replyWith = mensajeEntrada.getReplyWith();
        
        int performativa = mensajeEntrada.getPerformativeInt();
        if(performativa == ACLMessage.INFORM){
            estado = 0;
            System.out.println(" Movimiento aceptado ");
        }
        else{
            System.out.println(" Movimiento Denegado ");
            estado = 4;   
        }
        
        /**
         * Debo informar del resultado del movimiento al burócrata para que
         *  para que tenga encuenta que sigo moviendome
         *  o que he chocado.
         */
        mensaje.add("estado", estado);
        mensaje.add("refuel",refuel);
        enviarMensaje(id_burocrata, performativa, conversationID);
        
        /**
         * Esperando respuesta del burócrata para proseguir.
         */
        recibirMensaje();
            System.out.println("MENSAJE de la última fase "+ print(mensajeEntrada));
        cancel = mensajeEntrada.getPerformativeInt() == ACLMessage.CANCEL;
/*        
        if(performativa != ACLMessage.INFORM)
            while(cancel){
                System.out.println(
                        "\n ESTOY FUERA DE JUEGO "
                        +"["+this.getAid().getLocalName()+"]"
                        + " y NO DEBO RECIBIR MENSAJES \n");
                recibirMensaje();
                if(mensaje.get("cancel").asString().contains("cancel"))
                    cancel = mensajeEntrada.getPerformativeInt() == ACLMessage.CANCEL;
                System.out.println(
                        "\n ESTOY FUERA DE JUEGO "
                        +"["+this.getAid().getLocalName()+"]"
                        + " Mensaje recibido de "
                        + mensajeEntrada.getSender().getLocalName()
                        + " cuyo mensaje es: "+ print(mensajeEntrada)
                        +"\n"
                );
            }
*/        
        
// Cierre del while
        }
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
                    consumo = capabilities.get("fuelrate").asInt();
                    vuelo = capabilities.get("fly").asBoolean();
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
        replyWith = mensajeEntrada.getReplyWith();
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
        enviarMensaje(id_servidor, ACLMessage.QUERY_REF, conversationID, replyWith);
        if(informa)
            print(mensajeSalida);
        
        recibirMensaje();

        if(informa) print(mensajeEntrada);
        if(mensajeEntrada.getPerformativeInt() != ACLMessage.INFORM)
            estado = 4;
        else{
            sensor = mensaje.get("result").asObject().get("sensor").asArray();
            System.out.println("["+this.getAid().getLocalName()+"]"
              +" PERCEPCIONES GUARDADAS: "
              + sensor.toString());
            
        }
        
        if(informa)
            System.out.println("["
                    + this.getAid().getLocalName() 
                    + "] (estado: "+ estado +")"
                    + "\n Mi mensaje es: "+ mensaje.toString());
        
        replyWith = mensajeEntrada.getReplyWith();  
    }
    
    
    
    /**
     * @author: Germán
     *  Método que encapsula la comunicación con el burócrata cuando el vehículo
     *   ha recibido previamente la información de los sensores.
     * @return Devuelve si se ha realizado la comunicación con éxito.
     * 
     */
    private boolean enviar_percepciones(){
        boolean resultado;
        // Estado del vehículo, importante para el burócrata 
        // para tomar sus decisiones. 
        
        /**
         * Pide ayuda cuando percibe que el movimiento anterior y 
         *  el actual a realizar son opuestos.
         * @IMPORTANTE decidir en este momento el movimiento a realizar ahora.
         */
        mensaje.add("estado", estado);
//        mensaje.add("refuel", false);
        
        int performativa = mensajeEntrada.getPerformativeInt();
        resultado = performativa == ACLMessage.INFORM;
        if(resultado){
        // Pide permiso para realizar refuel.

        refuel = mensaje.get("result").asObject().get("battery").asInt() <= consumo;
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
        
        // Esperando respuesta del burócrata
           recibirMensaje();

           if(informa)
            System.out.println(
                " ["+ this.getAid().getLocalName() + "]"
                + "\n Recibido mensaje tras la actualización del mapa ");
          }
        
        resultado = mensajeEntrada.getPerformativeInt() == ACLMessage.INFORM;
        
        if(informa)
            System.out.println(" Resultado de la Actualización: "+ resultado);
        return resultado;
    }
   
    
/*** COMPORTAMIENTO BASICO ****************************************************/
    /**
     * @Reflexión
     * Conductas de exploración.
     * Cuando un vehíco se mueve éste descubre nuevas casillas,
     *  pero hay movimientos en donde se descubre más.
     *  Moverse en diagonal proporciona más información que moverse
     *  de forma lineal, como el consumo es el mismo para ambas decisiones
     *  creemos conveniente priorizar los movimiento en diagonal que lineal.
     *   ¿Cómo hacerlo ? Una forma sería ordenar la información en la toda 
     *  de decisiones siendo los primeros elementos los movimientos en diagonal.
     * 
     *  Para decidir el lugar a donde irá el vehículo, inicialmente 
     *   hemos pensado en realizar un conteo de casillas libres, condicionando
     *   al agente a decidir como movimiento más prometedor
     *   aquél de menor valor.
     * 
     *  ¿Cómo es el conteo? He decidido sumar el valor de cada casilla,
     *     0] libre
     *     1] obstáculo (solo para coche y camión)
     *     2] muro (obtáculo incluso para el dron)
     *     3] destino
     *     4] agente
     * 
     *  @Nota: El valor rompe con la idea,
     *  ¿Cómo solventarlo? Sustituyendo dicho valor por un valor negativo
     *  ya sea -3 o el valor negativo más pequeño. (Alejandro sugiere -8)
     *  
     *  ¿Qué criterio se usa para asignar dicho valor?
     *   Cada movimiento tiene asociado un cuadrante
     *   Cada cuadrante tiene asociado la misma cantidad de casillas,
     *    lo que provoca solapamiento en la información.
     *    No le veo inicialemente ningún problema, pero puede serlo.
     * 
     *   @Nota: Todo el comportamiento se encapsula en la clase comportamiento.
     *  
     *   
     */
     
 
    

    /**************************************************************************/
    
    
    /*************************************************************************/
    /*
        Tareas de Alvaro
    */
    /*************************************************************************/
    
    
    
    /**
     * Método que encapsula la rutina de pedir refuel al servidor
     * @return 
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
    
    
    /**
     * @author: Alvaro
     * @return 
     */
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
                    bateria = 100 ;
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
    
    
    /**
     * 
     */
    private void toStringAtributos() {
        System.out.println("ultimo mensaje de entrada (checkin)" + mensajeEntrada.toString());
        System.out.println("atributos : " 
                + "fuel :" + consumo
                + "rango :" + rango
                + "vuelo :" + vuelo);
    }
    
    
    /*************************************************************************/
    /*
        Tareas de Alex
    */
    /*************************************************************************/
    
    
    /**
     * Pide la informacion al servidor y actualiza su estado interno
     */
    protected void actualizarSensores() {
        pedir_percepciones();
        
        bateria = mensaje.get("result").asObject().get("battery").asInt();
        
        posicion = new Casilla(
                    mensaje.get("result").asObject().get("x").asInt(),
                    mensaje.get("result").asObject().get("y").asInt());
        
        JsonArray datos = new JsonArray();
        datos = mensaje.get("result").asObject().get("sensor").asArray();
        for (JsonValue valor : datos)
            radar.add(valor.asInt());
        
        objetivo = mensaje.get("result").asObject().get("goal").asBoolean();
    }
    
    
    /**
     * Cuando un vehículo detecta a otro, lo notifica al Controlador y continúa 
     * su exploración. Cuando 2 vehículos se han detectado mutuamente, el 
     * Controlador se encarga de mandarlos a otra zona del mapa para evitar que 
     * hagan el mismo camino que acaba de hacer su compañero.
     * 
     * @return 
     */
    protected boolean hayOtros() {
        
        if(radar.contains(4)) {
            for (int i = 0; i < radar.size(); i++) {
                if (radar.get(i)==4)
                    camaradas.add(new Casilla(0,0));            // TODO: conversion de coordenadas con GPS
            }
            
            enviarMensaje(id_burocrata, ACLMessage.INFORM, conversationID);     // TODO: enviarMensaje con informacion
        }
        
        return !radar.isEmpty();
    }
    
}
