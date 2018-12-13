/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
public class Burocrata extends Agente{

    private AgentID id_controlador;
    private String nombreMapa;
    private String equipo;
    private ArrayList<AgentID> agentID_vehiculos;
    private int vehiculos_activos;
    private int VEHICULOS_MAX;
    
    private BufferedImage mapa;
    private String rutaFichero;

    
//    private String[] conversationID_vehiculos ;
    private int[] estados_vehiculos ;
    private int tamanio_mapa ;
    
    /**
     * @author: Germán
     * Método inicializador de los atributos del burócrata. 
     * @param aID:            Identificador del agente
     * @param nombreServidor: Nombre del controlador 
     *                         con el que se comunicará el agente 
     * @param nombreMapa:     Nombre del mapa a explorar
     * @throws Exception 
     */
    private void inicializar(String nombreServidor, String nombreMapa,
            ArrayList<AgentID> vehiculos) 
            throws Exception {
//       System.out.println("\n INICIALIZACION DEL BUROCRATA");
        this.id_controlador = new AgentID(nombreServidor);
        conversationID = "";
        
        /**
         * Hasta que no se realice el chekin por parte de los exploradores
         * no se conoce la configuración de equipo.
         */
        equipo = "";
        vehiculos_activos = 0;
        VEHICULOS_MAX = vehiculos.size();
        
        
        agentID_vehiculos = new ArrayList<>();
        for(int i=0; i< VEHICULOS_MAX; i ++){
               agentID_vehiculos.add(vehiculos.get(i));
           }
        
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
    public Burocrata(AgentID aID, String nombreServidor,
            ArrayList<AgentID> vehiculos, String nombreMapa
            )
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
    public Burocrata(AgentID aID, String nombreServidor,
            ArrayList<AgentID> vehiculos, String nombreMapa, 
            boolean informa)
        throws Exception{
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
         */
       String vehiculo_registrado = "";
       
       for(int i = 0; i<VEHICULOS_MAX; i++){
           recibirMensaje();
           if(mensajeEntrada.getPerformativeInt() == ACLMessage.INFORM){
               vehiculo_registrado = "Agente: " 
                       + mensajeEntrada.getSender().getLocalName()
                       + " resistrado";
        
               vehiculos_activos++;
               System.out.println(vehiculo_registrado);
               System.out.println("equipo"+ equipo);
        
               equipo += mensaje.get("fuelrate");
               System.out.println("equipo: "+ equipo);     
           }
           else{
               System.out.println(
                       " Se ha recibido la performativa: " 
                        + mensajeEntrada.getPerformativeInt());
           }
       }

       
       /** 
        * @Nota: En este momento los vehículos están en disposición de pedir 
        * las percepciones al controlador, por tanto el agente burócrata
        * espera recibir dichas percepciones
        * 
        */
        System.out.println("AHORA TOCA pedir las percepciones de los vehiculos "
        + " al controlador y esperar a recibir el scanner de cada uno ");
       
        System.out.println(this.toString());
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
        System.out.println("conversationID: "+ conversationID);
    }
    
    private void obtenerMapa(){
        try {
            if(!cancel()){
                subscribe(nombreMapa);
                cancel();
            }
            getTraza(true);
            mapa = getMapa(rutaFichero, "hex");
            
        } catch (IOException ex) {
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
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
            String fecha = fecha();
            String nombreFichero =nombreMapa + "__" 
                    + equipo + " "
                    + fecha + "_"
                    + mensajeEntrada.getConversationId();
            
            if(exito){
                /* Recibimos la respuesta del servidor */
                recibirMensaje();
                rutaFichero= "../" 
                        + nombreFichero;
            }
            else{
                recibirMensaje();
                rutaFichero= "../" 
                        +"Error_traza_anterior_"
                        +nombreFichero;
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
     * Método que muestra el contenido del Mapa, en dististos formatos.
     * con enteros, hexadecimal o con caracteres
     * @param nombre_archivo: Ubicación del archivo a decodificar y mostrar
     * @param tipo          : Formato de presentación ("int", "hex", "string" )
     * @return              : Devuelve BufferedImage 
     * @throws IOException  : Excepción que en esta versión no se controla.
     */
    private static BufferedImage getMapa(String nombre_archivo, String tipo) throws IOException{
        File imageFile = new File(nombre_archivo);
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
}
