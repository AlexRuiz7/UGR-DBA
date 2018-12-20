/******************************************************************************
 *  Clase:          Nodo.java
 *  Autor:          Alejandro Ruiz Becerra
 *  Dependencias:   Casilla.java
 *
 *  PathFinder
 * 
 * https://medium.com/@nicholas.w.swift/easy-a-star-pathfinding-7e6689c7f7b2
 * 
 ******************************************************************************/
package transformers.utils;

/**
 * Estructura de datos necesaria para el PathFinder. Permite la generacion de 
 * grafos para la exploracion, asi como el calcula de los valores f, g, h.
 * 
 * Esta clase hereda de la clase Casilla ya que es necesario conocer las 
 * coordenadas y la tipografia de cada casilla.
 * 
 * @author alex
 */
public class Nodo extends Casilla implements Comparable<Nodo> {
    protected double f, g, h;
    protected Nodo padre;

    
    /**
     * Constructor basico para permitir la instanciacion
     * 
     */
    public Nodo() {
        super(0, 0, '?');
        f = g = h = 0;
        padre = null;
    }
    
    
    /**
     * Constructor de copia a partir de objetos de la superclase (conversion)
     * 
     * @param c Casilla que representa al nodo
     */
    public Nodo(Casilla c) {
        super(c.X, c.Y, c.getTipo());
        f = g = h = 0;
    }
    
    
    /**
     * Constructor predeterminado. Convierte e inicializa u nodo.
     * 
     *  - Conversion desde tipo Casilla (superclase) a tipo Nodo
     *  - Insercion en el grafo (asignacion de padre)
     * 
     * @param c Casilla que represnta al nodo
     * @param n Nodo Padre
     */
    public Nodo (Casilla c, Nodo n) {
        this(c);
        this.padre = n;
    }
    
    
    /**
     * Compara los valores f de dos nodos, devolviendo el mejor de ellos.
     * 
     * @param n nodo a comparar
     * @return mejor nodo
     */
    public Nodo mejor (Nodo n) {
        if (n.f <= this.f)
            return n;
        else
            return this;
    }
    
    
    /**
     * Sobrecarga del metodo compareTo de Java Collections. Necesario para poder
     * implementar colas de prioridad con estruturas de datos abstractas (Nodo)
     * 
     * Dados 2 nodos, el invocante y el parametrizado, devuelve un entero que 
     * indica cual de ellos es el mayor (mayor valor f).
     * 
     * @param n Nodo a comparar
     * @return 1 si el objeto invocante es mayor que el pasado por parametro.
     *         0 si los objetos son iguales
     *         -1 si el objeto invocante es menor que el pasado por parametro
     */
    @Override
    public int compareTo(Nodo n) {
        if(this.f > n.f)    return 1;
        if(this.f < n.f)    return -1;
        else                return 0;
    }
    
    
    /**
     * Sobrecarga del operador == o el metodo equals() nativo de Java Collections.
     * 
     * El metodo equals() devulve true si los objetos son exactamente iguales, es
     * decir si siendo del mismo tipo, tienen el mismo estado. Esto no es lo que
     * necesito, yo para compara Nodos necesito saber si sus coordenadas son la
     * mismas, luego los nodos lo son. El resto de atributos me es indiferente
     * si son iguales o no.
     * 
     * @param n Nodo a comparar
     * @return true si son iguales, false en caso contrario
     */
    public boolean igual(Nodo n) {
        return ( (this.X == n.X) && (this.Y == n.Y));
    }
    
    
    /**
     * Comprueba si un nodo es transitable. Un nodo es transitable si su 
     * topografia es distinta de M o B, es decir, si la casilla no representa 
     * un obstaculo (muro) o un borde (fin del mundo).
     * 
     * Los vehiculos, al ser ser dinamicos (se mueven) no se tienen en cuenta.
     * 
     * *********************************************************************
     * TODO: Añadir la comprobacion de vehiculos
     *  - Cambiar valor de retorno a char, de modo que:
     *      - 'S': nodo transitable
     *      - 'N': nodo no transitable, muro, borde.
     *      - 'V': nodo transitable. Vehiculo encontrado.
     *  - Si el valor devuelto es 'V':
     *      - Añadir la casilla actual a la lista de Incidencias
     *      - El Burocrata debe recoger esta lista y gestionarla si es necesario:
     *          - Para cada incidencia, detectar que vehiculo esta en esa casilla
     *          - Ordenar al vehiculo que mueva su culo de ahi
     * *********************************************************************
     * 
     * @return true si es transitable (no hay obstaculos) false en caso contrario
     */
    public boolean esTransitable() {
        return !((this.getTipo() == 'M') | (this.getTipo() == 'B'));
    }
    
    
    /**
     * Calcula y devuelve el coste todal del nodo, desde el origen hasta el
     * destino
     * 
     * @param origen Nodo oirgen
     * @param destino Nodo destino
     * @return valor F (total) del nodo
     */
    public double f(Casilla origen, Casilla destino) {
        f = g(origen) + h(destino);
        return f;
    }
    
    
    /**
     * Calcula y devuelve la distancia que hay entre el nodo y el origen
     *  
     * Distancia entre 2 puntos: d = sqrt[ (x2 - x1)**2 + (y2 - y1)**2 ]
     * 
     * El calculo de la raiz cuadrada se omite ya que no afecta al resultado
     * final, pero si al rendimiento.
     * 
     * Actualizacion: el calculo de la distancia se hace ahora conforme a la 
     * distancia Manhattan: d = |x2-x1| + |y2-y1|
     * 
     * @param origen Nodo origen
     * @return valor G del nodo (distancia recorrida)
     */
    private double g(Casilla origen) {
        /******************* Distancia Euclidea  *****************************/
        // g = Math.pow(origen.X - X, 2) + Math.pow(origen.Y - Y, 2);
        
        /******************* Distancia Manhattan  ****************************/
        g = Math.abs(this.X-origen.X) + Math.abs(this.Y-origen.Y);
        return g;
    }
    
    
    /**
     * Calcula y devuelve la distancia que hay entre el nodo y el destino
     * 
     * Distancia entre 2 puntos: d = sqrt[ (x2 - x1)**2 + (y2 - y1)**2 ]
     * 
     * El calculo de la raiz cuadrada se omite ya que no afecta al resultado
     * final, pero si al rendimiento.
     * 
     * Actualizacion: el calculo de la distancia se hace ahora conforme a la 
     * distancia Manhattan: d = |x2-x1| + |y2-y1|
     * 
     * @param destino Nodo objetivo
     * @return valor H del nodo (heuristica)
     */
    private double h(Casilla destino) {
        /******************* Distancia Euclidea  *****************************/
        // h = Math.pow(destino.X - X, 2) + Math.pow(destino.Y - Y, 2);
        
        /******************* Distancia Manhattan  ****************************/
        h = Math.abs(this.X-destino.X) + Math.abs(this.Y-destino.Y);
        return h;
    }

    
    /**
     * 
     * @return 
     */
    @Override
    public String toString() {
        return "Nodo{" + "f=" + f + " x=" + X + " y=" + Y + "}";
    }
    
}
