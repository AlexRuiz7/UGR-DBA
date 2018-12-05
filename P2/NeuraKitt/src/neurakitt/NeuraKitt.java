package neurakitt;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alvaro
 */
public class NeuraKitt {

    /**
     * @author Alvaro, Juan Germán
     * @param args the command line arguments
     * 
     * @author Alejandro
     * @custom.FechaModificacion 01/11/2018
     * @custom.Motivo añadida traza en caso de excepción
     */
    public static void main(String[] args) {
        Logger.global.setLevel(Level.OFF);
        
        try {
            // Identificadores de los agentes
            AgentID idKITT  = new AgentID("KITT");
            AgentID idNEURA = new AgentID("NEURA");
            
            // Mapa a explorar
            String mapa = "map1";
            
            // Creando conexión con el servidor
            AgentsConnection.connect("isg2.ugr.es", 6000, "Girtab", "Geminis",
                                      "France", false);
            System.out.println("Conectado a isg2.ugr.es");
            
            
            boolean anillo_exterior = true;
            int tope = 5000;
            int tiempo_olvido = Integer.MAX_VALUE;
            
            Kitt KITT   = new Kitt(idKITT, idNEURA.getLocalName(), mapa,
                                    anillo_exterior, tiempo_olvido);
            System.out.println("Agente KITT creado");
            Neura NEURA = new Neura(idNEURA, idKITT, anillo_exterior, tope, tiempo_olvido);
            System.out.println("Agente NEURA creado");
            
            // Comenzar actividad.
            NEURA.start();
            KITT.start();
            
        } catch (Exception ex) {
            System.out.println("Excepción " + ex.toString());
            Logger.getLogger(NeuraKitt.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
