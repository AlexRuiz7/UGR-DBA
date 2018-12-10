/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Germán
 */
public class Burocrata extends Agente{

    String conversationID;
    AgentID controlador;
    String nombreMapa;
    String equipo;
    BufferedImage mapa;
    String rutaFichero;
    
    Burocrata(AgentID aID, String nombreServidor, String nombreMapa) throws Exception {
        super(aID, true);  
        this.controlador = new AgentID(nombreServidor);
        conversationID = "";
        
        /**
         * Hasta que no se realice el chekin por parte de los exploradores
         * no se conoce la configuración de equipo.
         */
        equipo = "0000";
        System.out.println("Equipo " + equipo);
        
        /**
         * Necesario para crear el nombre del archivo que contendrá la traza
         */
        this.nombreMapa = nombreMapa;
        this.rutaFichero = "";
    }
    
    
    @Override
    public void execute(){
        /**
         * Inicialmente se manda un cancel para cerrar la sesión anterior.
         * y se procesa la traza para obtener las dimensiones del mapa
         */
//        System.out.println(" Llamando a CANCEL ");
        obtenerMapa();
        
        ToStringMapa("hex");
        subscribe(nombreMapa);
    }
    
    public boolean cancel(){
        // Creando mensaje vacio
        mensaje = new JsonObject();
        
        // Enviando petición de cancelación de suscripción
        enviarMensaje(controlador,ACLMessage.CANCEL, conversationID);
        recibirMensaje();
        
        // Comprobando respuesta del Controlador
        boolean resultado = mensajeEntrada.getPerformativeInt() == ACLMessage.AGREE;
        
        // Mostrando los resultados del controlador
        int performativa = mensajeEntrada.getPerformativeInt();
        switch(performativa){
            case ACLMessage.AGREE:
                System.out.println(" Aceptada petición de CANCEL ");
                break;
                
            case ACLMessage.NOT_UNDERSTOOD:
                System.out.println(" NO entiendo lo que quieres. ");
                System.out.println(" Detalles: " + mensaje.get("details"));
                break;
                
            default:
                System.out.println(
                        " Se ha recibido la performativa: " 
                        + performativa);
                break;    
        }
        
        return resultado;
        
    }
    
    private void subscribe(String nombreMapa){
        mensaje = new JsonObject();
        mensaje.add("world", nombreMapa);
        
        enviarMensaje(controlador, ACLMessage.SUBSCRIBE, conversationID);
        
        recibirMensaje();
        conversationID = mensajeEntrada.getConversationId();
        System.out.println("conversationID: "+ conversationID);
    }
    
    private void obtenerMapa(){
        try {
            if(!cancel()){
                subscribe(nombreMapa);
                cancel();
            }
            getTraza(true);
            mapa = getMapa(rutaFichero, "hex");
            
        } catch (IOException ex) {
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * PRE: La traza debe haberse recibido.
     * El método getTraza crea una imagen con la traza obtenida
     * @param exito: Indica el modo de tratamiento de la traza.
     *   TRUE --> Queremos la traza
     *   FALSE--> No queremos la traza pero debe gestionarse
     */
    private void getTraza(boolean exito){
        try {
            String fecha = Fecha();
            String nombreFichero =nombreMapa + "__" 
                    + equipo + " "
                    + fecha + "_"
                    + conversationID;
            
            if(exito){
                /* Recibimos la respuesta del servidor */
                recibirMensaje();
                rutaFichero= "../" 
                        + nombreFichero;
            }
            else{
                recibirMensaje();
                rutaFichero= "../" 
                        +"Error_traza_anterior_"
                        +nombreFichero;
            }
            
//            System.out.println("Mensaje recibido, del servidor, tras el logout: " + mensaje.toString());
            
            /* Cuando la respuesta es OK, guardamos la traza */
            if (mensaje.toString().contains("trace")) {
                System.out.println("Recibiendo traza");
                JsonArray ja = mensaje.get("trace").asArray();
            byte data[] = new byte [ja.size()];
            for (int i = 0 ; i < data.length; i++) {
                data[i] = (byte) ja.get(i).asInt();
            }

            FileOutputStream fos = new FileOutputStream(rutaFichero);
            
            fos.write(data);
            fos.close();

            System.out.println("Traza Guardada en "+ nombreFichero);
            }
            
        } catch (IOException ex) {
            System.err.println("Error al recibir la respuesta o al crear la salida con la traza");
            Logger.getLogger(Burocrata.class.getName()).log(Level.SEVERE, null, ex);
        }
    };
    
        /**
     * Método que muestra el contenido del Mapa, en dististos formatos.
     * con enteros, hexadecimal o con caracteres
     * @param nombre_archivo: Ubicación del archivo a decodificar y mostrar
     * @param tipo          : Formato de presentación ("int", "hex", "string" )
     * @return              : Devuelve BufferedImage 
     * @throws IOException  : Excepción que en esta versión no se controla.
     */
    public static BufferedImage getMapa(String nombre_archivo, String tipo) throws IOException{
        File imageFile = new File(nombre_archivo);
        BufferedImage image = ImageIO.read(imageFile);
        System.out.println("Características de la imagen. "
                + "\n Tipo:    " + image.getType()
                + "\n Altura:  " + image.getHeight()
                + "\n Anchura: " + image.getWidth());
        
        int hex;
        for(int c=0; c<image.getWidth(); c++){
            for(int f = 0; f<image.getHeight(); f++){
                hex = image.getRGB(f,c);
                
                System.out.print(conversor(hex, tipo) + " ");
            }
            System.out.println("");
        } 
        return image;
    }
    
    public static void MapaToString(String nombre_archivo) throws IOException{
        getMapa(nombre_archivo, "string");
    }
    
    public static String conversor(int hex, String tipo){
        String dato;
        
        if(tipo == "int")
            {dato = Integer.toString(hex);}
        else if(tipo == "hex")
            {dato = Integer.toHexString(hex);}
        else {
            switch(hex){
                case -4144960 :
// ffc0c0c0 ==> -4144960                    
                    dato = "?";
                    break;
                    
                case -1:
// ffffffff ==> -1                    
                    dato = " ";
                    break;
            
                case -16777216:
// ff000000 ==> -16777216                    
                    dato = "1";
                    break;
                    
                case -65536:
// ffff0000 ==> -65536                    
                    dato = "x";
                    break;
                    
                 case -16726016:
// ff00c800 ==> -16726016                    
                    dato = ".";
                    break;
                 case -16842496:
                     dato = "G";
                     break;
                default:
// ff00c800 ==> -16726016
                    dato = "$";
                    break;
            }
        }
        
        return dato;
    }
    
    public String Fecha(){
        Calendar c = Calendar.getInstance();
        int mes = c.get(Calendar.MONTH)+1;
        String fecha = + c.get(Calendar.DAY_OF_MONTH)+"-"
                + mes+"-"
                + c.get(Calendar.YEAR)+" "
                + c.get(Calendar.HOUR_OF_DAY)+":"
                + c.get(Calendar.MINUTE)+":"
                + c.get(Calendar.SECOND);
        return fecha;
    }
    
    private void ToStringMapa(String tipo){
        int hex; 
        for(int c=0; c<mapa.getWidth(); c++){
            for(int f = 0; f<mapa.getHeight(); f++){
                hex = mapa.getRGB(f,c);
                
                System.out.print(conversor(hex, tipo) + " ");
            }
            System.out.println("");
        }
    };
}
