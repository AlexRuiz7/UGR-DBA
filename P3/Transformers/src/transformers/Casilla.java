/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

/**
 *
 * @author Germán
 */
public class Casilla {
    public final int X;
    public final int Y;
    
    public Casilla(int coord_X, int coord_Y) {
        X = coord_X;
        Y = coord_Y;
    }
    
    public String toString(){
        return "[X, Y] : [" + X + ", " + Y +"]";
    }
}
