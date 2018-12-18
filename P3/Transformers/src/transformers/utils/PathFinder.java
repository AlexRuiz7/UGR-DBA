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

/**
 * Esta clase calcula el mejor que camino (si lo hay) desde un punto origen y 
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
    private ArrayList<Nodo> abiertos;
    private ArrayList<Nodo> cerrados; 
    private Nodo destino, origen;
    private ArrayList<Nodo> mapa;

    
   
    /**
     * Constructor
     */
    public PathFinder() {
        this.destino = this.origen = new Nodo();
        this.mapa = new ArrayList();
        
        abiertos = cerrados = new ArrayList();
    }
    

    /**
     * 
     */
    private void calcularPlan(){
        abiertos.clear();
        cerrados.clear();
        
        abiertos.add(origen);
        
//        for()
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
        mapa.clear();
        
        for (Casilla c : mapa)
            mapa.add(new Nodo(c));

        // A continuacion, obtener coste (f, g, h) para cada Nodo. Como es posible
        // que no sea necesario que todos los nodos del mapa sean expandidos,
        // el calculo del coste se realiza durante la expansion del nodo padre.
    }

    /**
     * 
     * @return 
     */
    @Override
    public String toString() {
        return "PathFinder{" + "destino=" + destino + ", origen=" + origen + ", mapa=" + mapa + '}';
    }
    
}
