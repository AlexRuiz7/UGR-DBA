package transformers;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Germán, Alvaro
 */
public class Burocrata extends Agente {

    private AgentID id_controlador;
    private String nombreMapa;
    private String equipo;
    private int vehiculos_activos;
    private int VEHICULOS_MAX;
    
    private BufferedImage mapa;
    private String rutaFichero;
    private int iteracion_actual;

    
//    private String[] conversationID_vehiculos ;
    private ArrayList<AgentID> agentID_vehiculos;
    private int[] estados_vehiculos ;
    private int tam_mapa ;
    
    private ArrayList<JsonObject> mensaje_vehiculos;
    private ArrayList<Integer> dorsales ;
    private ArrayList<Integer> espectros ;
    
    /**
     * @author: Germán
     * Método inicializador de los atributos del burócrata. 
     * @param aID:            Identificador del agente
     * @param nombreServidor: Nombre del controlador 
     *                         con el que se comunicará el agente 
     * @param nombreMapa:     Nombre del mapa a explorar
     * @throws Exception 
     */
    private void inicializar(String nombreServidor, String nombreMapa, ArrayList<AgentID> vehiculos) 
        throws Exception {
    //       System.out.println("\n INICIALIZACION DEL BUROCRATA");
        this.id_controlador = new AgentID(nombreServidor);
        conversationID = "";
        iteracion_actual= 0;
        
        /**
         * Hasta que no se realice el chekin por parte de los exploradores
         * no se conoce la configuración de equipo.
         */
        equipo = "";
        vehiculos_activos = 0;
        VEHICULOS_MAX = vehiculos.size();
        
        agentID_vehiculos = new ArrayList<>();
        mensaje_vehiculos = new ArrayList<>();
        
        for(int i=0; i< VEHICULOS_MAX; i ++){
            agentID_vehiculos.add(vehiculos.get(i));
            mensaje_vehiculos.add(new JsonObject());
        }
        estados_vehiculos = new  int[4];
        
        /** Colores para representar:
         *   - la ubicación de cada vehículo
         *   - su espectro de visión
         */
        dorsales = new ArrayList<>();
        dorsales.add(0xFF0000); dorsales.add(0x00FF00);
        dorsales.add(0x0000FF); dorsales.add(0x00FFFF);
        
        espectros = new ArrayList<>();
        espectros.add(0xFFdddd); espectros.add(0xddFFdd);
        espectros.add(0xe3FFdd); espectros.add(0xddFFFF);
        
        /**
         * Necesario para crear el nombre del archivo que contendrá la traza
         */
        this.nombreMapa = nombreMapa;
        this.rutaFichero = "";
    }

    
    /**
     * @author: Germán
     * Constructor donde no se explicita querer ser informado 
     * de toda la comunicación del agente.
     * @param aID:            Identificador del agente
     * @param nombreServidor: Nombre del controlador 
     *                         con el que se comunicará el agente 
     * @param nombreMapa:     Nombre del mapa a explorar
     * @param vehiculos
     * @throws Exception 
     */
    public Burocrata(AgentID aID, String nombreServidor, ArrayList<AgentID> vehiculos, 
            String nombreMapa)
        throws Exception{
            super(aID, false);
            inicializar(nombreServidor, nombreMapa, vehiculos);
    }
    /**
     * @author: Germán
     * Constructor donde se indica explicitamente el deseo de ser informado 
     * de toda la comunicación de este agente.
     * @param aID:            Identificador del agente
     * @param nombreServidor: Nombre del controlador 
     *                         con el que se comunicará el agente 
     * @param vehiculos 
     * @param nombreMapa:     Nombre del mapa a explorar
     * @param informa:        Con valor TRUE informa de toda comunicación.
     * @throws Exception 
     */
    public Burocrata(AgentID aID, String nombreServidor, ArrayList<AgentID> vehiculos, 
            String nombreMapa, boolean informa) throws Exception{
           
        super(aID, informa);
        inicializar(nombreServidor, nombreMapa, vehiculos);
    }
    
    
    /**
     * @author: Germán
     * Método que ejecutará el agente desde que despierta.
     * 
     * @Nota 9-12-2018: Probando los métodos subscribe, cancel y obtener mapa
     */
    @Override
    public void execute(){
        /**
         * Inicialmente se pretende mandar un mensaje de cancel para 
         * cerrar la sesión anterior y empezar la nueva sesión.
         * Hay dos situaciones posibles al envial el cancel.
         *  AGREE:
         *   Hubo una sesión anterior que no se cerró correctamente.
         *  
         *  NOT_UNDERSTOOD:
         *   Hubo una sesión anterior que se cerró correctamente o
         *   es la primera sesión en este mapa.
         * 
         * Se puede aprovechar dicha situación para obtener las dimensiones del 
         * mapa a partir de la traza.
         *  Para ello se cierra la sesión anterior y se toman las dimensiones
         *  o se inicia una subscripción y posteriormente un cancel.
         * 
         */
        obtenerMapa();
        
        ToStringMapa("hex");
        /**
         * @Nota: En este momento el burócrata no conoce el conversationID 
         *  necesario para que los vehículos realicen checkin.
         * Por tanto hay dos opciones:
         *   @Opción1: Que los vehículos pregunten al burócrata por el
         *    conversationID.
         *    Lo que implicaría que el burocrata respondiese que no lo tiene o
         *    que inicie una comunicación con el controlador.
         *    Lo que nos lleva a una situación donde el burócrata espera
         *    respuesta del controlador y de los vehículos.
         *    Por tanto hay riego de interbloqueo. ¿Comó evitarlo?
         *    Entrando el burócrata en un bucle donde solamente saldrá cuando 
         *    reciba elconversationID y rechazando todos los demás mensajes.
         *    Lo que conlleva que los vehículos entren en otro bucle del cual
         *    no saldrán mientras se le rechace;
         *    saldrán cuando reciban un mensaje con el conversationID.
         *    @IMPORTANTE: Hay una limitación en la comunicación por parte
         *    de la librería magentix que es de 65535 mensajes.
         * 
         *  @Opción2: Que los vehículos esperen a recibir el conversationID.
         *    Ello implica que el burócrata debe suscribirse y después enviar
         *    el conversationID a los vehículos.
         *    Por tanto no hay riesgo de interbloqueo ni de superar el límite
         *    de mensajes.
         */
        // Suscibriendose al mapa
        subscribe(nombreMapa);
        
        /**
         * @Nota: En este momento el controlador queda a la espera del checkin
         *  de los vehículos, y los vehículos desconocen el conversationID
         *  con el cual poder comunicarse con el controlador.
         * En este momento hay dos opciones:
         *  @Opción1: Ser los vehículos quien pregunten por el conversationID 
         *   con el riesgo de enviar el mensaje antes de haber acabado la 
         *   comunicación del burócrata con el controlador en el subscribe.
         *   @Reflexión ¿Cómo evita recibir el mensaje antes de tiempo
         *   y por ende evitar el interbloqueo?
         *   El mensaje se va a recibir si o si, no hay manera de sincronizarlos
         *   Lo que si se puede hacer es verificar si se esta recibiendo lo que 
         *   se espera recibir y en otro caso enviar REFUSE.
         *   Ello implica que el vehículo entra en un bucle while, 
         *   del cual saldrá cuando reciba una performativa distinta a REFUSE.
         *   @IMPORTANTE: Tenemos el problema del limite de mensajes de 65536
         * 
         *  @Opción2: Ser el burócrata quien envie el mensaje a los vehículos,
         *   ello implica que solo se enviará el mensaje del conversationID cuando
         *   el burócrata termina la conversación con el controlador, 
         *   evitando así el interbloqueo.
         *   @IMPORTANTE: El riego de llegar a 65535 mensajes no está presente.
         */

        // Envia mensaje a los agentes con el conversationID 
        mensaje = new JsonObject();
        mensaje.add("result", "Enviando conversationID");
        for(int i=0; i<VEHICULOS_MAX; i++){
            enviarMensaje(agentID_vehiculos.get(i), ACLMessage.INFORM, conversationID);
        }
        
        
        /** Ahora es el momento de esperar al checkin de los agentes para ser 
         * informado de sus capacidades.
         *  Hay dos opciones:
         *   @Opción1: El burócrata pide las capacidades a los vehículos,
         *    con el riesgo de enviar el mensaje antes de haber terminado el 
         *    vehículo la conversación con el controlador (durante el checkin)
         *    provocando interbloqueo.
         *   @Reflexión: ¿Cómo evitarlo?
         *   Siendo rechazadas las peticiones del burócrata entrando en un bucle 
         *    de pregunta y rechazo del cual saldrá cuando el vehículo envie sus
         *    capacidades al burócrata, esto ocurrirá cuando el vehículo termine 
         *    la conversación con el controlador.
         * 
         *  @Opción2: El burócrata espera a que el vehículo le envie sus 
         *   capacidades, esto ocurrirá cuando el vehículo termine la 
         *   conversación con el controlador.
         */
        
        /**
         * @PRE: Se presupone que los agentes recibidos en el constructor 
         * aún no se han movido y por tanto no están CRASHEADOS,
         * y aún no han pedido las percepciones al controlador.

         * @Nota: En este momento los vehículos están en disposición de pedir 
         *  las percepciones al controlador y por tanto el agente burócrata
         *  espera recibir dichas percepciones.
         * 
         *  Para seguir con el criterio de:
         *   "el último que recibe, es el siguiente en enviar "
         *  Toca no presuponer que el vehiculo pedirá las percepciones 
         *  y por tanto el burócrata le enviará un mensaje de "OK" 
         *  simbolizando que "Ok, he recibido tus capabilities "
         * 
         */
        for(int i = 0; i<VEHICULOS_MAX; i++){
           recibirMensaje();
            if(mensajeEntrada.getPerformativeInt() == ACLMessage.INFORM){
               vehiculos_activos++;
               equipo += mensaje.get("fuelrate");
               
               if(informa)
                    System.out.println("["+ this.getAid().getLocalName()+"]"
                       + "Agente: " 
                       + mensajeEntrada.getSender().getLocalName()
                       + " resistrado");   
            }
            else{
                System.out.println(
                       " Se ha recibido la performativa: " 
                        + mensajeEntrada.getPerformativeInt());
            }    
        }
       
        if(informa) System.out.println(" El equipo es: "+ equipo);
       
        for(int i=0; i<vehiculos_activos; i++){
           mensaje = new JsonObject();
           mensaje.add("result", "OK, ya está el equipo completo registrado.");
           enviarMensaje(agentID_vehiculos.get(i), ACLMessage.INFORM, conversationID);
        }
       
       
       /**
        * En este momento los vehículos pueden pedir las percepciones del mapa
        * al controlador y es el burócrata quien está a la espera de recibir 
        * dichas percepciones para actualizar el mapa que gestiona.
        */
       int posicion;
       int estado_actual;
       JsonObject percepciones;
       
       for(int i=0; i< vehiculos_activos; i++){
           recibirMensaje();
           
           /** 
            * Almaceno el estado del vehículo para responderle 
            * tras la actualización del mapa, pues tendré más información.
            */ 
           posicion = getPosicion(agentID_vehiculos, mensajeEntrada.getSender());
           estado_actual = mensaje.get("estado").asInt();
           estados_vehiculos[posicion] = estado_actual;
                   
           if(informa) System.out.println(
                   "["+ this.getAid().getLocalName() + "]"
                   + "\n Mensaje recibido del Agente: ["
                   + mensajeEntrada.getSender().getLocalName()+"]"
                   + " (Posición: "+ posicion +", Estado: "+ estado_actual+") "
                   + print(mensajeEntrada));
           
           /**
            * En este momento tomo el sensor y actualizo el mapa con la 
            *  información que contiene.
            *  VERIFICO que se ha aportado nueva inforamación,
            *   en caso contrario induzco al vehículo a que pare su actividad.
            *  ¿Cómo?
            *  @Opción1: Le envio un mensaje diciéndole que no se mueva y por 
            *   ende queda a la espera de un nuevo mensaje.
            * 
            *  @Opcion2: No le envio mensaje de respuesta para que no inicie
            *   el movimiento siguiente.
            * @IMPORTANTE: Ver PRIMERO el estado del vehículo, porque resume
            * parte de su situación y sería de interés para actualizar el mapa 
            * o no ya que en los estados distintos de 0] explorando, no aporta
            * información al mapa y no necesita el burócrata deducirlo, pues 
            * ya está indicado en el estado del vehículo.
            */
           percepciones = mensaje.get("result").asObject();
           
           if(actualizarMapa(
                   mensajeEntrada.getSender(),
                   percepciones.get("sensor").asArray(),
                   percepciones.get("x").asInt(),
                   percepciones.get("y").asInt())){
               System.out.println(" Mapa actualizado con datos de "
                       + "["+ mensajeEntrada.getSender().getLocalName()+"]");
           }
           else{
               System.out.println(" Error al actualizar el mapa "
                       + "con los datos de ["
                       + mensajeEntrada.getSender().getLocalName()
                       + "]");
           }

       }
       
       try {
            /**
             * En este momento el mapa está actualizado, por tanto
             *  debe realizarse una imagen png para capturar la situación actual.
             */
             iteracion_actual++;
             capturaSituacion();
             /**
              * Tras actualizar el mapa, el burócrata mira las demás percepciones.
              *  En caso de percibir que el veículo quiere ayuda.
              *   El burócrata puede decidir enviar otro vehículo a explorar
              *    las cercanías o indicarle algunas coordenadas hacia donde
              *    dirigirse el para explorar los alrededores.
              *  En caso de pedir refuel.
              *   El burócrata cotejará la energía que hay para verificar que se
              *    puede realizar el refuel, mandándole el mensaje de realizar
              *    refuel.
              *  En caso de cambio de estado del vehículo de 0] explorando
              *   a otro estado de inactividad, el burócrata debe decrementar
              *   el indicador de vehiculos_activos y almacenar el agentID
              *   de dicho vehículo para poder reactivarlo en caso de no estar
              *   en modo CHASH
              *
              * @IMPORTANTE, se ha visto que en cada situación el burócrata
              * debe enviar un mensaje distinto.
              * La cuestión es cuando enviar dicho mensaje de respuesta.
              * enviarlo de forma inmediata al recibir las percepciones hace
              * entrar al vehículo en otra fase de comunicación. Pudiendo esta
              * interferir en el actual diálogo que tiene el burócrata con los
              * demás vehículos.
              * Por tanto creo conveniente responder a cada vehículo después
              * de haber recibido todas las percepciones, ello implica tener
              * una estructura que almacene durante la actualización del mapa
              * la situación de cada vehiculo y procesar dicha estructura
              * para configurar cada mensaje a enviar.
              */
            } catch (IOException ex) {
                Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
            }
       
       /**
        * @Nota: Ahora mismo, para comprobar que la comunicación es acorde
        * y no existe interbloqueos envio un mensaje genérico a cada vehiculo
        * mostrando el mensaje "OK, haz lo que debas hacer".
        */
       for(int i=0; i<vehiculos_activos; i++){
           mensaje = new JsonObject();
           mensaje.add("result","OK, haz lo que debas hacer");
           enviarMensaje(agentID_vehiculos.get(i), ACLMessage.INFORM, conversationID);
       }
       
       /**
        * Ahora es el momento donde los vehículos envían al controlador la 
        *  decisión que han tomado en base a sus sensores.
        * Por tanto el burócrata solo le queda esperar percepciones y enviar
        *  indicaciones según el estado de cada vehículo.
        * Por tanto el burócrata entra en un estad de escucha interminable
        * hasta que los vehiculos_activos sean 0
        *  Esto ocurre cuando todos llegan al destino,
        *  o cuando algunos llegan al destino y otros se han CRASHEADO
        *  o se les ha inducido inactividad.
        */
       System.out.println(this.toString());
        
        
    }
    
    /**
     * @author: Germán
     * Función auxiliar para encontrar el AgentID del vehículo en el ArrayList
     *  Motivo, Almacenar la situación actual del vehículo y responderle tras
     *   el proceso de actualización del mapa, para tener más información para 
     *   la toma de decisiónes.
     * @param lista   Lugar donde buscar al agente
     * @param agente  Agente a buscar
     * @return        Devuelve la posición donde se encientra el agente.
     * 
     * @POS Devuelve -1, si el agente a buscar no se ha encontrado.
     */
    private int getPosicion(ArrayList<AgentID> lista, AgentID agente){
        int posicion = -1;
        String nombre;
        for(int i=0; i<lista.size(); i++){
            nombre = lista.get(i).getLocalName();
            if(nombre.equals(agente.getLocalName()))
                posicion = i;
        }
        return posicion;
    }
    
    /**
     * @author: Germán
     * Método que encapsula la actualización del mapa
     * Pasos:
     *  1º] Calcula el rango de visión a partir del tamaño del sensor.
     *  2º] Determina el vehículo que es mediante su agentID
     *  3º] Se determina el color asignado al agente mediante si agentID
     *  4º] Cada color tiene un espectro asociado.
     *       He llamado espectro a la región de mapa vislumbrada.
     *  5º] Se transladan las coodenadas para ajustarlas al mapa png
     *  6º] Se realiza la actualización del mapa verificando que el dato
     *       del sensor es: 0, 1, 2, 3, 4
     *       en caso contrario respondería con -1 dando por fallida la
     *       actualización.
     *  7º] Se actualiza la posición donde se encuentra el vehículo con 
     *       su color asignado.
     * @IMPORTANTE Tras actualizar el mapa debe realizarse un archivo png
     *  para mostrar la situación actual.
     */
    private boolean actualizarMapa(AgentID agente, JsonArray sensor,
            int coordenadaX, int coordenadaY){
        boolean resultado = true;
        
        int rango = (int) Math.sqrt(sensor.size());
        System.out.println("["+ agente.getLocalName()+"]"
                + "\n Rango de visión: " + rango);
        
        /**
         * Dadas las coordenadas y el rango de visión, es posible
         * rellenar la sección de mapa vislumbrada.
         */
        int pos = getPosicion(agentID_vehiculos, agente);
        int x0 = coordenada(coordenadaX, rango);
        int y0 = coordenada(coordenadaY, rango);
        int tono;
        for(int c=0; c<rango && resultado; c++){
            for(int f=0; f<rango && resultado; f++){
                tono = color(pos, sensor.get(c+f*rango).asInt());
                if(tono != -1)
                    mapa.setRGB( x0+c, y0+f, tono);
                else
                    resultado = false;
            }
        }

        return resultado;
    }
    /**
     * @author: Germán
     * Método auxiliar para situar las coordenadas del vehículo correctamente
     *  en el mapa png, pues el tamaño real es 110x110 pixeles
     *  y el area de acción es de 100x100
     *  donde las coordenadas recibidas van de (0,0) a (99,99)
     *  cuando en el png eso implica (5,5) a (104, 104)
     *  Motivo, cuando se reciben la coordenadas de posición (37, 0)
     *   NO se puede rellenar correctamente el png pues 
     *   por debajo de 0 no se puede, pero el vehículo percibe más allá
     *  ¿Por qué? Por el desplazamiento de las coordenadas.
     *   Recibir (37, 0) significa que estamos situados en (42, 5);
     *   por lo que ya si es posible rellenar bien el mapa.
     * @param numero
     * @param rango
     * @return 
     */
    private int coordenada(int numero, int rango){
        int coordenada_inicial = -1;
        switch(rango){
            case 3:
                coordenada_inicial = numero + 4;
                break;
            case 5:
                coordenada_inicial = numero + 3;
                break;
            case 11:
                coordenada_inicial = numero;
                break;
            default:
                coordenada_inicial = -1;
                break;
        }
        return coordenada_inicial;
    }
    
    /**
     * @author: Germán
     * Método auxiliar que conmuta entre dos representaciones:
     *  - La representación del profesor: 
     *    0 libre, 1 obstáculo, 2 muro, 3 destino, 4 agente
     *  - La representación en png de los valores del profesor:
     *    0xffffff libre, 0x000000 obstáculo,
     *    0x000000 muro , 0xff0000 destino, 0xffffff agente.
     *  - Se pretende cambiar la representación libre por un color 
     *  similar al que posee el agente para indicar el rastro que ha seguido
     *  durante su exploración.
     */
    private int color(int pos, int valor_sensor){
        int resultado;
        switch(valor_sensor){
            case 0:
                resultado = espectros.get(pos);
                break;
            case 1:
            case 2:
                resultado = 0x000000;
                break;
            case 4:
                resultado = dorsales.get(pos);
                break;
            case 3: 
                resultado = 0xFF0000;
                break;
            default:
                resultado = -1;
        }
        return resultado;
    }
    
    /**
     * author: Germán
     * Método para crear una imagen png para capturar la situación actual del
     * mapa
     */
    private boolean capturaSituacion() throws IOException{
        String nombre_del_fichero = nombreFichero();
        File archivo = new File(nombre_del_fichero);
        boolean resultado =  ImageIO.write(mapa, "png", archivo);
        return resultado;
    }
    
    /**
     * @author: Germán
     * Método que encapsula la rutina de cancelar la suscripción al mapa.
     * @return 
     */
    private boolean cancel(){
        // Creando mensaje vacio
        mensaje = new JsonObject();
        
        // Enviando petición de cancelación de suscripción
        enviarMensaje(id_controlador,ACLMessage.CANCEL, conversationID);
        recibirMensaje();
        
        // Comprobando respuesta del Controlador
        boolean resultado = mensajeEntrada.getPerformativeInt() == ACLMessage.AGREE;
        
        // Mostrando los resultados del controlador
        int performativa = mensajeEntrada.getPerformativeInt();
        switch(performativa){
            case ACLMessage.AGREE:
                System.out.println(" Aceptada petición de CANCEL ");
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
        
        conversationID = "";
        return resultado;
        
    }
    /**
     * Método que encapsula la rutina de suscribirse a un mapa
     * @author: Germán
     * Método que encapsula la rutina de suscribirse al mundo
     * @param nombreMapa: Indica el mapa al que se quiere subscribirse
     */
    private void subscribe(String nombreMapa){
        mensaje = new JsonObject();
        mensaje.add("world", nombreMapa);
        
        enviarMensaje(id_controlador, ACLMessage.SUBSCRIBE, conversationID);
        
        recibirMensaje();
        conversationID = mensajeEntrada.getConversationId();
    }
    
    /**
     * @author: Germán
     * Método que gestiona la traza de sesiones anteriores usandola como 
     * lienzo de para la sesión actual.
     * @IMPORANTE: Este lienzo puede contener información de irrelevante,
     *  por ello debe limpiarse.
     */
    private void obtenerMapa(){
        try {
            if(!cancel()){
                subscribe(nombreMapa);
                cancel();
            }
            getTraza(true);
            mapa = getMapa("hex");
            limpiarMapa();
            
        } catch (IOException ex) {
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @author: Germán
     * Método auxiliar que limpia el mapa que hay en memoria.
     * @PRE: Debe haber un elemento Buffererd
     */
    private void limpiarMapa(){
        int gris = 0xc0c0c0;
        int muro = 0x000000;
        for(int c=0; c< mapa.getWidth(); c++)
            for(int f=0; f< mapa.getHeight(); f++){
                if(mapa.getRGB(c, f) != gris )
                    mapa.setRGB(c, f, gris);
                
// Dibuja los bordes                
//                if(f<5 || f>104) mapa.setRGB(c, f, muro);
//                if(c<5 || c>104) mapa.setRGB(c, f, muro);
            }
    }
    /**
     * @author: Germán
     * @PRE: La traza debe haberse recibido.
     * El método getTraza crea una imagen con la traza obtenida
     * @param exito: Indica el modo de tratamiento de la traza.
     *   TRUE --> Queremos la traza
     *   FALSE--> No queremos la traza pero debe gestionarse
     */
    private void getTraza(boolean exito){
        try {
            String nombreFichero = nombreFichero();
            
            if(exito){
                /* Recibimos la respuesta del servidor */
                recibirMensaje();
                rutaFichero = nombreFichero;
            }
            else{
                recibirMensaje();
                rutaFichero = nombreFichero
                        +"Error_traza_anterior_";
            }
            
//            System.out.println("Mensaje recibido, del servidor, tras el logout: " + mensaje.toString());
            
            /* Cuando la respuesta es OK, guardamos la traza */
            if (mensaje.toString().contains("trace")) {
                System.out.println("Recibiendo traza");
                JsonArray ja = mensaje.get("trace").asArray();
            byte data[] = new byte [ja.size()];
            for (int i = 0 ; i < data.length; i++) {
                data[i] = (byte) ja.get(i).asInt();
            }

            FileOutputStream fos = new FileOutputStream(rutaFichero);
            
            fos.write(data);
            fos.close();

            System.out.println("Traza Guardada en "+ nombreFichero);
            }
            
        } catch (IOException ex) {
            System.err.println("Error al recibir la respuesta o al crear la salida con la traza");
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
        }
    };
    
    /**
     * @author: Germán
     * Método que encapsula la tarea de crear un nombre para el mapa png
     * @IMPORTANTE 
     *  Se puede usar solo para obtener la situación final de la 
     *   exploración en el mapa.
     *  Pero su utilidad reside en crear mun nombre distinto para cada situación
     *   que se quiera capturar, pues agrupando todas ellas se logra visualizar
     *   la evolución de los agentes en la exploración del mapa.
     * 
     * @return Devuelve un nombre (con formato específico para poder hacer gif)  
     */
    private String nombreFichero(){
        // Creando carpeta contenedora de los pngs
        String nombre_carpeta = "../mapa_";
        File carpeta_pngs = new File(nombre_carpeta);
        if(carpeta_pngs.mkdir()){
            System.out.println("Directorio "+ "mapa_ "+"Creado ");
        }
        else{
            System.out.println("Directorio ya existente ");
        }
        
        String ruta = nombre_carpeta +"/";
        String fecha = fecha();
        String cadena = ruta
                + iteracion_actual + "__"
                + nombreMapa + "__" 
                + equipo + " "
                + fecha + "_"
                + mensajeEntrada.getConversationId();
        return cadena;
    }
    /**
     * @author: Germán
     * Método que muestra el contenido del Mapa, en dististos formatos.
     * con enteros, hexadecimal o con caracteres
     * @param nombre_archivo: Ubicación del archivo a decodificar y mostrar
     * @param tipo          : Formato de presentación ("int", "hex", "string" )
     * @return              : Devuelve BufferedImage 
     * @throws IOException  : Excepción que en esta versión no se controla.
     */
    private BufferedImage getMapa(String tipo) throws IOException{
        if(rutaFichero != ""){
            File imageFile = new File(rutaFichero);
            BufferedImage image = ImageIO.read(imageFile);
            System.out.println("Características de la imagen. "
                + "\n Tipo:    " + image.getType()
                + "\n Altura:  " + image.getHeight()
                + "\n Anchura: " + image.getWidth());
             
            int hex;
            for(int c=0; c<image.getWidth(); c++){
                for(int f = 0; f<image.getHeight(); f++){
                    hex = image.getRGB(f,c);
                    System.out.print(conversor(hex, tipo) + " ");
                }
                System.out.println("");
            }
            return image;
        }
        else{
            System.out.println("\n NO HAY RUTA !!! ");
            return null;
        }
        
    }
    
    /**
     * @author: Germán
     * Método auxiliar dar formato a un dato entero.
     * @param hex:   Valor del entero
     * @param tipo:  Tipo de representación elegida 
     * @return 
     */
    private static String conversor(int hex, String tipo){
        String dato;
        
        if(tipo == "int")
            {dato = Integer.toString(hex);}
        else if(tipo == "hex")
            {dato = Integer.toHexString(hex);}
        else {
            switch(hex){
                case -4144960 :
// ffc0c0c0 ==> -4144960                    
                    dato = "?";
                    break;
                    
                case -1:
// ffffffff ==> -1                    
                    dato = " ";
                    break;
            
                case -16777216:
// ff000000 ==> -16777216                    
                    dato = "1";
                    break;
                    
                case -65536:
// ffff0000 ==> -65536                    
                    dato = "x";
                    break;
                    
                 case -16726016:
// ff00c800 ==> -16726016                    
                    dato = ".";
                    break;
                 case -16842496:
                     dato = "G";
                     break;
                default:
// ff00c800 ==> -16726016
                    dato = "$";
                    break;
            }
        }
        
        return dato;
    }
    
    /**
     * @author: Germán
     * Método que encapsula la tarea de obtener la fecha y hora actual
     *  (solo usado para crear el nombre del archivo png que contendrá la traza)
     * @return: Devuelve la fecha y la hora en un String
     * @POST: Los caracteres '/' en el sistema Ubuntu 16.04 los interpreta como
     *  ruta, por ello se ha usado '-' como separador de fecha  
     */
    private String fecha(){
        Calendar c = Calendar.getInstance();
        int mes = c.get(Calendar.MONTH)+1;
        String fecha = + c.get(Calendar.DAY_OF_MONTH)+"-"
                + mes+"-"
                + c.get(Calendar.YEAR)+" "
                + c.get(Calendar.HOUR_OF_DAY)+":"
                + c.get(Calendar.MINUTE)+":"
                + c.get(Calendar.SECOND);
        return fecha;
    }
    
    /**
     * @author: Germán
     * Métod para mostrar el mapa que se tiene actualmente.
     * @param tipo 
     */
    private void ToStringMapa(String tipo){
        int hex; 
        for(int c=0; c<mapa.getWidth(); c++){
            for(int f = 0; f<mapa.getHeight(); f++){
                hex = mapa.getRGB(f,c);
                
                System.out.print(conversor(hex, tipo) + " ");
            }
            System.out.println("");
        }
    };
    
    
    private void getTrazaAnterior(){}
        
    private void dirigeteA(Casilla c){}
    
    
    
    
    
    
    /*************************************************************************/
    /*
        Tareas de Alex
    */
    /*************************************************************************/
    
    private void pathFinder() {
        
    }
    
    
    private void quadTree() {
        
    }
    
    
}
