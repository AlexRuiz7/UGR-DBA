package transformers.utils;

/**
 * 
 * @author Germán
 * @custom.FechaModificacion 08/11/2018
 * @custom.Motivo
 *  Se han añadido los atributos:
 *
 *      - contadorDeActivacion: almacena la última vez que el agente pasó por la
 *                              casilla.
 *
 *      - memoria:              almacena las veces que el agente ha pasado por la 
 *                              casilla y trascurrido un número concreto de 
 *                              iteraciones olvida la información.
 * @author Alex
 */
public class Memoria extends Casilla {

    private int contadorDeActivacion;
    private int memoria;

    
    /**
     * 
     * @param coord_X
     * @param coord_Y 
     */
    public Memoria(int coord_X, int coord_Y) {
        super(coord_X, coord_Y);
        contadorDeActivacion = 0;
        memoria = 0;
    }

    
    /**
     * getMemoria devuelve las veces que el agente ha pasado por la casilla.
     * Además si trascurrido un tiempo (transirridas ciertas iteraciones)
     * no se vuelve a pasar por ella, se pierde dicha información.
     * @author German
     * 
     * @param pasosAndados
     * @param olvidarTrasKPasos
     * @return contador de la casilla.
     */
    public int getMemoria(int pasosAndados, int olvidarTrasKPasos) {
        
        if(this.getContador() == 0 || pasosAndados - contadorDeActivacion > olvidarTrasKPasos){
           memoria = 0;
        }
        else
            memoria++;
        
        contadorDeActivacion = pasosAndados;       
        return memoria;
    }
}
