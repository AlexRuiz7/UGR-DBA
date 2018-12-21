package transformers.utils;

/**
 * 
 * @author alex
 */
public class Heightmap extends Casilla {

    private int altura;
    
    /**
     * 
     * @param coord_X
     * @param coord_Y 
     */
    public Heightmap(int coord_X, int coord_Y) {
        super(coord_X, coord_Y);
        altura = 0;
    }
    
    
    /**
     * 
     * @param c 
     * @param a 
     */
    public Heightmap(Casilla c, int a) {
        super(c.X, c.Y);
        altura = a;
    }
    
    
    /**
     * 
     * @param alt 
     */
    public void setAltura(int alt) {
        altura = alt;
    }
    
    
    /**
     * 
     * @return 
     */
    public int getAltura() {
        return altura;
    }
}
