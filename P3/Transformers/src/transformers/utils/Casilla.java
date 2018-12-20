package transformers.utils;

/**
 * Esta clase está dedicada a dar soporte a la memoria del agente. Constando de 
 * 2 miembros PÚBLICOS y 1 PRIVADO, almacena por cada posición en la que pasa, 
 * sus coordenadas y el número de veces que ha pasado por ella.
 * 
 * Los valores de las coordenadas se han establecido como valores constantes ya
 * que serán inalterables una vez establecidos (las coordenadas de esas celda 
 * no va a cambiar nunca), y solo pueden ser inicializados mediante el 
 * constructor. Cualquier intento de alterar las coordenadas de una casilla ya
 * creada producirá un error de ejecución.
 * 
 * Por otro lado, se ha asignado un contador para cada coordenada (X,Y), 
 * accesible públicamente pero sólo alterado privadamente. Cada vez que se pase
 * por una coordenada, se deberá llamar al método aumentarContador() para que
 * el contador aumente en uno.
 * 
 * La idea general, es crear un ArrayList en la clase NEURA, del tipo casilla,
 * inicializar el Array y luego, por cada movimiento, crear y añadir al array
 * un objeto del tipo casilla, previa comprobación de que esa casilla no haya
 * sido aún registrada en la memoria.
 * 
 * @author Alejandro
 * @custom.FechaModificacion 03/11/2018
 * 
 * 
 * @author Alejandro
 *
 * Para la practica 3 se han implementado los siguientes cambios:
 * 
 *  - Añadido atributo de tipo char para representar la naturaleza de la casilla:
 *      - L --> Libre
 *      - M --> Muro (obstaculo)
 *      - B --> Borde (fuera del mundo)
 *      - X --> Objetivo
 *      - V --> vehiculo
 *      - ? --> Desconocido (no explorado)
 */
public class Casilla {
    public final int X;
    public final int Y;
    private      int contador;
    private final char topografia;
    
   
    
    /**
     * Constructor con parámetros.
     * 
     * Constructor destinado a crear una nueva casilla de memoria, para la cual
     * se inicializan las coordenadas a los valores dados por parámetros y el 
     * contador a 0. Posteriormente de ser creada, deberá llamarse al método
     * aumentarContador()
     * 
     * @param coord_X   coordenada X
     * @param coord_Y   coordenada Y
     * @see aumentarContador()
     */
    public Casilla(int coord_X, int coord_Y) {
        X = coord_X;
        Y = coord_Y;
        contador = 0;
        topografia = '?';
    }
    
    
    /**
     * Constructor con parámetros.
     * 
     * Constructor destinado a crear una nueva casilla topografica.
     * 
     * @param coord_X   coordenada X
     * @param coord_Y   coordenada Y
     * @param tipo
     * @see aumentarContador()
     */
    public Casilla(int coord_X, int coord_Y, char tipo) {
        X = coord_X;
        Y = coord_Y;
        contador = 0;
        topografia = tipo;
    }     
            
    
    /**
     * @author Alejandro
     * 
     * Aumenta el contador de la casilla en una unidad.
     */
    public void aumentarContador() {
        contador++;
    }
    
    
    /**
     * @author Alejandro
     * @return contador de la casilla.
     */
    public int getContador() {
        return contador;
    }
    
    
    /**
     * 
     * @return 
     */
    public char getTipo(){
        return topografia;
    }
    
    
    /**
     * @author Alejandro
     * @return String
     */
    @Override
    public String toString(){
        return "[X,Y,#] : " + X + "," + Y;
    }
}
