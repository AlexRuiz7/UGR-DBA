package transformers.utils;

/**
 *
 * @author Alejandro
 */
public class Nodo extends Casilla{
    protected double f, g, h;

    
    /**
     * Constructor
     * 
     */
    public Nodo() {
        super(0, 0, '?');
        f = g = h = 0;
    }
    
    
    /**
     * Constructor de copia
     * 
     * @param c 
     */
    public Nodo(Casilla c) {
        super(c.X, c.Y, c.getTipo());
        f = g = h = 0;
    }   
    
    
    /**
     * Calcula y devuelve el valor del nodo
     * 
     * @param origen
     * @param destino
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
     * El calculo de la raiz cuadrada se omite ya que no afecte al resultado
     * final, pero si al rendimiento.
     * 
     * @param origen
     * @return valor G del nodo (distancia recorrida)
     */
    public double g(Casilla origen) {
        g = Math.pow(origen.X - X, 2) + Math.pow(origen.Y - Y, 2);
        return g;
    }
    
    
    /**
     * Calcula y devuelve la distancia que hay entre el nodo y el destino
     * 
     * Distancia entre 2 puntos: d = sqrt[ (x2 - x1)**2 + (y2 - y1)**2 ]
     * 
     * El calculo de la raiz cuadrada se omite ya que no afecte al resultado
     * final, pero si al rendimiento.
     * 
     * @param destino
     * @return valor H del nodo (heuristica)
     */
    public double h(Casilla destino) {
       h = Math.pow(destino.X - X, 2) + Math.pow(destino.Y - Y, 2);
       return h;
    }
    
}
