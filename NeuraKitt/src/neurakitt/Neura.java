package neurakitt;

import es.upv.dsic.gti_ia.core.AgentID;
import java.util.ArrayList;

/**
 * @author Alvaro
 * 
 * @author Alejandro
 * @FechaModificación: 01/11/2018
 * @Motivo: java permite el carácter Ñ sin problema
 */
public class Neura extends Agente {
    /**
     * Atributos iniciales
     * 
     * @auhor Alvaro
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @Motivo variables static. Cambio los atributos a privados ya que son
     * propios del agente. Realizar el acceso mediante métodos get, si procede.
     * 
     * He creado un ENUM con las posibles acciones del agente en lugar de 
     * almacenarlas en un vector.
     * 
     */
    
    // Entorno que percibe, cuadrado de 5x5
    private static int TAM_ENTORNO = 25;
    // Destinados a moverse en el mapa.    
    private float scanner[]     = new float[TAM_ENTORNO];     // Tamaño fijo
    
    /**************************************************************************/
    private int tamañoScanner   = TAM_ENTORNO;        // ¿PARA QUÉ?
    /**************************************************************************/
    
    private int radar[]         = new int[TAM_ENTORNO];       // Tamaño fijo
    
    /**************************************************************************/
    private int tamañoRadar     = TAM_ENTORNO;        // ¿PARA QUÉ?
    /**************************************************************************/
    
    // Destinados a memoria    
    private int gps[]           = new int[3];                   // [x, y, nº veces]
    
    /**************************************************************************/
    private ArrayList caminado  = new ArrayList();      // ¿TIPO?
    /**************************************************************************/

    // Movimientos posibles.    
    /**************************************************************************/
    // private Accion miAccion;                 A USAR MÁS TARDE
    /**************************************************************************/
    private String movimientos[]= {
        "moveNW","moveN" ,"moveNE",
        "moveW" ,"logout","moveE" ,
        "moveSW","moveS" ,"moveSE"
    };
    
    
    /**
     * Contiene la información del scanner y del radar,
     * reducida a los 8 movimientos.
     */
    private static int TAM_RADANNER = 9;
    private float radanner[] = new float[TAM_RADANNER];
    // Identificador del agente KITT
    private AgentID idKITT;
    
    
    /**
     * @author Alvaro, Germán
     * @param aID       identificador de Neura
     * @param idKITT    identificador de receptor de los mensajes de Neura
     * @throws Exception 
     */
    public Neura(AgentID aID, AgentID idKITT) throws Exception {
        super(aID);
        
       for(int i=0; i< TAM_ENTORNO; i++){
        //   scanner[i] = (float) Math.random()*70;
           scanner[i] = 0;
        //   radar[i] = (int) Math.floor(Math.random()*3);
           radar[i] = -1;
        }
        gps[0]=-1;
        gps[1]=-1;
        gps[2]= 0;
       
        this.idKITT = idKITT;
    }
    
    @Override
    /**
     * Comportamiento del agente NUERA
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
        
        // while (miAccion != Accion.logout)
        while (true){
            recibirMensaje();
            // procesarMensaje();       GERMÁN
            procesarInformacion();
            decidirAccion();
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
     * Transforma la informacion del scanner para mejor gestión de ésta.
     * 
     * @author  Juan Germán Gómez Gómez.
     * @note    La representación: 0 libre, 1 obstáculo, 2 destino
     * no la considero adecuada por la reflexión anterior.
     * Por ello realizaré el cambio: 1 libre, 0 obstáculo, -1 destino.
     * 
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @Motivo Nombre más significativo. Cambio de ProcesarRadarYEscanner() a
     * procesarInformacion()
     * 
     * 
     */
    private void procesarInformacion(){
        int traslacion;
        for(int i=0; i<tamañoScanner; i++){
//           scanner[i] = (1 - radar[i])*scanner[i];
            traslacion = 1 - radar[i];    // Transformación, el radar no se altera.
            if(traslacion < 0){          //Es celda destino
               scanner[i]= -1;
            }
            else
               scanner[i]=traslacion*scanner[i];
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
    * @Motivo Nombre más significativo. Cambio Radanner() por actualizarRadanner()
    *   Cambiada accesibilidad a privada.
    */
    private void actualizarRadanner(){
       radanner[0] = scanner[6];
       radanner[1] = scanner[7];
       radanner[2] = scanner[8];
       radanner[3] = scanner[11];
       radanner[4] = scanner[12];
       radanner[5] = scanner[13];
       radanner[6] = scanner[16];
       radanner[7] = scanner[17];
       radanner[8] = scanner[18];
    }
   
   
    /**
     * Neura decide el elemento más prometedor de las proximidades de Kitt.
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
     * @Motivo Las funciones y métodos deben ser siempre verbos ya que tal y como
     * está ahora no sé lo que hace visto desde otra clase a no ser que mire el código.
     * Con "Decision" no sé si devuelve la decision, la toma, si es una variabe o qué.
     * 
     * - Cambio el nombre del método a decidirAccion().
     * - Cambio visibilidad a privada.
     */
    private int decidirAccion(){
       float minimo;
       minimo = Float.POSITIVE_INFINITY;
       int posicion = -1;
       
    /**
     *  Miro primeramente la posición 4 que es donde está Kitt,
     * para averiguar si estoy en destino.
     * En otro caso recorro el radanner.
     */   
        if(radanner[4]<0)        
           return 4;            // Kitt está en el destino
        else{                    // Kitt no está en destino
            for(int i=0; i< TAM_RADANNER; i++){
                if(radanner[i]<0)
                   return i;    
               
                else if(radanner[i]<minimo && radanner[i] != 0.0){
                   minimo = radanner[i];
                   posicion = i;
                }
            }
       }
       return posicion;
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
     * @Motivo Las funciones y métodos deben ser siempre verbos ya que tal y como
     * está ahora no sé lo que hace visto desde otra clase a no ser que mire el código.
     * Con "Acción" no sé si devuelve la acción, la toma, si es una variabe o qué.
     * Cambio el nombre del método a getAccion()
     */
    public String getAccion(){
       return movimientos[decidirAccion()];
    }
    
    
    /**
     * Muestra el contenido de cada sensor
     * 
     * @author  Germán
     * @note    Útil para apreciar los cambios que se realizan
     * 
     * @author Alejandro
     * @FechaModificación 01/11/2018
     * @Motivo  Cambio PrintSensores() por getSensores()
     */
    public void getSensores(){
        System.out.print("\n Radar: ");
        for(int i=0; i<TAM_ENTORNO; i++){
           System.out.print(radar[i]+" ");
        }
       
        System.out.print("\n Scanner: ");
        for(int i=0; i<TAM_ENTORNO; i++){
           System.out.print(scanner[i]+" ");
        }
       
        System.out.print("\n GPS: x="+ gps[0] + ", y="+ gps[1]);
       
        System.out.println("\n Radanner: ");
        for(int i=0; i<9; i++){
           System.out.println("i= "+i+" "+radanner[i]+" ");
        }
    }
}
