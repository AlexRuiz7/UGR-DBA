/******************************************************************************
 *  Clase:          PathFinder.java
 *  Autor:          Alejandro Ruiz Becerra
 *  Dependencias:   Nodo.java, Casilla.java
 *
 *  PathFinder
 * 
 * https://medium.com/@nicholas.w.swift/easy-a-star-pathfinding-7e6689c7f7b2
 * 
 ******************************************************************************/
package transformers.utils;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Stack;

/**
 * Esta clase calcula el [mejor] camino (si lo hay) desde un punto origen y 
 * un punto destino, dentro de un mapa bidimesional representado por sus
 * coordenadas X e Y.
 * 
 * Para hacer uso de esta clase, se debe crear uns instancia de la misma y
 * luego, cada vez que se necesite un plan (camino y sus movimientos), llamar
 * al metodo creado para tal efecto indicando los parametros necesarios.
 * 
 * En concreto, la rutina correcta es:
 *  - Crear una instacia de PathFinder
 *  - Inicializar sus parametros:
 *      - setOrigen(), setDestino(), setMapa()
 *  - Calcular el camino.
 *
 * 
 * @author alex
 */
public class PathFinder {
    private PriorityQueue<Nodo> abiertos;
    private PriorityQueue<Nodo> cerrados; 
    private Stack<Nodo>         camino;
    private ArrayList<Nodo>     mapa;
    private Nodo destino, origen;
    
   
    /**
     * Constructor
     */
    public PathFinder() {
        this.destino = new Nodo();
        this.origen  = new Nodo();
        this.mapa    = new ArrayList();
        
        abiertos = new PriorityQueue();
        cerrados = new PriorityQueue();
        camino   = new Stack();
    }
    

    /**
     * Para la busqueda del camino
     * 
     * 1. Inicializar abiertos y cerrados (vacias)
     * 2. Origen --> abiertos
     * 3. Mientras ( abiertos.noVacio() )
     *  3.1 nodo_actual = abiertos.pop()
     *  3.2 expandir nodos adyacentes a nodo actual (solo transitables)
     *  3.2bis (solo transitables, no_en_cerrados y no_en_abiertos). Evita la
     *      re-exploracion de nodos (ciclos)
     *      3.2.1 para los nodos expandidos (nodo.padre = nodo_actual)
     *      3.2.2 para los nodos expandidos --> calcular f
     *  3.3 añadir los nodos expandidos a abiertos
     *  3.4 cerrados.add(nodo_actual)
     *  3.5 nodo_actual = min(nodos expandidos)
     *  3.5bis Esto no es necesario, ya que se ha usado una cola con prioridad (ordenada)
     *  3.6 if (nodo_actual == objetivo)
     *          encontrado = true
     * 
     * Si sale del bucle (abiertos esta vacio) y encontrado == false:
     *      no hay camino
     * 
     * Para la reconstruccion del camino:
     *  1. Desde Nodo_Objetivo hasta Nodo_Objetivo.padre != null
     *      1.1. Pila.add(nodo.padre)
     *      1.2. nodo = nodo.padre
     */
    private void buscar() {
        boolean encontrado = false;
        Nodo nodo_actual = new Nodo(), hijo;
        int tam, pos_actual, pos;
        
        abiertos.clear();
        cerrados.clear();
        
        origen.f(origen,destino);
        abiertos.add(origen);
        
        int i;
        for(i=0; (!encontrado && !abiertos.isEmpty()); i++) {
            nodo_actual = abiertos.poll();
            
            tam = (int) Math.sqrt(mapa.size());
            pos_actual = nodo_actual.X * tam + nodo_actual.Y;
            
            cerrados.add(nodo_actual);
            
            // Expandir nodos adyacentes (8)
                // Si el nodo es no transitable, ignorar
                // Para los nodos transitables, calcular f
                // nodo_actual = min(nodos transitables)
            for(int x=-1; x<=1; x++) {
                for (int y=-1; y<=1; y++) {
                    pos = pos_actual + x*tam + y;
                    hijo = mapa.get(pos);
                    if(hijo.esTransitable() && !contiene(cerrados, hijo) && !contiene(abiertos, hijo)){
                        hijo.padre = nodo_actual;
                        hijo.f(nodo_actual, destino);
                        abiertos.add(hijo);
                        if(hijo.igual(destino))
                            encontrado = true;
                    }
                }
            }
        }   // while
        
        // Si se ha encontrado un camino, generarlo
        if(encontrado) {
            destino = nodo_actual;
            System.out.println("Iteraciones: " + i);
            generarCamino();
        }
        else
            System.out.println("Camino no encontrado");
    }
    
    
    /**
     * Reconstruye el camino a partir del nodo objetivo, recorriendo el arbol en
     * sentido inverso a traves de los padres.
     * 
     */
    private void generarCamino() {
        Nodo n = destino;
        while(n.padre != null) {
            camino.add(n);
            n = n.padre;
        }
        
        System.out.println("Camino generado: ");
//        System.out.println(camino.toString());
    }
    
    
    /**
     * Devuelve el camino generado
     * 
     * @return pila con los nodos a recorrer para llegar al destino
     */
    public Stack<Nodo> getCamino() {
        return camino;
    }
    
    
    /**
     * Comienza la busqueda. Interfaz a la funcionalidad
     */
    public void start() {
        buscar();
    }
    
    
    /**
     * Comprueba si el Nodo N se encuentra dentro de la Cola c.
     * 
     * Este metodo puede verse como una sobrecargar del metodo contains de Java,
     * sin embargo, en mi caso solo me interesa conocer si las coordenadas del
     * nodo son iguales (por lo tanto el nodo lo es), ya que el resto de valores
     * puede y es variable, lo que hace que contains "falle" y se re-exploren
     * nodos.
     * 
     * @param c
     * @return 
     */
    private boolean contiene(PriorityQueue<Nodo> c, Nodo n) {
        for (Nodo a : c)
            if ( (a.X == n.X) && (a.Y == n.Y) )
                return true;
        return false;
    }
    
    
    /**
     * 
     * @param destino 
     */
    public void setDestino(Casilla destino) {
        this.destino = new Nodo(destino);
    }


    /**
     * 
     * @param origen 
     */
    public void setOrigen(Casilla origen) {
        this.origen = new Nodo(origen);
    }
    

    /**
     * Recibe el mapa en tipo Casilla (X, Y, tipo) y los convierte a tipo Nodo
     * para poder efectuar las operaciones necesarias para calcular el camino.
     * 
     * @param mapa 
     */
    public void setMapa(ArrayList<Casilla> mapa) {
        this.mapa.clear();
        
        for (Casilla c : mapa)
            this.mapa.add(new Nodo(c));

        // A continuacion, obtener coste (f, g, h) para cada Nodo. Como es posible
        // que no sea necesario que todos los nodos del mapa sean expandidos,
        // el calculo del coste se realiza durante la expansion del nodo padre.
    }
    
    
    /**
     * Muestra por pantalla una representacion del mapa y los valores f de cada
     * casilla.
     */
    public void getMapaF() {
        int tam = (int) Math.sqrt(mapa.size());
        
        for(int x=0; x<tam; x++){
            for(int y=0; y<tam; y++)
                System.out.print(" " + mapa.get(x*tam+y).f + " ");
                
            System.out.println("");
        }
    }

    
    /***************************************************************************
     * 
     * TEST
     * 
     **************************************************************************/
//    public static void main(String args[]) {
//        ArrayList<Casilla> map = new ArrayList();
//        Casilla origen = new Casilla(0,0);
//        Casilla destino = new Casilla(0,0);
//        int tam = 50;
//        
//        for(int x=0; x<tam; x++) {
//            for (int y=0; y<tam; y++) {
//                // Insercion de bordes
//                if( (x==0) || (y==0) )
//                    map.add(new Casilla(x,y,'B'));
//                else if( (x==(tam-1)) || (y==(tam-1)) )
//                    map.add(new Casilla(x,y,'B'));
//                // Insercion de muros
//                else if( (x==2) && (y>1) )
//                    map.add(new Casilla(x,y,'M'));
//                else if( (x==(tam-4)) && (y<(tam-2)) )
//                    map.add(new Casilla(x,y,'M'));
//                // Insercion de objetivo
//                else if( (x==tam-2) && (y==2) ) {
//                    destino = new Casilla(x,y,'X');
//                    map.add(destino);
//                }
//                // Insercion de origen
//                else if( (x==1) && (y==tam-2) ) {
//                    origen = new Casilla(x,y,'S');
//                    map.add(origen);
//                }
//                else
//                    map.add(new Casilla(x,y,' '));
//            }
//        }
//        
//        PathFinder pf = new PathFinder();
//        pf.setOrigen(origen);
//        pf.setDestino(destino);
//        pf.setMapa(map);
//        pf.start();
//        Stack<Nodo> n = pf.getCamino();
//        
////        pf.getMapaF();
//        
//        for (Nodo a : n) {
//            map.set(a.X*tam+a.Y, new Casilla(a.X, a.Y, '*'));
//        }
//        
//        for(int x=0; x<tam; x++){
//            for(int y=0; y<tam; y++)
//                System.out.print(" " + map.get(x*tam+y).getTipo() + " ");
//                
//            System.out.println("");
//        }
//    }
}