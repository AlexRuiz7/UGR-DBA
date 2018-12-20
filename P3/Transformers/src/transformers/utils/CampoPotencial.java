/******************************************************************************
 *  Clase:  CampoPotencial.java
 *  Autor:  Alejandro
 *
 *  Representacion del mapa en forma tridimensional
 * 
 ******************************************************************************/
package transformers.utils;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Esta clase genera un mapa tridimensional (X, Y, altura) a partir de un mapa
 * bidimensional (X, Y). Para ello, analiza el mapa dado, y en aquellos lugares 
 * donde encuentra un obstaculo, eleva el terreno cercano. Por el contrario, si
 * en una posicion enceuntra un objetivo, disminuye la altura del terreno en esa
 * zona. Los obstaculos y los objetivos representan la maxima y la minima
 * altura del terreno, respectivamente. Ninguno de estos dos elementos ve su 
 * altura alterada si en las cercanias hay un elemento de tipo contrario, es decir,
 * la altura de un obstaculo no se ve afectada si junto a el existe un objetivo,
 * y viceversa.
 * 
 * Para hacer uso de esta clase, se debe crear uns instancia de la misma y
 * luego, cada vez que se necesite, actualizar el mapa mediante el metodo
 * creado para tal efecto. Luego, recoger la informacion proporcionada.
 *
 * @author alex
 */
public class CampoPotencial {
    private ArrayList<Heightmap> mapa;
    private int radio;
    
    
    /**
     * Constructor
     */
    public CampoPotencial() {
        mapa = new ArrayList();
        radio = 0;
    }
    
    
    /**
     * Constructor predeterminado
     * 
     * @param mapa 
     * @param r 
     */
    public CampoPotencial(ArrayList<Casilla> mapa, int r) {
        radio = r;
        for(Casilla c: mapa)
            this.mapa.add(new Heightmap(c.X, c.Y));
        
    }
    
    /**
     * 
     */
    public void generarMapa() {
        
        for(Casilla c)
        
        for(int x=-1; x<=1; x++) {
            for (int y=-1; y<=1; y++) {

            }
        }
        
        
    }
    
    
    /**
     * 
     * @return 
     */
    public ArrayList<Heightmap> getMapa() {
        return mapa;
    }

    /**
     * 
     * @param mapa 
     */
    public void setMapa(ArrayList<Heightmap> mapa) {
        this.mapa = mapa;
    }

    
    /**
     * 
     * @return 
     */
    public int getRadio() {
        return radio;
    }

    
    /**
     * 
     * @param radio 
     */
    public void setRadio(int radio) {
        this.radio = radio;
    }

    
    /**
     * 
     * @return 
     */
    @Override
    public String toString() {
        int tam = mapa.size();
        String salida = new String();
        
        for(int x=0; x<tam; x++){
            for(int y=0; y<tam; y++)
                salida.concat(" " + mapa.get(x*tam+y).getAltura() + " ");
                
            salida.concat("\n");
        }
        
        return salida + "\nradio=" + radio;
    }    
    
}
