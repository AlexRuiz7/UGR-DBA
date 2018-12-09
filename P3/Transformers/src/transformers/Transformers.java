/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Germán
 */
public class Transformers {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
         try{
            AgentID nombre = new AgentID("Optimus_Prime");
            AgentID destinatario = new AgentID("Explorador1");
            
            // Creando conexión con el servidor
            AgentsConnection.connect("isg2.ugr.es", 6000, "Girtab", "Geminis", "France", false);
            System.out.println("Conectado a isg2.ugr.es");
            
            // Crear agente
            Burocrata optimusPrime = new Burocrata(nombre);
            
            // Despiertas agente.
            //optimusPrime.start();
        }
        catch(Exception ex) {
            System.out.println("Excepción " + ex.toString());
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
