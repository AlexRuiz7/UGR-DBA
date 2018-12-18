/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Germán, Alvaro
 */
public class Transformers {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
         try{    
            String nombreServidor = "Girtab";
            String nombreMapa = "map1";
            AgentID id_servidor = new AgentID(nombreServidor);
            
            // Creando conexión con el servidor
            AgentsConnection.connect("isg2.ugr.es", 6000, nombreServidor, "Geminis", "France", false);
            System.out.println("Conectado a isg2.ugr.es");
            
            
            // Creando agentID para los agentes
            AgentID id_burocrata = new AgentID("Optimus_Prime000000000000");
            AgentID id_v1 = new AgentID("vehiculo000000000000");
            AgentID id_v2 = new AgentID("vehiculo111111111111");
            AgentID id_v3 = new AgentID("vehiculo222222222222");
            AgentID id_v4 = new AgentID("vehiculo333333333333");
            
            ArrayList<AgentID> vehiculos = new ArrayList<>();
            vehiculos.add(id_v1);
            vehiculos.add(id_v2);
            vehiculos.add(id_v3);
            vehiculos.add(id_v4);
            
             System.out.println("\n VEHICULOS: "+ vehiculos.toString()
             + " cuyo tamaño es: "+ vehiculos.size());
             
            // Creando a los agentes y activando el verboseo
            boolean informa = true;
            
            Burocrata optimusPrime = new Burocrata(id_burocrata, nombreServidor,
                    vehiculos, nombreMapa, informa);
            
            Vehiculo v1 = new Vehiculo(id_v1, id_servidor, id_burocrata, informa);
            Vehiculo v2 = new Vehiculo(id_v2, id_servidor, id_burocrata, informa);
            Vehiculo v3 = new Vehiculo(id_v3, id_servidor, id_burocrata, informa);
            Vehiculo v4 = new Vehiculo(id_v4, id_servidor, id_burocrata, informa);
            /** Despertando a los agentes.
             * @Nota: Es necesario que los vehículos despierten antes porque
             * esperan recbir del agente burócrata el conversationID.
             */
            v1.start();
            v2.start();
            v3.start();
            v4.start();
            optimusPrime.start();
            
        }
        catch(Exception ex) {
            System.out.println("Excepción " + ex.toString());
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
