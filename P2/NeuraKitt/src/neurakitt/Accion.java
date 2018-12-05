package neurakitt;

/**
 * Clase de tipo Enumerado que contiene las acciones posibles del agente NEURA
 * Usar el método toString() para convertir cada elemento en un string usable por
 * JSON, del modo que Accion.moveN.toString(); devuelve "moveN"
 * 
 * @author Alejandro
 * @date 01/11/2018
 * @deprecated No usar hasta nuevo aviso. Álex.
 */
public enum Accion {
    moveNW, moveN, moveNE, moveW, logout, moveE, moveSW, moveS, moveSE
}
