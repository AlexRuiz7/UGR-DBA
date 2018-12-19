/******************************************************************************
 *  Clase:          QuadTree.java
 *  Autor:          Alejandro Ruiz Becerra
 *  Dependencias:   
 *
 *  Quad tree.
 * 
 * https://algs4.cs.princeton.edu/92search/QuadTree.java.html
 * 
 ******************************************************************************/

package transformers.utils;

import java.util.Scanner;

/**
 * 
 *  
 * @author Alejandro
 * @param <Key>
 * @param <Value> porcentaje explorado
 */

public class QuadTree<Key extends Comparable <Key>, Value> {
    private Nodo raiz;
    
    // Estructura de datos auxiliar
    private class Nodo {
        Key x, y;                       // Clave de busqueda. 
        Nodo NE, NO, SE, SO;
        Value porcentaje;
        
        Nodo(Key x, Key y, Value value) {
            this.x = x;
            this.y = y;
            this.porcentaje = value;
        }
    }
       
    /*********************************************************************
    *   Insertar (x, y) en el cuadrante apropiado
    **********************************************************************/
    public void insertar(Key x, Key y, Value valor) {
        raiz = insertar(raiz, x, y, valor);
    }
     
    private Nodo insertar(Nodo padre, Key x, Key y, Value valor) {
        if (padre == null)
            return new Nodo(x, y, valor);
        else if ( less(x, padre.x) &&  less(y, padre.y))
            padre.SE = insertar(padre.SE, x, y, valor);
        else if ( less(x, padre.x) && !less(y, padre.y))
            padre.NE = insertar(padre.NE, x, y, valor);
        else if (!less(x, padre.x) &&  less(y, padre.y))
            padre.SE = insertar(padre.SE, x, y, valor);
        else if (!less(x, padre.x) && !less(y, padre.y)) 
            padre.NE = insertar(padre.NE, x, y, valor);

        return padre;
    }
        
    /***************************************************************************
    *  Funciones auxiliares para comparaciones
    ***************************************************************************/

    private boolean less(Key k1, Key k2) { return k1.compareTo(k2) <  0; }
    private boolean eq  (Key k1, Key k2) { return k1.compareTo(k2) == 0; }
        
    
//    public static void main(String[] args) {
//        int N = 2;
//
//        QuadTree<Integer, String> st = new QuadTree();
//
//        // insert N random points in the unit square
//        for (int i = 0; i < N; i++) {
//            Integer x = (int) (100 * Math.random());
//            Integer y = (int) (100 * Math.random());
//            // StdOut.println("(" + x + ", " + y + ")");
//            st.insertar(x, y, "P" + i);
//        }
//        
//        boolean s = true;
//        Scanner cin = new Scanner(System.in);
//        int x, y;
//        
//        while(s){
//            System.out.println("Introduce cuadrante (x): ");
//            x = cin.nextInt();
//            System.out.println("Introduce cuadrante (y): ");
//            y = cin.nextInt();
//            
//        }
//    }
}
