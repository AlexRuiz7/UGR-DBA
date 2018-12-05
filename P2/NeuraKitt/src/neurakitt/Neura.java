package neurakitt;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.ArrayList;

/**
 * @author Alvaro
 * 
 * @author Alejandro
 * @FechaModificación: 01/11/2018
 * @custom.Motivo: java permite el carácter Ñ sin problema
 */
public class Neura extends Agente {
    /**
     * Atributos iniciales
     * 
     * @auhor Alvaro
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @custom.Motivo variables final. Cambio los atributos a privados ya que son
     * propios del agente. Realizar el acceso mediante métodos get, si procede.
     * 
     * He creado un ENUM con las posibles acciones del agente en lugar de 
     * almacenarlas en un vector.
     * 
     * @FechaModificación 03/11/2018
     * @custom.Motivo inclusión de la nueva clase Casilla para la gestión de la memoria
     * del agente, que sustituye a la matriz GPS[ int [3] ].
     */
    
    // Alcance de los sensores. 5x5
    private final int TAM_ENTORNO  = 25;
    private final int NUM_SENSORES = 3;
    private final int TAM_RADANNER = 9;
    // Destinados a reconocer el mapa.    
    private ArrayList<Float>    scanner;
    private ArrayList<Integer>  radar;
    private ArrayList<Float>    radanner;
    // Destinados a memoria    
    private ArrayList<Casilla>  memoria;
    private Casilla             pos_actual;
    // Destinados al movimiento  
    private ArrayList<String>   movimientos; 
    private String              accion;
    // Destinados a la toma de decisión
    private ArrayList<Casilla>  alrededores;
    // Identificador del agente KITT
    private final AgentID       idKITT;
    // Destinado a saber qué
    private final boolean anilloExterior;
    
    /**
     * CONSTRUCTOR
     * 
     * @author  Alvaro, Germán, Alejandro
     * @param   aID         identificador de Neura
     * @param   idKITT      identificador de receptor de los mensajes de Neura
     * @param   ext         Germán sabrá
     * @param   tope        Germán sabrá
     * @param   antiguedad  Germán sabrá
     * @throws  Exception 
     * 
     * 
     * @author  Alejandro
     * @FechaModificación 31/11/2018
     * @custom.Motivo  inclusión de la clase Casilla. Movidas TODAS las inicializaciones
     *  al constructor, en lugar de en la definición de los miembros.
     */
    public Neura(AgentID aID, AgentID idKITT, boolean ext, int tope, int antiguedad) 
            throws Exception {
        super(aID);
        
        scanner         = new ArrayList(TAM_ENTORNO);
        radar           = new ArrayList(TAM_ENTORNO);
        radanner        = new ArrayList(TAM_RADANNER);
        memoria         = new ArrayList();
        movimientos     = new ArrayList(TAM_RADANNER);
        accion          = "";
        this.idKITT     = idKITT;
        pos_actual      = new Casilla(0,0);
        anilloExterior  = ext;
        iteracionesTope = tope;
        this.antiguedad = antiguedad;
        
        movimientos.add("moveNW");
        movimientos.add("moveN");
        movimientos.add("moveNE");
        movimientos.add("moveW");
        movimientos.add("logout");
        movimientos.add("moveE");
        movimientos.add("moveSW");
        movimientos.add("moveS");
        movimientos.add("moveSE");
        
        alrededores = new ArrayList(TAM_RADANNER);
        alrededores.add(new Casilla(-1,-1));                // NW
        alrededores.add(new Casilla( 0,-1));                // W
        alrededores.add(new Casilla( 1,-1));                // NE
        alrededores.add(new Casilla(-1, 0));                // W
        alrededores.add(new Casilla( 0, 0));                // KITT
        alrededores.add(new Casilla( 1, 0));                // W
        alrededores.add(new Casilla(-1, 1));                // SW
        alrededores.add(new Casilla( 0, 1));                // S
        alrededores.add(new Casilla( 1, 1));                // SE
        
        for(int i=0; i<TAM_RADANNER; i++)
            radanner.add(Float.POSITIVE_INFINITY);

        for(int i=0; i<TAM_ENTORNO; i++){
            scanner.add(Float.MAX_VALUE);
            radar.add(Integer.MAX_VALUE);
        }
    }
    
    
    @Override
    /**
     * Comportamiento del agente NEURA
     * 
     * MIENTRAS <condición de parada>
     *      recibirMensajes(servidor)
     *      procesarMensaje(extraer GPS, RADAR, SCANNER del JSON)
     *      procesarInformación()
     *      decidirAcción()
     *      enviarAcción(a KITT)
     * 
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * 
     * @HU 5.2 y 5.3
     */
    public void execute(){   
        while (accion != "logout"){
            actualizarSensores();           // Recibe los mensajes y los procesa
            procesarInformacion();          
            getSensores();                  /* Comprobar estado de los sensores */
            
            iteracionActual++;
            if(iteracionActual == iteracionesTope)
                accion = "logout";
            else
                accion = getAccion();

            mensaje = new JsonObject();
            mensaje.add("accion", accion);
            if(DEBUG)
                System.out.println("[NEURA] Accion a realizar: " + accion);
            enviarMensaje(this.idKITT);
        }
    }
    
    
    /** Funciones Auxiliares ****************************************/ 
    
    /**
     * @author: Germán
     * Reflexión.
     * Previos:
     *  El radar representa la información del entorno con 0,1,2 (libre, obstáculo, 
     *  destino)
     *  El scanner representa la información mediante un número real que simboliza
     *  la distancia al destino.
     * 
     *  Si quiero extraer la información de ambos, debo mirar a ambos, ello me 
     *  implica trabajar con ambos vectores simultaneamente, lo que es un foco 
     *  de posibles errores.
     *  
     *  Por ello he considerado unificar la información en un solo vector,
     *  multiplicando ambos vectores componente a componente.
     *  Tal como están ahora mismo el producto resultante es un vector que muestra
     *  los obstáculos con información irrelevante, además de perderse la ubicación
     *  del destino.
     * 
     *  Por tanto, considero cambiar los valores:
     *      0, libre,       a  1
     *      1, obstáculo,   a  0
     *      2, destino,     a -1
     * 
     *  Multiplicando ambos vectores, el resultado sería información de la 
     *  distancia al destino y de simbolizar el destino con un valor negativo.
     *  Que a afectos de buscar la celda de menor valor ésta sería negativa.
     * 
     *  Considerada la tranformación del radar, ahora toca procesar la del scanner,
     *  pues la distancia al destino cuando se está sobre él es 0.
     *  y con este cambio no sería perceptible. Por este motivo debería 
     *  revisar todo el scanner hasta encontrar 0 e intercambiarlo por un valor 
     *  positivo.
     * 
     *  Ejemplo: 1, motivo: radar*scanner = (-1)*(1) es negativo.
     */
   
    /**
     * Transforma la informacion interna scanner para mejorar la toma de decisiones.
     * 
     * @author  Juan Germán Gómez Gómez.
     * @note    La representación: 0 libre, 1 obstáculo, 2 destino
     * no la considero adecuada por la reflexión anterior.
     * Por ello realizaré el cambio: 1 libre, 0 obstáculo, -1 destino.
     * 
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @custom.Motivo Nombre más significativo. Cambio de ProcesarRadarYEscanner() a
     * procesarInformacion()
     */
    private void procesarInformacion(){
        int traslacion;
        
        for(int i=0; i<scanner.size(); i++){
            traslacion = 1 - radar.get(i);
            
            if(traslacion < 0)                  // Es celda destino
                scanner.set(i, -1f);
            else
                scanner.set(i, traslacion*scanner.get(i));
        }
        
        actualizarRadanner();
    }
   
    
    /**
    * Asigna a cada posición el contenido procesado del Radar y del Scanner.
    * 
    * @author  Germán
    * @note Reflexión
    *   Los movimientos son: N, S, E, W, NE, NW, SE, SW
    *   Que corresponden diretamente con las celdas adyacentes a Kitt:
    *   6 --> NW        7 --> N         8 --> NE
    *   11--> W         12--> Kitt      13--> E
    *   16--> SW        17--> S         18--> SE
    * 
    * 
    * @author Alejandro
    * @FechaModificación 01/11/2018
    * @custom.Motivo Nombre más significativo. Cambio Radanner() por actualizarRadanner()
    *  Cambiada accesibilidad a privada.
    */
    private void actualizarRadanner() {
        radanner.set(0, scanner.get(6));
        radanner.set(1, scanner.get(7));
        radanner.set(2, scanner.get(8));
        radanner.set(3, scanner.get(11));
        radanner.set(4, scanner.get(12));
        radanner.set(5, scanner.get(13));
        radanner.set(6, scanner.get(16));
        radanner.set(7, scanner.get(17));
        radanner.set(8, scanner.get(18));
        
        procesarEntorno();
    }
   
    
    /**
     * Neura decide el elemento más prometedor de las proximidades de KITT.
     * 
     * En primer lugar, se comprueba la posición 4 por si KITT se encuentra ya
     * en el destino. En caso de que no lo esté, continúa la búsqueda.
     * 
     * 
     * @author:  Germán  
     * @return Devuelve la posición del elemento más prometedor.
     * @version: 1.1
     * @note:    Creo que es más importante la posición donde se encuentra
     *  el elemento más prometedor que la información que contiene.
     * 
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @custom.Motivo Las funciones y métodos deben ser siempre verbos ya que tal y como
     * está ahora no sé lo que hace visto desde otra clase a no ser que mire el código.
     * Con "Decision" no sé si devuelve la decision, la toma, si es una variabe o qué.
     * 
     * - Cambio el nombre del método a decidirAccion().
     * - Cambio visibilidad a privada.
     * 
     * @custom.FechaModificacion 03/11/2018
     * - Cambiado el algoritmo de búsqueda por uno más simple y que no rompa 
     *   el bucle con varios returns.
     * 
     *  El algoritmo busca en radanner por un valor menor que cero, el destino.
     *  En caso de no encontrarlo, obtiene la casilla más ventajosa para
     *  alcanzarlo, es decir, aquella que tenga el menor valor de entre todas.
     * 
     *  Mantengo la comprobación original de si KITT ya ha llegado al destino.
     */
    private int decidirAccion(){
        float minimo = Float.POSITIVE_INFINITY;
        int posicion = -1;
       
       
        if(radanner.get(4) < 0)
            posicion = 4;
        else 
            for(int i=0; i<radanner.size() && radanner.get(i)<0; i++) {
                if(radanner.get(i)<0)
                    posicion = i;
                else
                    if(radanner.get(i)<minimo && radanner.get(i)!=0.0 && i!=4) {
                        minimo = radanner.get(i);
                        posicion = i;
                    }
            }
        
        return posicion;
    }
   
    
    /**
     * Recibe los mensajes que envía el servidor a los sensores del agente y 
     * procesa la información, actualizando su estado interno con los datos
     * proporcionados por el servidor.
     * 
     * @author Germán, Alejandro
     * 
     * @custom.FechaModificacion 03/11/2018
     * 
     */
    private void actualizarSensores(){
        JsonArray datos = new JsonArray();
               
        for(int j=0; j<NUM_SENSORES; j++){
            recibirMensaje();
            
            if(mensaje.toString().contains("scanner")) {
                datos = mensaje.get("scanner").asArray();
                for(int i=0; i<datos.size(); i++)
                    scanner.set(i, datos.get(i).asFloat());
            }
            else if(mensaje.toString().contains("radar")) {
                datos = mensaje.get("radar").asArray();
                for(int i=0; i<datos.size(); i++)
                    radar.set(i, datos.get(i).asInt());
            }
            else if(mensaje.toString().contains("gps")) {
                int x = mensaje.get("gps").asObject().get("x").asInt(); 
                int y = mensaje.get("gps").asObject().get("y").asInt();

                pos_actual = comprobarCasillaExiste(x,y);
                pos_actual.aumentarContador();
                memoria.add(pos_actual);
            }
            else {
                System.out.println("[ERROR] " + mensaje.asString());
            }
        }
    }
   
    
    /**
     * Este método comprueba si ya se ha pasado por la posición X,Y, buscando en
     * la memoria del agente. En caso de encontrar que las coordenadas dadas ya
     * han sido registradas, delvuelve la casilla en cuestión para que se pueda
     * trabajar con ella. Sin embargo, si la casilla no se encuentra, significa
     * que es la primera vez que se pasa por esa posición, luego se guarda en la
     * memoria creando y devolviendo una nueva casilla para que se pueda trabajar
     * con ella.
     * 
     * @author Alejandro
     * 
     * @return el objeto de tipo casilla que corresponde con la posición X,Y 
     * dada o, en caso de no encontrarse, una casilla recién creada.
     */
    private Casilla comprobarCasillaExiste(int x, int y) {
        
        for(Casilla i : memoria) {
            if(i.X==x && i.Y==y)
                return i;
        }
        
        return new Casilla(x,y);
    }
    
     
    
    /**
     * @author: German
     * AnalizadorEntorno determina las casillas por donde ha estado el coche
     * haciendo uso de la memoria, con ello pondera la percepcion del radaner
     * para hacer dicha opcion menos prometedora.
     * 
     * @author Alejandro
     * @custom.FechaModificacion 08/11/2018
     * @custom.Motivo
     *      - Cambiado nombre por uno más significativo
     *          (ponderadorDelEntorno --> procesarEntorno)
     *      - Eliminado método redundante para acceder al contador de una casilla.
     *          (el método comprobarCasillaExiste ya implementa esa funcionalidad)
     *      - Modificado el código del método actual para ajustarlo al cambio del
     *        punto anterior.
     */
    private void procesarEntorno(){
        
        int multiplicador = -1;
        
        for(int i=0; i<radanner.size(); i++){
           if(radanner.get(i) > 0) {
                multiplicador =  comprobarCasillaExiste(
                    pos_actual.X + alrededores.get(i).X,
                    pos_actual.Y + alrededores.get(i).Y
                ).getMemoria(iteracionActual, antiguedad);
               
                if(multiplicador > 1)
                    radanner.set(i, radanner.get(i)*multiplicador);
               
                if(anilloExterior)
                    procesarEntornoLejano(i, multiplicador);
           }
        }
    }
    
    /**
     * 
     * @Reflexión:
     * @Pregunta: ¿En qué podría servir saber la información de las celdas 
     * que no son adyacentes al agente?
     * 
     * @Respuesta: Podrián servir para evitar aproximarnos a una pared
     * pues necesariamente habrá que cambiar el sentido en el proximo movimiento
     * 
     * @Pregunta: ¿Sería mejor cambiar el sentido antes de acercarnos a la pared
     * Si sabemos que más allá de esta no podemos pasar?
     * 
     * @Respuesta: Si, 
     * se podía incluir una ponderacíon para ver esa decisión menos atractiva.
     * -Def.- 
     *      Defino muro sin huecos a la consecución de al menos tres obstáculos.
     *      Defino muro con huecos a la consecución de dos obstáculos.
     * 
     *  Ejemplo con radar:
     *      1   1   1   0   0
     *      1   0   0   0   1
     *      0   1   0   0   1
     *      0   0   0   0   1
     *      0   0   0   1   0
     * 
     *  Se puede ver que moveNW no parece buena acción  (Muro sin hueco 0,1,5)
     *  Se puede ver que moveE  no parece buena acción  (Muro sin hueco 9,14,19)
     *  Se puede ver que moveS  si parece buena acción  (No hay muro)
     *  Se puede ver que moveSE si parece buena acción  (Hay muro con hueco)
     *  Se puede ver que moveN  si parece buena acción  (Hay muro con hueco)
     *  Se puede ver que moveNE si parece buena acción  (No hay muro)
     *  Se puede ver que moveSW si parece buena acción  (No hay muro)
     */
     
    /**
     * Pondera una posición concreta del radanner en función de si hay muro 
     * frente a dicha posición.
     * 
     * @author; Germán
     * @param posicionIesimaDelRadanner
     * 
     */
    private void procesarEntornoLejano(int pos_radanner, int contador){
        int i = pos_radanner;
        int multiplicador = contador;
        
        if(hayMuro(i))
            radanner.set(i, multiplicador*radanner.get(i));
     }
    
    /**
     * @author: Germán
     * Muro mira si hay o no obtáculo en frente de la casilla adyacente al agente
     * @param posicionIesimaDelRadanner
     * @return Devuelve si hay o no muro enfrente de la casilla adyacente al agente 
     * 
     * @Nota:
     *  Hay muro al NOeste si las casillas  0,  1,  5 del radar tienen un 1.
     *  Hay muro al Norte  si las casillas  1,  2,  3 del radar tienen un 1.
     *  Hay muro al NEste  si las casillas  3,  4,  9 del radar tienen un 1.
     *  Hay muro al Este   si las casillas  9, 14, 19 del radar tienen un 1.
     *  Hay muro al SEste  si las casillas 19, 23, 24 del radar tienen un 1.
     *  Hay muro al Sur    si las casillas 21, 22, 23 del radar tienen un 1.
     *  Hay muro al SOeste si las casillas 15, 20, 21 del radar tienen un 1.
     *  Hay muro al Oeste  si las casillas  5, 10, 15 del radar tienen un 1.
     * 
     */
    private boolean hayMuro(int posicion){
        boolean muro = false;
        
        switch(posicion){
            case 0:
                muro=radar.get(0)==1 && radar.get(1)==1 && radar.get(5)==1;
                break;
            case 1:
                muro=radar.get(1)==1 && radar.get(2)==1 && radar.get(3)==1;
                break;
            case 2: 
                muro=radar.get(3)==1 && radar.get(4)==1 && radar.get(8)==1;
                break;
            case 3:  
                muro=radar.get(5)==1 && radar.get(10)==1 && radar.get(15)==1;
                break;
            case 4:   
                
                break;
            case 5:
                muro=radar.get(9)==1 && radar.get(14)==1 && radar.get(19)==1;
                break;
            case 6:
                muro=radar.get(15)==1 && radar.get(20)==1 && radar.get(21)==1;
                break;
            case 7:   
                muro=radar.get(21)==1 && radar.get(22)==1 && radar.get(23)==1;
                break;
            case 8:   
                muro=radar.get(19)==1 && radar.get(23)==1 && radar.get(24)==1;
                break;
            default:
                System.out.println("["+posicion+"] No es una posición válida ");
                muro = false;
                break;
        }
        return muro;
    }
    
    
    /**
     * Neura indica la acción más prometedora. 
     * 
     * @author Juan Germán Gómez Gómez
     * @return Devuelve el movimiento a realizar 
     * @note   De momento Neura no tiene memoria, de tenerla debería ordenar 
     * los movimientos más prometedores para poder cotejarlos con los realizados.
     * 
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @custom.Motivo Las funciones y métodos deben ser siempre verbos ya que tal y como
     * está ahora no sé lo que hace visto desde otra clase a no ser que mire el código.
     * Con "Acción" no sé si devuelve la acción, la toma, si es una variabe o qué.
     * Cambio el nombre del método a getAccion()
     * 
     * @see decidirAccion()
     */
    public String getAccion(){
        return movimientos.get(decidirAccion());
    }
    
    
    /**
     * Muestra el contenido de cada sensor
     * 
     * @author  Germán
     * @note    Útil para apreciar los cambios que se realizan
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @custom.Motivo  Cambio PrintSensores() por getSensores()
     * 
     * @custom.FechaModificacion 03/11/2018
     * @custom.Motivo reemplazo de bucles for por funciones propias del lenguaje
     */
    public void getSensores(){
        System.out.println("[NEURA] Radar: "            + radar.toString());
        System.out.println("[NEURA] Scanner: "          + scanner.toString());
        System.out.println("[NEURA] Radanner: "         + radanner.toString());
        System.out.println("[NEURA] Posición (GPS): "   + memoria.toString());
    }
}
