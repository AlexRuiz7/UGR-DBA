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

    String separador_linux = "/";
    String separador_whidows = "\\";
    String separador = separador_linux;
    
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
    private ArrayList<Integer> dorsales;
    private ArrayList<Integer> espectros;

    private ArrayList<Integer> estados_vehiculos;
    private ArrayList<Boolean> refuel_vehiculos;
    private ArrayList<Integer> fuelrate_vehiculos;
    private ArrayList<JsonObject> percepciones_vehiculos;
    private int vehiculos_en_movimiento;
    private ArrayList<JsonObject> mensajes_respuesta;

    /**
     * @author: Germán Método inicializador de los atributos del burócrata.
     * @param aID: Identificador del agente
     * @param nombreServidor: Nombre del controlador con el que se comunicará el
     * agente
     * @param nombreMapa: Nombre del mapa a explorar
     * @throws Exception
     */
    private void inicializar(String nombreServidor, String nombreMapa, ArrayList<AgentID> vehiculos)
            throws Exception {
        //       System.out.println("\n INICIALIZACION DEL BUROCRATA");
        this.id_controlador = new AgentID(nombreServidor);
        conversationID = "";
        iteracion_actual = 0;

        /**
         * Hasta que no se realice el chekin por parte de los exploradores no se
         * conoce la configuración de equipo.
         */
        equipo = "";
        vehiculos_activos = 0;
        VEHICULOS_MAX = vehiculos.size();

        agentID_vehiculos = new ArrayList<>();
        percepciones_vehiculos = new ArrayList<>();
        mensajes_respuesta = new ArrayList<>();
        estados_vehiculos = new ArrayList<>();
        refuel_vehiculos = new ArrayList<>();
        fuelrate_vehiculos = new ArrayList<>();
        vehiculos_en_movimiento = 0;

        for (int i = 0; i < VEHICULOS_MAX; i++) {
            agentID_vehiculos.add(vehiculos.get(i));

            estados_vehiculos.add(-1);
            refuel_vehiculos.add(false);
            fuelrate_vehiculos.add(0);
            percepciones_vehiculos.add(new JsonObject());
            mensajes_respuesta.add(new JsonObject());
        }

        /**
         * Colores para representar: - la ubicación de cada vehículo - su
         * espectro de visión
         */
        dorsales = new ArrayList<>();
        dorsales.add(0xFF0000);
        dorsales.add(0x00cc00);
        dorsales.add(0x0000FF);
        dorsales.add(0xFF00FF);

        espectros = new ArrayList<>();
        espectros.add(0xFFdddd);
        espectros.add(0xccFFcc);
        espectros.add(0xccccFF);
        espectros.add(0xFFccFF);

        /**
         * Necesario para crear el nombre del archivo que contendrá la traza
         */
        this.nombreMapa = nombreMapa;
        this.rutaFichero = "";
    }

    /**
     * @author: Germán Constructor donde no se explicita querer ser informado de
     * toda la comunicación del agente.
     * @param aID: Identificador del agente
     * @param nombreServidor: Nombre del controlador con el que se comunicará el
     * agente
     * @param nombreMapa: Nombre del mapa a explorar
     * @param vehiculos
     * @throws Exception
     */
    public Burocrata(AgentID aID, String nombreServidor, ArrayList<AgentID> vehiculos,
            String nombreMapa)
            throws Exception {
        super(aID, false);
        inicializar(nombreServidor, nombreMapa, vehiculos);
    }

    /**
     * @author: Germán Constructor donde se indica explicitamente el deseo de
     * ser informado de toda la comunicación de este agente.
     * @param aID: Identificador del agente
     * @param nombreServidor: Nombre del controlador con el que se comunicará el
     * agente
     * @param vehiculos
     * @param nombreMapa: Nombre del mapa a explorar
     * @param informa: Con valor TRUE informa de toda comunicación.
     * @throws Exception
     */
    public Burocrata(AgentID aID, String nombreServidor, ArrayList<AgentID> vehiculos,
            String nombreMapa, boolean informa) throws Exception {

        super(aID, informa);
        inicializar(nombreServidor, nombreMapa, vehiculos);
    }

    /**
     * @author: Germán Método que ejecutará el agente desde que despierta.
     *
     * @Nota 9-12-2018: Probando los métodos subscribe, cancel y obtener mapa
     */
    @Override
    public void execute() {
        /**
         * Inicialmente se pretende mandar un mensaje de cancel para cerrar la
         * sesión anterior y empezar la nueva sesión. Hay dos situaciones
         * posibles al enviar el cancel. AGREE: Hubo una sesión anterior que no
         * se cerró correctamente.
         *
         * NOT_UNDERSTOOD: Hubo una sesión anterior que se cerró correctamente o
         * es la primera sesión en este mapa.
         *
         * Se puede aprovechar dicha situación para obtener las dimensiones del
         * mapa a partir de la traza. Para ello: se cierra la sesión anterior y
         * se toman las dimensiones o se inicia una subscripción y
         * posteriormente un cancel.
         *
         * @Nota: La traza de la sesión anterior puede esta "manchada"
         *
         */
        obtenerMapa();

        //ToStringMapa("hex");
        /**
         * @Nota: En este momento el burócrata no conoce el conversationID
         * necesario para que los vehículos realicen checkin. Por tanto hay dos
         * opciones:
         * @Opción1: Que los vehículos pregunten al burócrata por el
         * conversationID. Lo que implicaría que el burocrata respondiese que no
         * lo tiene o que inicie una comunicación con el controlador. Lo que nos
         * lleva a una situación donde el burócrata espera respuesta del
         * controlador y de los vehículos. Por tanto hay riego de interbloqueo.
         * ¿Comó evitarlo? Entrando el burócrata en un bucle donde solamente
         * saldrá cuando reciba el conversationID y rechazando todos los demás
         * mensajes. Lo que conlleva que los vehículos entren en otro bucle del
         * cual no saldrán mientras se les rechace; saldrán cuando reciban un
         * mensaje con el conversationID.
         * @IMPORTANTE: Hay una limitación en la comunicación por parte de la
         * librería magentix que es de 65535 mensajes.
         *
         * @Opción2: Que los vehículos esperen a recibir el conversationID. Ello
         * implica que el burócrata debe suscribirse y después enviar el
         * conversationID a los vehículos. Por tanto no hay riesgo de
         * interbloqueo ni de superar el límite de mensajes.
         */
        // Suscibriendose al mapa
        subscribe(nombreMapa);

        /**
         * @Nota: En este momento el controlador queda a la espera del checkin
         * de los vehículos, y los vehículos desconocen el conversationID con el
         * cual poder comunicarse con el controlador.
         * @Nota: El burócrata lo conoce o está en comunicación para obtenerlo.
         * En este momento hay dos opciones:
         * @Opción1: Ser los vehículos quien pregunten por el conversationID con
         * el riesgo de enviar el mensaje antes de haber acabado la comunicación
         * del burócrata con el controlador en el subscribe.
         * @Reflexión ¿Cómo evitar recibir el mensaje antes de tiempo y por ende
         * evitar el interbloqueo? El mensaje se va a recibir si o si, no hay
         * manera de sincronizarlos Lo que si se puede hacer es verificar que:
         * "se está recibiendo lo que se espera recibir" y en otro caso enviar
         * REFUSE. Ello implica que el vehículo entra en un bucle while, del
         * cual saldrá cuando reciba una performativa distinta a REFUSE.
         * @IMPORTANTE: Tenemos el problema del limite de mensajes de 65536
         *
         * @Opción2: Ser el burócrata quien envíe el mensaje a los vehículos,
         * ello implica que solo se enviará el mensaje del conversationID cuando
         * el burócrata termina la conversación con el controlador, evitando así
         * el interbloqueo.
         * @IMPORTANTE: El riego de llegar a 65535 mensajes no está presente.
         */
        // Envia mensaje a los agentes con el conversationID 
        mensaje = new JsonObject();
        mensaje.add("result", "Enviando conversationID");
        for (int i = 0; i < VEHICULOS_MAX; i++) {
            enviarMensaje(agentID_vehiculos.get(i), ACLMessage.INFORM, conversationID);
        }

        /**
         * Ahora es el momento de esperar al checkin de los agentes para ser
         * informados de sus capacidades. ¿Quédebe hace el burócrata? Hay dos
         * opciones:
         *
         * @Opción1: El burócrata pide las capacidades a los vehículos, con el
         * riesgo de enviar el mensaje antes de haber terminado el vehículo la
         * conversación con el controlador (durante el checkin) provocando
         * interbloqueo.
         * @Reflexión: ¿Cómo evitarlo? Siendo rechazadas las peticiones del
         * burócrata entrando en un bucle de pregunta y rechazo del cual saldrá
         * cuando el vehículo envie sus capacidades al burócrata, esto ocurrirá
         * cuando el vehículo termine la conversación con el controlador.
         *
         * @Opción2: El burócrata espera a que el vehículo le envie sus
         * capacidades, esto ocurrirá cuando el vehículo termine la conversación
         * con el controlador.
         */
        /**
         * @PRE: Se presupone que los agentID de los agentes recibidos en el
         * constructor aún no se han movido y por tanto no están CRASHEADOS, y
         * aún no han pedido las percepciones al controlador.
         *
         * @Nota: En este momento los vehículos están en disposición de pedir
         * las percepciones al controlador y por tanto el agente burócrata
         * espera recibir dichas percepciones.
         *
         * Para seguir con el criterio de: "el último que recibe, es el
         * siguiente en enviar " Toca no presuponer que el vehiculo pedirá las
         * percepciones y por tanto el burócrata le enviará un mensaje de "OK"
         * simbolizando que "OK, he recibido tus capabilities "
         *
         */
        int pos;
        int fuelrate;
        for (int i = 0; i < VEHICULOS_MAX; i++) {
            recibirMensaje();
            if (mensajeEntrada.getPerformativeInt() == ACLMessage.INFORM) {
                vehiculos_activos++;
                vehiculos_en_movimiento++;
                fuelrate = mensaje.get("consumo").asInt();
                equipo += mensaje.get("consumo");

                pos = getPosicion(agentID_vehiculos, mensajeEntrada.getSender());
                fuelrate_vehiculos.set(pos, fuelrate);

                if (informa) {
                    System.out.println("[" + this.getAid().getLocalName() + "]"
                            + "Agente: "
                            + mensajeEntrada.getSender().getLocalName()
                            + " resistrado");
                }

            } else {
                System.out.println(
                        "\n Performativa distina a INFORM mientras recibo las "
                        + " capacidades de los agentes"
                        + "\n Se ha recibido la performativa: "
                        + mensajeEntrada.getPerformativeInt());
            }
        }

        if (informa) {
            System.out.println(" El equipo es: " + equipo);
        }

        for (int i = 0; i < vehiculos_activos; i++) {
            mensaje = new JsonObject();
            mensaje.add("result", "OK, ya está el equipo completo registrado.");
            enviarMensaje(agentID_vehiculos.get(i), ACLMessage.INFORM, conversationID);
        }

        /**
         * En este momento los vehículos pueden pedir las percepciones del mapa
         * al controlador y es el burócrata quien está a la espera de recibir
         * dichas percepciones para actualizar el mapa que gestiona, y decidir
         * las estrategias a seguir, que son: 1] Recibir la mayor información
         * posible de cada vehículo. Lo que implica tener un control mínimo de
         * cantidad de información que éste aporta. 2] Ayudar a un vehículo si
         * éste pide ayuda, habiendo dos estrategias: 2.1] El burócrata busca a
         * otro vehículo para acercarse al que está pidiendo ayuda. Con ello se
         * presupone que el burócrata tendrá más mapa descubierto con lo que
         * puede decidir hacia donde dirigir al vehículo a auxiliar. 2.2] El
         * burócrata busca pero no encuentra a otro (vehículo) por lo que
         * dirigirá al vehiculo a auxiliar a una zona inexplorada
         */
        int posicion;
        int estado_actual;
        boolean pide_refuel;
        JsonObject percepciones;
        int energia = -1;
        while (vehiculos_activos > 0) {
            System.out.println("VEHICULOS EN MOVIMIENTO " + vehiculos_en_movimiento);
            for (int i = 0; i < vehiculos_en_movimiento; i++) {
                // Esperando percepciones de cada vehiculo
                recibirMensaje();

                /**
                 * Almaceno el estado del vehículo para responderle tras la
                 * actualización del mapa, pues tendré más información.
                 */
                posicion = getPosicion(agentID_vehiculos, mensajeEntrada.getSender());
                System.out.println(" Candidato: " + posicion);
                estado_actual = mensaje.get("estado").asInt();
                estados_vehiculos.set(posicion, estado_actual);

                pide_refuel = mensaje.get("refuel").asBoolean();
                refuel_vehiculos.set(posicion, pide_refuel);

                if (informa) {
                    System.out.println(
                            "[" + this.getAid().getLocalName() + "]"
                            + "\n Mensaje recibido del Agente: ["
                            + mensajeEntrada.getSender().getLocalName() + "]"
                            + " (Posición: " + posicion + ", Estado: " + estado_actual + ") "
                            + print(mensajeEntrada));
                }
                /*
            * En este momento mediante la variable 'posicion' puedo identificar
            *  en qué lugar del array agentID_agentes se encuantra el
            *  vehículo actual, con ello he podido extraer su estado he inclirlo
            *  en el array 'estados_vehiculos' y lo mismo con 
            *  la variable percepciones, es decir,
            *  uso 'posicion' para modificar el contenido del mensaje que hay
            *  en percepciones_vehiculos, para su posterios tratamiento al 
            *  terminar de actualizar el mapa.
                 */
//           System.out.println(" ESTADO ACTUAL: "+ estado_actual);
                if (estado_actual == 0) {
                    percepciones = mensaje.get("result").asObject();

                    if (energia == -1) {
                        energia = percepciones.get("energy").asInt();
                        System.out.println("\n Energía actual: " + energia + "\n");
                    }

                    /*        
                 Creo conveniente coger la energía en este momento ya que            
                  cualquier mensaje que proceda de vehiculos_en_movimiento 
                  es información actualizada.
                     */
                    /**
                     * En este momento tomo el sensor y actualizo el mapa con la
                     * información que contiene. VERIFICO que se ha aportado
                     * nueva inforamación, en caso contrario induzco al vehículo
                     * a que pare. ¿Cómo?
                     *
                     * @Opción1: Le envio un mensaje diciéndole que no se mueva
                     * y por ende queda a la espera de un nuevo mensaje.
                     *
                     * @Opcion2: No le envio mensaje de respuesta para que no
                     * inicie el movimiento siguiente.
                     * @IMPORTANTE: Ver PRIMERO el estado del vehículo, porque
                     * resume parte de su situación y sería de interés para
                     * actualizar el mapa o no ya que en los estados distintos
                     * de 0] explorando, no aporta información al mapa y no
                     * necesita el burócrata deducirlo, pues ya está indicado en
                     * el estado del vehículo.
                     */
                    int pixeles_descubientos = actualizarMapa(
                            mensajeEntrada.getSender(),
                            percepciones.get("sensor").asArray(),
                            percepciones.get("x").asInt(),
                            percepciones.get("y").asInt());

                    if (pixeles_descubientos > 0) {
                        System.out.println(" Mapa actualizado con datos de "
                                + "[" + mensajeEntrada.getSender().getLocalName() + "]");
                    } else if (pixeles_descubientos == 0) {

                        System.out.println(
                                "\n [" + mensajeEntrada.getSender().getLocalName() + "]"
                                + " No has descubierto nada nuevo. "
                                + " Considero que necesitas ayuda. ");
                        estados_vehiculos.set(posicion, 2);
                    } else if (pixeles_descubientos == -1) {
                        System.out.println(" Error al actualizar el mapa "
                                + "con los datos de ["
                                + mensajeEntrada.getSender().getLocalName()
                                + "]");
                    } else {
                        System.out.println(" Valor recibido fuera del rango de "
                                + "interpretación ("
                                + pixeles_descubientos
                                + ")");
                    }
                    //Percepciones dentro del bucle.
                    if (informa) {
                        System.out.println(" Percepciones de ["
                                + mensajeEntrada.getSender().getLocalName()
                                + "]"
                                + percepciones.toString());
                    }

                } else {
                    percepciones = new JsonObject();
                }

                System.out.println("TODAS LAS PERCEPCIONSES DENTRO DEL BUCLE");
                percepciones_vehiculos.set(posicion, percepciones);
                System.out.println("HOLA " + i);
            }

            System.out.println("TODAS LAS PERCEPCIONSES FUERA DEL BUCLE");
            // Percepciones fuera del bucle for 
            for (int i = 0; i < vehiculos_activos && informa; i++) {
                System.out.println(
                        "\n Agente: " + agentID_vehiculos.get(i).getLocalName()
                        + "\n Estado: " + estados_vehiculos.get(i)
                        + "\n Percepciones obtenidas: "
                        + "\n\t " + percepciones_vehiculos.get(i).toString());

            }
            try {
                /**
                 * En este momento el mapa está actualizado, por tanto debe
                 * realizo una imagen png para capturar la situación actual.
                 * además incremento el contador 'iteración_actual' para ordenar
                 * las capturas y poder realizar el gif con mayor comodidad
                 */
                iteracion_actual++;
                System.out.println("\n [ITERACION] " + iteracion_actual + "\n");
                capturaSituacion();

                /**
                 * Tras actualizar el mapa, el burócrata mira las demás
                 * percepciones. como el estado y el refuel de cada vehículo y
                 * para ello hará uso del atrubuto percepciones_vehiculos En
                 * caso de percibir que el veículo quiere ayuda. El burócrata
                 * puede decidir enviar otro vehículo a explorar las cercanías o
                 * indicarle algunas coordenadas hacia donde dirigirse para
                 * explorar los alrededores. En caso de pedir refuel. El
                 * burócrata cotejará la energía que hay para verificar que se
                 * puede realizar el refuel, mandándole el mensaje de realizar
                 * refuel. En caso de cambio de estado del vehículo de 0]
                 * explorando a otro estado de inactividad, el burócrata debe
                 * decrementar el indicador de vehiculos_en_movimiento y tener
                 * presente dicho estado para no enviar mensaje al vehículo.
                 *
                 * @Nota: En el bucle donde envia todos los mensajes a los
                 * vehículos basta verificar antes el estado para evitar el
                 * envio.
                 * @Nota: En esta versión un vehículo que ha llegado al objetivo
                 * Se le considera vehículo inmovil (incapaz de moverse)
                 * @Nota: Si hay tiempo se le tendrá en consideración como
                 * candidato a prestar ayuda.
                 *
                 * @Reflexión, se ha visto que en cada situación el burócrata
                 * debe enviar un mensaje distinto. La cuestión es cuando enviar
                 * dicho mensaje de respuesta. enviarlo de forma inmediata al
                 * recibir las percepciones hace entrar al vehículo en otra fase
                 * de comunicación. Pudiendo esta interferir en el actual
                 * diálogo que tiene el burócrata con los demás vehículos. Por
                 * tanto creo conveniente responder a cada vehículo después de
                 * haber recibido todas las percepciones, ello implica tener una
                 * estructura que almacene, durante la actualización del mapa,
                 * la situación de cada vehiculo y procesar dicha estructura
                 * para configurar cada mensaje a enviar.
                 */
            } catch (IOException ex) {
                Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
            }

            /**
             * PASOS a seguir para procesar las percepciones almacenadas durante
             * la actualización del mapa. 1º] Recorrer el array de percepciones
             * o de estados para ver el estado de cada vehículo. Ello nos
             * indicará los agentes actualmente activos.
             *
             * 2º] Buscar el vehículo que pida refuel y comprobar si hay enegía
             * para ello. (En este momento no entro en complejidades, si hay
             * energía se adjunata al mensaje la confirmación)
             *
             * 3º] Buscar el vehículo que necesita ayuda y decidir enviar a otro
             * en su auxilio.
             *
             * @NOTA: ¿Puede un vehículo que pide refuel ser enviado a ayudar?
             * Sería una opción válida. ¿Cómo hacerlo? Con varior recorridos,
             * rellenando los campos de forma sistemática. El primero para saber
             * los vehículos activos La segunda para adjuntar la confirmación o
             * rechazo de refuel. La tercera para seleccionar al vehículo
             * candidato a ayudar La cuarta para indicar a donde debe dirigirse
             * cada vehículo. Aclaracion: - No enviar coordenadas a los
             * vehiculos activos significa que pueden seguir explorando. - No
             * enviar un mensaje al vehículo que pide ayuda provocaría
             * ocasionaría un bloqueo en éste. ¿Cuando indicarle a dicho
             * vehiculo que se puede mover? -] Cuando el burócrata decida
             * enviarle coordenadas ¿Cuando será eso? Hay dos situaciones que se
             * contemplan. Cuando NO hay vehículos disponibles para ayudar el
             * burócrata de envia un mensaje con coordenadas de un lugar aún sin
             * explorar. Cuando SI hayvehículos disponnibles para ayudar el
             * burócrata enviará a uno para obtener más información sobre el
             * mapa y poder así decidir a donde enviar el vehículo a auxiliar.
             * Ese momento será cuando el vehículo auxiliador detecta en su
             * sensor la presencia del otro vehículo. por tanto debe ser en ese
             * momento donde enviale las nuevas coordenada para dirigirlo a
             * algún lugar inexplorado. ¿Como detectar eso? El vehículo
             * auxiliador manda en su mensaje de percepciones las coordenadas
             * del auxiliado, cuya clave será auxiliado_x, auxiliado_y -
             * Enviarle una coordenadas al vehiculo seleccionado para ayudar
             * Pero ¿Qué estado debería tener desde que empieza la ayuda hasta
             * que llega?
             * @RESPUESTA: El estado de activo pero ocupado. Y cuando llega al
             * destino ¿Qué estado debe tener?
             * @RESPUESTA: El de activo y candidato a ayudar.
             */
            int inmovil = 0;
            ArrayList<Integer> candidatos_para_ayudar = new ArrayList();
            ArrayList<Integer> vehiculos_pidiendo_ayuda = new ArrayList();
            /**
             * Versión en la que un vehículo que ha llegado a la meta se le
             * considera candidato a ayudar for(int i=0; i<VEHICULOS_MAX; i++){
             * if(estados_vehiculos.get(i) >=5) inmovil++;
             * if(estados_vehiculos.get(i) == 0 || estados_vehiculos.get(i) == 2
             * ) candidatos_para_ayudar.add(i); if(estados_vehiculos.get(i) ==
             * 4) vehiculos_pidiendo_ayuda.add(i); }
             */
            // PRE: estados_vehiculos.size() == VEHICULOS_MAX
            for (int i = 0; i < estados_vehiculos.size(); i++) {
                if (estados_vehiculos.get(i) >= 3 || estados_vehiculos.get(i) == 1) {
                    inmovil++;
                }
                if (estados_vehiculos.get(i) == 2) {
                    vehiculos_pidiendo_ayuda.add(i);
                }
                if (estados_vehiculos.get(i) == 0) {
                    candidatos_para_ayudar.add(i);
                }
            }

            // Los vehiculos_activos se pueden mover ahora ó más tarde
            vehiculos_activos = VEHICULOS_MAX - inmovil;
//        vehiculos_en_movimiento = vehiculos_activos - vehiculos_pidiendo_ayuda.size();

            if (informa) {
                System.out.println(" Vehiculos activos: " + vehiculos_activos);
                System.out.println(" Vehiculos en movimiento: " + candidatos_para_ayudar.size());
                System.out.println(" Vehiculos pidiendo ayuda: " + vehiculos_pidiendo_ayuda.size());
            }
            /* Los vehículos_en_movimiento son los únicos que
         *  se han movido en esta iteración
         *  y se van a mover en la próxima iteración
             */
            // Índice_del_necesitado --> i_necesitado
            int i_necesitado;
            if (vehiculos_pidiendo_ayuda.size() > 0) {
                //SI hay alguien pidiendo ayuda
                if (informa) {
                    System.out.println(" Hay pidiendo ayuda: "
                            + vehiculos_pidiendo_ayuda.size());
                }

                for (int i = 0; i < vehiculos_pidiendo_ayuda.size(); i++) {
                    System.out.println("Dentro del for");
                    i_necesitado = vehiculos_pidiendo_ayuda.get(i);
                    int elegido = candidatoMasCercano(
                            percepciones_vehiculos,
                            candidatos_para_ayudar, i_necesitado);
                    if (elegido != -1) {
                        JsonObject respuesta = new JsonObject();
                        respuesta.add("result", "OK, ayuda al vehículo ");
                        respuesta.add("dirigete_a", true);
                        respuesta.add("x", percepciones_vehiculos
                                .get(i_necesitado).get("x"));
                        respuesta.add("y", percepciones_vehiculos
                                .get(i_necesitado).get("y"));

                        mensajes_respuesta.set(elegido, respuesta);
                        candidatos_para_ayudar.remove(elegido);
                    } else {
                        System.out.println("Buscando Coordenadas ");
                        JsonObject respuesta = buscarCoordenadas(
                                percepciones_vehiculos.get(i_necesitado));

                        mensajes_respuesta.set(i_necesitado, respuesta);
                        System.out.println("Coordenadas asignadas");

                        /* Al enviarle coordenadas el burócrata, 
                        se moverá, por tanto lo considero vehiculo_en_movimiento
                         */
                        estados_vehiculos.set(i_necesitado, 0);
                    }
                }
            } else {
                // NO hay nadie pidiendo ayuda
                if (informa) {
                    System.out.println(" No hay nadie pidiendo ayuda ");
                }
                for (int i = 0; i < candidatos_para_ayudar.size(); i++) {
                    i_necesitado = candidatos_para_ayudar.get(i);
                    JsonObject respuesta = new JsonObject();
                    respuesta.add("result", "OK, haz lo que debas hacer");
                    mensajes_respuesta.set(i_necesitado, respuesta);
                }
            }

            /**
             * @OPCION elegida Previo recuento de vehiculos Inactivos
             * permanentemente (Inmóviles). TODOS = VEHICULOS_MAX - Inmóviles
             *
             * ¿Hay vehiculo pidiendo ayuda? SI hay ==> ¿Hay vehículo
             * disponible? SI hay ==> ¿Cual mando? --> El más cercano. NO hay
             * ==> ¿Qué hago? ----> Dale coordenadas NO hay ==> ¿Eres vehiculo
             * activo? SI ==> ¿Qué hago? ----> Déjalo hacer lo que estén
             * haciendo NO ==> ¿Te puedes reactivar? SI ==> ¿Estás en la meta?
             * SI ==> ¿Estaís TODOS? SI ==> Cancel() NO ==> Dale coordenadas. NO
             * ==> Inactivo permanente.
             *
             * Cuando todos los vehículos están inactivos se realiza cancel.
             *
             * @Nota: El inactivo permanente implica que el equipo se ha quedado
             * sin ese miembro y por tanto cuando se pregunta por ¿TODOS?, a él
             * no se le tiene en cuenta. Es decir, se necesita un contador de
             * impedidos o realizar un conteo previo de los que están en juego.
             *
             * @Nota: Ahora se hace recuento de vehículos_activos tanto los que
             * ya estaban activos, como los que se han activado.
             *
             * @Nota: en vez de utilizar centinelas, mejor utilizar arrays para
             * incluir a los que piden ayuda y a los candidatos a ofrecerla.
             * Para poder crear los mensajes
             */
            /*
        * @Nota: Los vehículos_en_movimiento han podido variar,
        *  espercialmente si un vehículo ha pedido ayuda
        *  (no se considera en movimiento) pero no ha habido vehiculos para 
        *   prestar su ayuda, por tanto el burócrata ha considerado enviarle
        *   coordenadas para ponerlo en marcha, ello implica que ahora si se le 
        *   cosidera en movimiento y por tanto debe realizarse un recuento.
        *
       System.out.println("RECUENTO");
       vehiculos_en_movimiento = 0;
       for(int i=0; i<VEHICULOS_MAX; i++){
           if(estados_vehiculos.get(i) == 0)
               vehiculos_en_movimiento++;
       }
        System.out.println(" Actualmente hay "
                + vehiculos_en_movimiento 
                + " vehiculos_en_movimiento ");
             */
 /* En esta momento el burócrata ha especificado qué debe hacer
           cada vehículo, es decir,
            -] A los que piden ayuda, enviarles ayuda (si la hay).
                bloqueando al que está pidiendo ayuda.
            -] A los que piden ayuda, enviarles coordenadas (si no hay ayuda)
            -] A los que expliran, dejarlos explorar. 
         
         Ahora seria una buena ocasión para indicarles refuel a los 
          vehículos en movimiento que lo pidan.
          En esta versión no voy a entrar en complejidades, quien quiera, tiene.
             */
            for (int i = 0; i < VEHICULOS_MAX; i++) {
                if (estados_vehiculos.get(i) == 0
                        && refuel_vehiculos.get(i)) {
                    if (informa) {
                        System.out.println(" Solicitud de refuel. "
                                + " Energia actual: "
                                + energia);
                    }
                    /*FIXMI hay que controlar la energia mientra de dismensa */
                    fuelrate = fuelrate_vehiculos.get(i);
                    if (fuelrate < energia) {
                        JsonObject respuesta = mensajes_respuesta.get(i);
                        respuesta.add("refuel", true);
                        mensajes_respuesta.set(i, respuesta);

                        energia = energia - fuelrate;
                        if (informa) {
                            System.out.println("\t Aceptada solicitud para "
                                    + "["
                                    + agentID_vehiculos.get(i).getLocalName()
                                    + "]");
                            System.out.println(" Energía restante: " + energia);
                        }
                    }
                }
            }

            //Impongo que se actualice el valor de la energía en cada iteración.
            energia = -1;
            /**
             * @Nota: Ahora mismo, para comprobar que la comunicación es acorde
             * y no existe interbloqueos envio un mensaje genérico a cada
             * vehiculo mostrando el mensaje "OK, haz lo que debas hacer".
             *
             * @Modificación 17-12-2018: Ya está realizada la funcionalidad para
             * que el burócrata envie los mensajes a los vehiculos ahora toca
             * enviar mensajes a los vehículos en estado de exploración y a los
             * que están en estado de ayuda pero se les ha enviado coordenadas
             * porque no hay auxiliadores.
             *
             */
            /**
             * FIXMI ¿Se podría hacer un recuento de vehículos en movimiento?
             * Asçi solo se enviarían mensajes a los que realmente los
             * gestionaran
             */
            System.out.println("AHORA toca enviar los mensaje creados");
            for (int i = 0; i < VEHICULOS_MAX && vehiculos_activos > 0; i++) {
                if (estados_vehiculos.get(i) == 0) {
                    mensaje = new JsonObject();
                    mensaje = mensajes_respuesta.get(i);
                    if (informa) {
                        System.out.println("\n Mensaje enviado a ["
                                + agentID_vehiculos.get(i).getLocalName() + "]"
                                + "\n Con mensaje de respuesta: "
                                + mensaje.toString());
                    }
                    enviarMensaje(agentID_vehiculos.get(i), ACLMessage.INFORM, conversationID);
                } else {
                    System.out.println("\n El estado de ["
                            + agentID_vehiculos.get(i).getLocalName() + "]"
                            + " es: "
                            + "\n Según " + this.getAid().getLocalName()
                            + " es: \t" + estados_vehiculos.get(i)
                            + "\n Según " + agentID_vehiculos.get(i).getLocalName()
                            + " es: \t" + percepciones_vehiculos.get(i).get("estado"));
                }
            }

            /**
             * Puede que alguno se haya estrellado. Por lo que no podrá pedir
             * las percepciones. Por eso debo saber cuantos
             * vehiculos_en_movimiento hay para adecuar la cantidad de mensajes
             * que espero recibir. Al inicio de la siguiente iteración.
             */
            ArrayList<Integer> vehiculos_con_objetivo = new ArrayList<>();
            for (int i = 0; i < vehiculos_en_movimiento; i++) {
                // Esperando recibi desenlace del movimiento.
                recibirMensaje();
                System.out.println("iteración:: " + i);
                estado_actual = mensaje.get("estado").asInt();
                pos = getPosicion(agentID_vehiculos, mensajeEntrada.getSender());
                estados_vehiculos.set(pos, estado_actual);
                if (estado_actual == 0) {
                    System.out.println(mensajeEntrada.getSender().getLocalName()
                            + " SIGUE EN JUEGO");
                    vehiculos_con_objetivo.add(pos);
                    System.out.println(" Superviviente: "
                            + agentID_vehiculos.get(i).getLocalName()
                            + " posición: " + pos);
                } else {
                    System.out.println(mensajeEntrada.getSender().getLocalName()
                            + " Ha chocado. (Un activo menos)");
                }
            }
            for (int i = 0; i < vehiculos_con_objetivo.size(); i++) {
                System.out.println(" Vehiculo con objetivo:" + vehiculos_con_objetivo.get(i));
            }

            vehiculos_en_movimiento = vehiculos_con_objetivo.size();
            System.out.println("\n Estados: ");
            for (int i = 0; i < estados_vehiculos.size(); i++) {
                System.out.print(" " + estados_vehiculos.get(i));
            }
            System.out.println(" Vehiculos con objetivo: " + vehiculos_con_objetivo.size());
            System.out.println("\n Vehiculos en movimiento: " + vehiculos_en_movimiento);
            System.out.println("\n");
            /**
             * Ahora es el momento de enviar la confirmación de sus estados
             */
            for (int i = 0; i < vehiculos_con_objetivo.size(); i++) {
                mensaje = new JsonObject();
                mensaje.add("result", "OK, sigue jugando ");
                enviarMensaje(agentID_vehiculos.get(vehiculos_con_objetivo.get(i)),
                        ACLMessage.INFORM, conversationID);
            }

            /**
             * Ahora es el momento donde los vehículos envían al controlador la
             * decisión que han tomado en base a sus sensores. Por tanto el
             * burócrata solo le queda esperar percepciones y enviar
             * indicaciones según el estado de cada vehículo. Por tanto el
             * burócrata entra en un estado de escucha interminable hasta que
             * los vehiculos_activos sean 0 Esto ocurre cuando todos llegan al
             * destino, o cuando algunos llegan al destino y otros se han
             * CRASHEADO o se les ha inducido inactividad.
             */
            //System.out.println(this.toString());
            /**
             * Cierre del while
             */
        }

        /**
         * Se ha salido del while porque no hay vehículos con posibilidad de
         * moverse Pues están CRASHEADOS, sin posibilidad de moverse o en la
         * meta
         */
        cancel();
        getTraza(true);

        /**
         * Como los vehículos quedan a la espera tras llegar al destino o cuando
         * están CRASHEADOS Y como el programa no se acaba hasta que todos los
         * agentes terminan su ejecución, creo conveniente enviar un último
         * mensaje para que puedan finalizar su ejecución
         */
        for (int i = 0; i < agentID_vehiculos.size(); i++) {
            mensaje = new JsonObject();
            enviarMensaje(
                    agentID_vehiculos.get(i),
                    ACLMessage.CANCEL,
                    conversationID);
        }
    }

    /**
     * @author: Germán Función auxiliar para encontrar el AgentID del vehículo
     * en el ArrayList Motivo, Almacenar la situación actual del vehículo y
     * responderle tras el proceso de actualización del mapa, para tener más
     * información para la toma de decisiónes.
     * @param lista Lugar donde buscar al agente
     * @param agente Agente a buscar
     * @return Devuelve la posición donde se encientra el agente.
     *
     * @POS Devuelve -1, si el agente a buscar no se ha encontrado.
     */
    private int getPosicion(ArrayList<AgentID> lista, AgentID agente) {
        int posicion = -1;
        String nombre;
        for (int i = 0; i < lista.size(); i++) {
            nombre = lista.get(i).getLocalName();
            if (nombre.equals(agente.getLocalName())) {
                posicion = i;
            }
        }
        return posicion;
    }

    /**
     * @author: Germán Método que encapsula la actualización del mapa Pasos: 1º]
     * Calcula el rango de visión a partir del tamaño del sensor. 2º] Determina
     * el vehículo que es mediante su agentID 3º] Se determina el color asignado
     * al agente mediante si agentID 4º] Cada color tiene un espectro asociado.
     * (He llamado espectro a la región de mapa vislumbrada.) 5º] Se transladan
     * las coodenadas para ajustarlas al mapa png 6º] Se realiza la
     * actualización del mapa verificando que el dato del sensor es: 0, 1, 2, 3,
     * 4 en caso contrario respondería con -1 dando por fallida la
     * actualización. 7º] Se actualiza la posición donde se encuentra el
     * vehículo con su color asignado.
     * @IMPORTANTE Tras actualizar el mapa debe realizarse un archivo png para
     * mostrar la situación actual.
     */
    private int actualizarMapa(AgentID agente, JsonArray sensor,
            int coordenadaX, int coordenadaY) {
        int pixeles_descubientos = 0;

        int rango = (int) Math.sqrt(sensor.size());
        System.out.println("[" + agente.getLocalName() + "]"
                + "\n Rango de visión: " + rango);

        /**
         * Dadas las coordenadas y el rango de visión, es posible rellenar la
         * sección de mapa vislumbrada.
         */
        int pos = getPosicion(agentID_vehiculos, agente);
        int x0 = coordenada(coordenadaX, rango);
        int y0 = coordenada(coordenadaY, rango);
        int tono;
        int dato;
        int i;      // Contador en el  JsonArray
        for (int c = 0; c < rango && pixeles_descubientos != -1; c++) {
            for (int f = 0; f < rango && pixeles_descubientos != -1; f++) {
                if (mapa.getRGB(x0 + c, y0 + f) == 0xffc0c0c0) {
                    pixeles_descubientos++;
                }
                i = c + f * rango;
                dato = sensor.get(i).asInt();
                if (dato != 4 || i == rango / 2) {
                    tono = color(pos, dato);
                    if (tono != -1) {
                        mapa.setRGB(x0 + c, y0 + f, tono);
                    } else {
                        pixeles_descubientos = -1;
                    }
                }
            }
        }

        return pixeles_descubientos;
    }

    /**
     * @author: Germán Método auxiliar para situar las coordenadas del vehículo
     * correctamente en el mapa png, pues el tamaño real es 110x110 pixeles y el
     * area de acción es de 100x100 donde las coordenadas recibidas van de (0,0)
     * a (99,99) cuando en el png eso implica (5,5) a (104, 104) , es decir, un
     * desplazamiento de 5 unidades en cada coordenada. Motivo, cuando se
     * reciben la coordenadas de posición (37, 0) NO se puede rellenar
     * correctamente el png pues por debajo de 0 no se puede, pero el vehículo
     * percibe más allá ¿Por qué? Por el desplazamiento de las coordenadas.
     * Recibir (37, 0) significa que estamos situados en (42, 5); por lo que ya
     * si es posible rellenar bien el mapa.
     * @param numero
     * @param rango
     * @return
     */
    private int coordenada(int numero, int rango) {
        int coordenada_inicial;
        switch (rango) {
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
     * @author: Germán Método auxiliar que conmuta entre dos representaciones: -
     * La representación del profesor: 0 libre, 1 obstáculo, 2 muro, 3 destino,
     * 4 agente - La representación en png de los valores del profesor: 0xffffff
     * libre, 0x000000 obstáculo, 0x000000 muro , 0xff0000 destino, 0xffffff
     * agente. - Se pretende cambiar la representación libre por un color
     * similar al que posee el agente para indicar el rastro que ha seguido
     * durante su exploración.
     */
    private int color(int pos, int valor_sensor) {
        int resultado;
        switch (valor_sensor) {
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
     * @author: Germán Función auxiliar para obtener al vehículo más cercano del
     * que está pidiendo ayuda.
     * @param percepciones_vehiculos
     * @param indices_candidatos
     * @param indice_del_necesitado
     * @return Devuelve el índice del vehículo más cercano al necesitado.
     */
    private int candidatoMasCercano(
            ArrayList<JsonObject> percepciones_candidatos,
            ArrayList<Integer> indices_candidatos,
            int indice_del_necesitado) {

        int candidatoMasCercano = -1;

        int x_candidato;
        int y_candidato;
        int x_necesitado = percepciones_candidatos
                .get(indice_del_necesitado)
                .get("x").asInt();
        int y_necesitado = percepciones_candidatos
                .get(indice_del_necesitado)
                .get("y").asInt();
        int x;
        int y;
        double distancia_minima = Double.POSITIVE_INFINITY;
        double resultado_e;

        for (int i = 0; i < indices_candidatos.size(); i++) {
            x_candidato = percepciones_candidatos
                    .get(i)
                    .get("x").asInt();
            y_candidato = percepciones_candidatos
                    .get(i)
                    .get("y").asInt();

            /* Obteniendo las coordenadas del vector que une
             * al vehículo candidato y al vehículo necesitado
             */
            x = x_necesitado - x_candidato;
            y = y_necesitado - y_candidato;

            // Calculando el modulo del vector
            resultado_e = Math.sqrt(x * x + y * y);

            // Comparando distancia anterior con la actual
            // Me quedo con la distancia mínima.
            if (resultado_e < distancia_minima) {
                distancia_minima = resultado_e;
                candidatoMasCercano = i;
            }

        }
        return candidatoMasCercano;
    }

    /**
     * @author Germán Función auxiliar que proporciona las coordenadas de una
     * región cercana que tiene más píxeles sin descubrir
     * @param percepciones_de_vehiculo
     * @return Devuelve un Json con la clave 'dirigete_a':'true' y las
     * coordenadas 'x':x e 'y':y
     */
    private JsonObject buscarCoordenadas(JsonObject percepciones_de_vehiculo) {
        ArrayList<Integer> zona_inexplorada = new ArrayList<>();
        int rango = (int) Math.sqrt(percepciones_de_vehiculo
                .get("sensor").asArray().size());

        // Me situo en la primera zona 
        int x0 = percepciones_de_vehiculo.get("x").asInt() - rango;
        int y0 = percepciones_de_vehiculo.get("y").asInt() - rango;
        System.out.println(" Coordenadas de inicio: "
                + "\n x: " + x0
                + "\n y: " + y0);
        int x1;
        int y1;

        // Busco la región con mayor cantidad de pixeles inexplorados.
        int cantidad_maxima = Integer.MIN_VALUE;
        int cantidad;
        boolean resultado;
        /* Almacenarán las coordenadas de la zona 
            que más pixeles inexplorados tenga */
        int x = -1;
        int y = -1;

        /* Al rededor de un vehículo de rango r, podemos buscar regiones
            de rango rxr al rededor del vehículo.
         */
        for (int f = 0; f < 3; f++) {
            for (int c = 0; c < 3; c++) {
                if (c == 1 && f == 1) {
                    zona_inexplorada.add(Integer.MAX_VALUE);
                } /* No quiero que la mejor zona a explorar sea,
                    quedarse quieto.
                 */ else {
                    x1 = x0 + c * rango;
                    y1 = y0 + f * rango;
                    resultado = x1 >= 0 && x1 <= 99 && y1 >= 0 && y1 <= 99;

                    System.out.println("\n Coordenadas (x1,y1) --> "
                            + "(" + x1 + ", " + y1 + ")"
                            + " Resultado: " + resultado
                            + " rango " + rango);
                    if (resultado) {
                        cantidad = pixelesInexplorados(x1, y1, rango);
                        zona_inexplorada.add(cantidad);
                        if (cantidad > cantidad_maxima) {
                            cantidad_maxima = cantidad;
                            x = x1;
                            y = y1;
                        }
                    }
                }
            }
        }

        JsonObject coordenadas = new JsonObject();
        coordenadas.add("result", "OK, muevete a estas coordenadas");
        coordenadas.add("x", x);
        coordenadas.add("y", y);
        coordenadas.add("dirigete_a", true);

        return coordenadas;
    }

    /**
     * @author Germán Método auxiliar que cuenta los pixeles que aún no se han
     * descubiento al rededor de las coordenadas indicadas, con un rango de
     * visión dado.
     * @param x
     * @param y
     * @param rango
     * @return Devuelve la cantidad de pixeles sin descubrir.
     */
    private int pixelesInexplorados(int x, int y, int rango) {
        int cantidad = 0;

        // Situando correctamente las coordenadas en el png
        int x0 = coordenada(x, rango);
        int y0 = coordenada(y, rango);
        System.out.println("\n contando pixeles:");
        for (int f = 0; f < rango; f++) {
            for (int c = 0; c < rango; c++) {
                int X = x0 + c;
                int Y = y0 + f;
                System.out.print("(" + X + "," + Y + ")");
                if (mapa.getRGB(x0 + c, y0 + f) == 0xffc0c0c0) {
                    cantidad++;
                }
            }
            System.out.println("");
        }
        System.out.println(" Inexplorados: " + cantidad);
        return cantidad;
    }

    /**
     * author: Germán Método para crear una imagen png para capturar la
     * situación actual del mapa
     */
    private boolean capturaSituacion() throws IOException {
        String nombre_del_fichero = nombreFichero();
        File archivo = new File(nombre_del_fichero);
        boolean resultado = ImageIO.write(mapa, "png", archivo);
        return resultado;
    }

    /**
     * @author: Germán Método que encapsula la rutina de cancelar la suscripción
     * al mapa.
     * @return
     */
    private boolean cancel() {
        // Creando mensaje vacio
        mensaje = new JsonObject();

        // Enviando petición de cancelación de suscripción
        enviarMensaje(id_controlador, ACLMessage.CANCEL, conversationID);
        recibirMensaje();

        // Comprobando respuesta del Controlador
        boolean resultado = mensajeEntrada.getPerformativeInt() == ACLMessage.AGREE;

        // Mostrando los resultados del controlador
        int performativa = mensajeEntrada.getPerformativeInt();
        switch (performativa) {
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
     *
     * @author: Germán Método que encapsula la rutina de suscribirse al mundo
     * @param nombreMapa: Indica el mapa al que se quiere subscribirse
     */
    private void subscribe(String nombreMapa) {
        mensaje = new JsonObject();
        mensaje.add("world", nombreMapa);

        enviarMensaje(id_controlador, ACLMessage.SUBSCRIBE, conversationID);

        recibirMensaje();
        conversationID = mensajeEntrada.getConversationId();
    }

    /**
     * @author: Germán Método que gestiona la traza de sesiones anteriores
     * usandola como lienzo de para la sesión actual.
     * @IMPORANTE: Este lienzo puede contener información de irrelevante, por
     * ello debe limpiarse.
     */
    private void obtenerMapa() {
        try {
            if (!cancel()) {
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
     * @author: Germán Método auxiliar que limpia el mapa que hay en memoria.
     * @PRE: Debe haber un elemento Buffererd
     */
    private void limpiarMapa() {
        int gris = 0xffc0c0c0;
        int muro = 0xff000000;
        for (int c = 0; c < mapa.getWidth(); c++) {
            for (int f = 0; f < mapa.getHeight(); f++) {
                if (mapa.getRGB(c, f) != gris) {
                    mapa.setRGB(c, f, gris);
                }

// Dibuja los bordes                
//                if(f<5 || f>104) mapa.setRGB(c, f, muro);
//                if(c<5 || c>104) mapa.setRGB(c, f, muro);
            }
        }
    }

    /**
     * @author: Germán
     * @PRE: La traza debe haberse recibido. El método getTraza crea una imagen
     * con la traza obtenida
     * @param exito: Indica el modo de tratamiento de la traza. TRUE -->
     * Queremos la traza FALSE--> No queremos la traza pero debe gestionarse
     */
    private void getTraza(boolean exito) {
        try {
            String nombreFichero = nombreFichero();

            if (exito) {
                /* Recibimos la respuesta del servidor */
                recibirMensaje();
                rutaFichero = nombreFichero;
            } else {
                recibirMensaje();
                rutaFichero = nombreFichero
                        + "Error_traza_anterior_";
            }

//            System.out.println("Mensaje recibido, del servidor, tras el logout: " + mensaje.toString());
            /* Cuando la respuesta es OK, guardamos la traza */
            if (mensaje.toString().contains("trace")) {
                System.out.println("Recibiendo traza");
                JsonArray ja = mensaje.get("trace").asArray();
                byte data[] = new byte[ja.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) ja.get(i).asInt();
                }

                FileOutputStream fos = new FileOutputStream(rutaFichero);

                fos.write(data);
                fos.close();

                System.out.println("Traza Guardada en " + nombreFichero);
            }

        } catch (IOException ex) {
            System.err.println("Error al recibir la respuesta o al crear la salida con la traza");
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    ;
    
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
    private String nombreFichero() {
        // Creando carpeta contenedora de los pngs
        String nombre_carpeta = ".."+separador+"mapa_";
        File carpeta_pngs = new File(nombre_carpeta);
        if (carpeta_pngs.mkdir()) {
            System.out.println("Directorio " + "mapa_ " + "Creado ");
        } else {
            System.out.println("Directorio ya existente ");
        }

        String ruta = nombre_carpeta + separador;
        String fecha = fecha();
        String cadena = ruta
                + iteracion_actual + "__"
                + nombreMapa + "__"
                + equipo + "_"
                + fecha + "_"
                + mensajeEntrada.getConversationId();
        return cadena;
    }

    /**
     * @author: Germán Método que muestra el contenido del Mapa, en dististos
     * formatos. con enteros, hexadecimal o con caracteres
     * @param nombre_archivo: Ubicación del archivo a decodificar y mostrar
     * @param tipo : Formato de presentación ("int", "hex", "string" )
     * @return : Devuelve BufferedImage
     * @throws IOException : Excepción que en esta versión no se controla.
     */
    private BufferedImage getMapa(String tipo) throws IOException {
        if (rutaFichero != "") {
            File imageFile = new File(rutaFichero);
            BufferedImage image = ImageIO.read(imageFile);
            System.out.println("Características de la imagen. "
                    + "\n Tipo:    " + image.getType()
                    + "\n Altura:  " + image.getHeight()
                    + "\n Anchura: " + image.getWidth());

            int hex;
            for (int c = 0; c < image.getWidth(); c++) {
                for (int f = 0; f < image.getHeight(); f++) {
                    hex = image.getRGB(f, c);
                    System.out.print(conversor(hex, tipo) + " ");
                }
                System.out.println("");
            }
            return image;
        } else {
            System.out.println("\n NO HAY RUTA !!! ");
            return null;
        }

    }

    /**
     * @author: Germán Método auxiliar dar formato a un dato entero.
     * @param hex: Valor del entero
     * @param tipo: Tipo de representación elegida
     * @return
     */
    private static String conversor(int hex, String tipo) {
        String dato;

        if (tipo == "int") {
            dato = Integer.toString(hex);
        } else if (tipo == "hex") {
            dato = Integer.toHexString(hex);
        } else {
            switch (hex) {
                case -4144960:
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
     * @author: Germán Método que encapsula la tarea de obtener la fecha y hora
     * actual (solo usado para crear el nombre del archivo png que contendrá la
     * traza)
     * @return: Devuelve la fecha y la hora en un String
     * @POST: Los caracteres '/' en el sistema Ubuntu 16.04 los interpreta como
     * ruta, por ello se ha usado '-' como separador de fecha
     */
    private String fecha() {
        Calendar c = Calendar.getInstance();
        int mes = c.get(Calendar.MONTH) + 1;
        String fecha = +c.get(Calendar.DAY_OF_MONTH) + "-"
                + mes + "-"
                + c.get(Calendar.YEAR) + "_"
                + c.get(Calendar.HOUR_OF_DAY) + ":"
                + c.get(Calendar.MINUTE) + ":"
                + c.get(Calendar.SECOND);
        return fecha;
    }

    /**
     * @author: Germán Métod para mostrar el mapa que se tiene actualmente.
     * @param tipo
     */
    private void ToStringMapa(String tipo) {
        int hex;
        for (int c = 0; c < mapa.getWidth(); c++) {
            for (int f = 0; f < mapa.getHeight(); f++) {
                hex = mapa.getRGB(f, c);

                System.out.print(conversor(hex, tipo) + " ");
            }
            System.out.println("");
        }
    }

    ;
    
    
    private void getTrazaAnterior() {
    }

    private void dirigeteA(Casilla c) {
    }

    /**
     * **********************************************************************
     */
    /*
        Tareas de Alex
     */
    /**
     * **********************************************************************
     */
    private void pathFinder() {

    }

    private void quadTree() {

    }

}
