/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transformers;

import com.eclipsesource.json.JsonArray;
import java.util.ArrayList;

/**
 *
 * @author juan
 */
public class Comportamiento {
    protected ArrayList<Integer> indice;
    protected int movimiento_anterior1;
    protected int movimiento_anterior2;
    protected int movimiento_anterior3;
    
    ArrayList<String> movimiento;
    ArrayList<Double> radanner;
    boolean informa;

    public Comportamiento(boolean informa) {
        iniciar_estructura();
        this.informa = informa;
    }
    

    /**
     * @author Germán
     * Método que instancia los atributos necesarios para
     * realizar el procesamiento de los datos del sensor.
     * 
     *  @Nota: 
     *   1º] Se priorizan los movimientos diagonales
     *   2º] Se instancian obstaculo y radanner en función del 
     *        tamaño de movimiento (No se contempla la posición 4 )
     *   3º] Se crea una estructura de índices para automatizar la actualización
     *        del radanner.
     * 
     * Es decir,
     * datos del sensor:         s1, s2, s3, s4, yo, s5, s6, s7, s8
     * puntos cardinales:        NW,  N, NE,  W, yo,  E, SW,  S, SE
     * movimientos priorizados:  NW, NE, SE, SW,  N,  W,  S,  E, yo
     */
    protected void iniciar_estructura(){
        movimiento = new ArrayList();
        movimiento.add("moveNW");
        movimiento.add("moveNE");
        movimiento.add("moveSE");
        movimiento.add("moveSW");
        movimiento.add("moveN");
        movimiento.add("moveW");
        movimiento.add("moveS");
        movimiento.add("moveE");
        movimiento.add("idle");
        
        radanner  = new ArrayList();
        for(int i=0; i< movimiento.size(); i++){
            radanner.add(Double.POSITIVE_INFINITY);
        }
        
        indice = new ArrayList();
        indice.add(0);  // NW
        indice.add(2);  // NE
        indice.add(8);  // SE
        indice.add(6);  // SW
        indice.add(1);  // N
        indice.add(3);  // W
        indice.add(7);  // S
        indice.add(5);  // E
        indice.add(4);  // yo
        
        movimiento_anterior1 = -1;
        movimiento_anterior2 = -1;
        movimiento_anterior3 = -1;
        
        //Elemento exclusivo para camion
        shared_data = new ArrayList();
        for(int i=0; i< movimiento.size(); i++){
            shared_data.add(Integer.MAX_VALUE);
        }
        
    }
    
    /**
     * @author: German
     * Dependiendo del rango de vision del vehículo, podemos conocer
     *  que tipo de vehículo es.
     *  En base a ello decidir el tipo de comportamiento.
     * @param sensor
     * @return 
     */
    protected String explorar(JsonArray sensor) {
        int decision = -1;
        int rango = (int) Math.sqrt(sensor.size());
        
        switch(rango){
            case 3:
                vision_dron(sensor);
                break;
                
            case 5:
                vision_coche(sensor);
                break;
                
            case 11:
                vision_camion(sensor);
                break;
                
            default:
                System.out.println(" Rango no contemplado ("
                        + rango +")");
                break;       
        }
        decision = movimiento_elegido();
        System.out.println(" Decisión tomada: " + decision);
        System.out.println(" Movimiento asociado: " + movimiento.get(decision));
        
        return movimiento.get(decision);
    }
    
    /**
     * @author Germán
     * Método auxiliar para encontrar el valor mínimo
     * @return 
     */
    protected int movimiento_elegido(){
        double minimo= Integer.MAX_VALUE;
        int movimiento_asociado = -1;
        for(int i=0; i<radanner.size(); i++){
            if( radanner.get(i) != 0 && minimo > radanner.get(i)){
                minimo = radanner.get(i);
                movimiento_asociado = i;
            }
        }
        
        movimiento_anterior3 = movimiento_anterior2;
        movimiento_anterior2 = movimiento_anterior1;
        movimiento_anterior1 = movimiento_asociado;
        return movimiento_asociado;
    }

    
    
    
    
    protected void vision_dron(JsonArray sensor){
        if(informa)
            System.out.println(" Visión del Drón: ");
        
        // Actualización del radanner
/*        int j;
        for(int i=0; i< sensor.size(); i++){
            j= indice.get(i);
            radanner.set(i, ponderador(j, sensor));
        }
*/        
        // NW
        radanner.set(0, ponderador(0, sensor));
        
        
        // NE
        radanner.set(1, ponderador(2, sensor));
        
        
        // SE
        radanner.set(2, ponderador(8, sensor));
        
        
        // SW
        radanner.set(3, ponderador(6, sensor));
        
        
        // N
        radanner.set(4, ponderador(1, sensor));
        
        
        // W
        radanner.set(5, ponderador(3, sensor));
        
        
        // S
        radanner.set(6, ponderador(7, sensor));
        
        
        // E 
        radanner.set(7, ponderador(5, sensor));
        
        
        // yo
        radanner.set(8, ponderador(4, sensor));
        
 
    }
    
    
    
    
    protected void vision_coche(JsonArray sensor){
        boolean rango_coche = (int) Math.sqrt(sensor.size()) == 5;
        System.out.println(" Rango_coche: "+ rango_coche);
        if(rango_coche){
            double suma;
            
            /** 
             * Evita que la suma sea 0, lo que implica que el factor ponderador
             *  no actua, prococando una interpretacion erronea.  
             */
            double corrector = 0.2;     
          
            System.out.println(" Visión del coche: ");
        
            
            // NW
            suma = sensor.get(0).asInt() 
                 + sensor.get(1).asInt() 
                 + sensor.get(5).asInt()
                 + corrector;
            radanner.set(0, suma*ponderador(6, sensor));
        
        
            // NE 
            suma = sensor.get(3).asInt() 
                 + sensor.get(4).asInt() 
                 + sensor.get(9).asInt()
                 + corrector;            
            radanner.set(1, suma*ponderador(8, sensor));

        
            // SE    
            suma = (sensor.get(19).asInt() 
                 +  sensor.get(23).asInt() 
                 +  sensor.get(24).asInt())
                 + corrector;            
            radanner.set(2, suma*ponderador(18, sensor));       
        
            // SW   
            suma = (sensor.get(15).asInt() 
                 +  sensor.get(20).asInt() 
                 +  sensor.get(21).asInt())
                 + corrector;            
            radanner.set(3, suma*ponderador(16, sensor));
        
        
            // N 
            suma = (sensor.get(1).asInt() 
                 +  sensor.get(2).asInt() 
                 +  sensor.get(3).asInt())
                 + corrector;            
            radanner.set(4, suma*ponderador(7, sensor));
        
        
            // W
            suma = (sensor.get(5).asInt() 
                 + sensor.get(10).asInt() 
                 + sensor.get(15).asInt())
                 + corrector;            
            radanner.set(5, suma*ponderador(11, sensor));
        
            // S 
            suma = (sensor.get(21).asInt() 
                 +  sensor.get(22).asInt() 
                 +  sensor.get(23).asInt())
                 + corrector;            
            radanner.set(6, suma*ponderador(17, sensor));
        
            // E     
            suma = (sensor.get(9).asInt() 
                + sensor.get(14).asInt() 
                + sensor.get(19).asInt())
                + corrector;            
            radanner.set(7, suma*ponderador(13, sensor));
            
            
            // yo
            radanner.set(8, ponderador(12, sensor));
            
            
        }
        else{
            System.out.println(
                    " El rango del sensor no corresponde "
                    + "con la visión de un agente coche"
                    + "\n Por tanto NO se realizarán cambios");
        }
    }
    
    protected ArrayList<Integer> shared_data;
    protected void vision_camion(JsonArray sensor){
        boolean rango_camion = (int) Math.sqrt(sensor.size()) == 11;
        
        if(rango_camion){
            double suma;
            double corrector = 0.2;
            actualizarSharedData(sensor);
            if(informa)
                System.out.println(" Elementos de shared_data: "+ shared_data.toString());
            System.out.println(" Visión del camión: ");
            
            // NW
            suma = (sensor.get(0).asInt() + sensor.get(12).asInt() 
                 + sensor.get(24).asInt() + sensor.get(36).asInt()
                 + shared_data.get(0)     + shared_data.get(1))
                 + corrector;
            radanner.set(0, suma*ponderador(48, sensor));
        
        
            // NE   
            suma = (sensor.get(10).asInt() + sensor.get(20).asInt() 
                 +  sensor.get(30).asInt() + sensor.get(40).asInt()
                 +  shared_data.get(2)     + shared_data.get(3))
                 + corrector;
            radanner.set(1, suma*ponderador(50, sensor));
        
        
            // SE    
            suma = (sensor.get(120).asInt() + sensor.get(108).asInt() 
                  + sensor.get( 96).asInt() + sensor.get( 84).asInt()
                  + shared_data.get(4)      + shared_data.get(5))
                  + corrector;
            radanner.set(2, suma*ponderador(72, sensor));
       
        
            // SW   
            suma = (sensor.get(110).asInt() + sensor.get(100).asInt() 
                 +  sensor.get( 90).asInt() + sensor.get(80).asInt()
                 +  shared_data.get(6)      + shared_data.get(7))
                 + corrector;
            radanner.set(3, suma*ponderador(70, sensor));
        
        
            // N 
            suma = (sensor.get(5).asInt() + sensor.get(16).asInt() 
                 + sensor.get(27).asInt() + sensor.get(38).asInt()
                 + shared_data.get(1)     + shared_data.get(2))
                 + corrector;
            radanner.set(4, suma*ponderador(49, sensor));
        
        
            // W
            suma = (sensor.get(55).asInt() + sensor.get(56).asInt() 
                  + sensor.get(57).asInt() + sensor.get(58).asInt()
                  + shared_data.get(0)     + shared_data.get(7))
                  + corrector;
            radanner.set(5, suma*ponderador(59, sensor));
        
        
            // S 
            suma = (sensor.get(82).asInt() + sensor.get( 93).asInt() 
                 + sensor.get(104).asInt() + sensor.get(115).asInt()
                 + shared_data.get(6)      + shared_data.get(5))
                 + corrector;
            radanner.set(6, suma*ponderador(71, sensor));
        
        
            // E     
            suma = (sensor.get(62).asInt() + sensor.get(63).asInt() 
                 +  sensor.get(64).asInt() + sensor.get(65).asInt()
                 + shared_data.get(5)     + shared_data.get(4))
                 + corrector;
            radanner.set(7, suma*ponderador(61, sensor));
            
        }
        else{
            System.out.println(
                    " El rango del sensor no corresponde "
                    + "con la visión de un agente camion"
                    + "\n Por tanto NO se realizarán cambios");
        }
    }

    /**
     * @author German
     * Metodo auxiliar y exclusivo para el comportamiento de camion
     * @param sensor
     */
    protected void actualizarSharedData(JsonArray sensor){
        boolean rango_camion = (int) Math.sqrt(sensor.size()) == 11;
        if(rango_camion){
            int suma;
            
            // Region 0
            suma = sensor.get(11).asInt() + sensor.get(22).asInt() 
                 + sensor.get(23).asInt() + sensor.get(33).asInt() 
                 + sensor.get(34).asInt() + sensor.get(35).asInt() 
                 + sensor.get(44).asInt() + sensor.get(45).asInt() 
                 + sensor.get(46).asInt() + sensor.get(47).asInt();
            shared_data.set(0,suma);
            
            
            // Region 1
            suma = sensor.get( 1).asInt() + sensor.get( 2).asInt() 
                 + sensor.get( 3).asInt() + sensor.get( 4).asInt() 
                 + sensor.get(13).asInt() + sensor.get(14).asInt() 
                 + sensor.get(15).asInt() + sensor.get(25).asInt() 
                 + sensor.get(26).asInt() + sensor.get(37).asInt();
            shared_data.set(1,suma);
            
            
            // Region 2
            suma = sensor.get( 6).asInt() + sensor.get( 7).asInt() 
                 + sensor.get( 8).asInt() + sensor.get( 9).asInt() 
                 + sensor.get(17).asInt() + sensor.get(18).asInt() 
                 + sensor.get(19).asInt() + sensor.get(28).asInt() 
                 + sensor.get(29).asInt() + sensor.get(39).asInt();
            shared_data.set(2,suma);
            
            
            // Region 3
            suma = sensor.get(21).asInt() + sensor.get(31).asInt() 
                 + sensor.get(32).asInt() + sensor.get(41).asInt() 
                 + sensor.get(42).asInt() + sensor.get(43).asInt() 
                 + sensor.get(51).asInt() + sensor.get(52).asInt() 
                 + sensor.get(53).asInt() + sensor.get(54).asInt();
            shared_data.set(3,suma);
            
            
            // Region 4
            suma = sensor.get(73).asInt() + sensor.get( 74).asInt() 
                 + sensor.get(75).asInt() + sensor.get( 76).asInt() 
                 + sensor.get(85).asInt() + sensor.get( 86).asInt() 
                 + sensor.get(87).asInt() + sensor.get( 97).asInt() 
                 + sensor.get(98).asInt() + sensor.get(109).asInt();
            shared_data.set(4,suma);
            
            
            // Region 5
            suma = sensor.get( 83).asInt() + sensor.get( 94).asInt() 
                 + sensor.get( 95).asInt() + sensor.get(105).asInt() 
                 + sensor.get(106).asInt() + sensor.get(107).asInt() 
                 + sensor.get(116).asInt() + sensor.get(117).asInt() 
                 + sensor.get(118).asInt() + sensor.get(119).asInt();
            shared_data.set(5,suma);
            
            
            // Region 6
            suma = sensor.get( 81).asInt() + sensor.get( 91).asInt() 
                 + sensor.get( 92).asInt() + sensor.get(101).asInt() 
                 + sensor.get(102).asInt() + sensor.get(103).asInt() 
                 + sensor.get(111).asInt() + sensor.get(112).asInt() 
                 + sensor.get(113).asInt() + sensor.get(114).asInt();
            shared_data.set(6,suma);
            
            
            // Region 7
            suma = sensor.get(66).asInt() + sensor.get(67).asInt() 
                 + sensor.get(68).asInt() + sensor.get(69).asInt() 
                 + sensor.get(77).asInt() + sensor.get(78).asInt() 
                 + sensor.get(79).asInt() + sensor.get(88).asInt() 
                 + sensor.get(89).asInt() + sensor.get(99).asInt();
            shared_data.set(7,suma);
            
           
        }
    }

    /**
     * @author Germán 
     * Método auxiliar para ponderar los obstáculos
     * @param i
     * @param sensor
     * @return Devuelve:
     *           0   Si en esa posición hay obstáculo
     *           1   Si en esa posición no hay obstáculo
     *          -3   Si en esa posición está la meta
     *          -1   Si en esa posición hay un valor distinto de 0,1,2,3,4
     */
    protected double ponderador(int i, JsonArray sensor){
        int rango = (int) Math.sqrt(sensor.size());
        double fly = -1;
        double alpha = 0;
        double beta = 0;
        if(  i == posicion(movimiento_anterior1, rango)
          || i == posicion(movimiento_anterior2, rango)){
                alpha = 0.2;
        }

        if( i == posicion(movimientoOpuesto(movimiento_anterior1), rango))
            alpha += 0.2;
        
        if(rango == 3)
            fly = 1 + alpha ;
        else
            fly = 0;
        
        double resultado = 0;
        switch(sensor.get(i).asInt()){
                case 0:  resultado = 1 + alpha + beta;  break;
                case 1:  resultado = fly;        break;  
                case 3:  resultado = -3;         break;             
                case 2:            
                case 4:  resultado = 0;          break;               
                default: resultado = -1;         break;
                            
                }
        
        return resultado;
    }
    
    protected int posicion(int mov_anterior, int rango){
        int resultado;
        switch(rango){
            case 3:  resultado = iDron(mov_anterior);   break;
            case 5:  resultado = iCoche(mov_anterior);  break;
            case 11: resultado = iCamion(mov_anterior); break;
            default: resultado = -1;                    break;      
        }
        return resultado;
    }
    
    protected int iDron(int mov_anterior){
        return mov_anterior;
    }
    
    protected int iCoche(int mov_anterior){
        int resultado;
        switch(mov_anterior){
            case 0:  resultado =  6;  break;  
            case 1:  resultado =  8;  break;
            case 2:  resultado = 18;  break;
            case 3:  resultado = 16;  break;        
            case 4:  resultado =  7;  break;  
            case 5:  resultado = 11;  break;
            case 6:  resultado = 17;  break;
            case 7:  resultado = 13;  break;
            case 8:  resultado = 12;  break;
            default: resultado = -1;  break;
        }
        
        return resultado;
    }
    
    protected int iCamion(int mov_anterior){
        int resultado;
        switch(mov_anterior){
            case 0:  resultado = 48;  break;  
            case 1:  resultado = 50;  break;
            case 2:  resultado = 72;  break;
            case 3:  resultado = 70;  break;        
            case 4:  resultado = 49;  break;  
            case 5:  resultado = 59;  break;
            case 6:  resultado = 71;  break;
            case 7:  resultado = 61;  break;
            case 8:  resultado = 60;  break;
            default: resultado = -1;  break;
        }
        
        return resultado;
    }
    
    protected int movimientoOpuesto(int movimiento){
        int resultado;
        switch(movimiento){
            case 0:  resultado = 2;  break;       
            case 1:  resultado = 3;  break;           
            case 2:  resultado = 0;  break;           
            case 3:  resultado = 1;  break;            
            case 4:  resultado = 6;  break;          
            case 5:  resultado = 7;  break;            
            case 6:  resultado = 4;  break;           
            case 7:  resultado = 5;  break;           
            default      :  resultado = -1 ;  break;
        }
                
        return resultado;
    }
    
/** ZONA de pruebas ***********************************************************/ 
    
    public static void main(String[] args) throws Exception {
        Comportamiento c = new Comportamiento(true);
        System.out.println("Hola");
        JsonArray sensor = new JsonArray();
        sensor.add(0);         
        sensor.add(0);        
        sensor.add(0);
        sensor.add(0);
        sensor.add(0);
        sensor.add(0);
        sensor.add(0);
        sensor.add(0);
        sensor.add(0);
        
        c.vision_dron(sensor);
        System.out.println(" Primer movimiento:");
        System.out.println(" Radanner: " + c.radanner.toString());
        System.out.println(" Movimiento elegido: " + c.movimiento.get(c.movimiento_elegido()));
        
        c.vision_dron(sensor);
        System.out.println(" Segundo movimiento:");
        System.out.println(" Radanner: " + c.radanner.toString());
        System.out.println(" Movimiento elegido: " + c.movimiento.get(c.movimiento_elegido()));
        
        c.vision_dron(sensor);
        System.out.println(" Tercer movimiento:");
        System.out.println(" Radanner: " + c.radanner.toString());
        System.out.println(" Movimiento elegido: " + c.movimiento.get(c.movimiento_elegido()));
    
        c.vision_dron(sensor);
        System.out.println(" Cuarto movimiento:");
        System.out.println(" Radanner: " + c.radanner.toString());
        System.out.println(" Movimiento elegido: " + c.movimiento.get(c.movimiento_elegido()));
    
        c.vision_dron(sensor);
        System.out.println(" Quinto movimiento:");
        System.out.println(" Radanner: " + c.radanner.toString());
        System.out.println(" Movimiento elegido: " + c.movimiento.get(c.movimiento_elegido()));
        
        
        c.vision_dron(sensor);
        System.out.println(" Sexto movimiento:");
        System.out.println(" Radanner: " + c.radanner.toString());
        System.out.println(" Movimiento elegido: " + c.movimiento.get(c.movimiento_elegido()));
        
        
        JsonArray sensor_coche = new JsonArray();
        sensor_coche.add(0);         
        sensor_coche.add(0);        
        sensor_coche.add(0);
        sensor_coche.add(2);
        sensor_coche.add(2);
        sensor_coche.add(0);
        sensor_coche.add(1);
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);         
        sensor_coche.add(0);        
        sensor_coche.add(0);
        sensor_coche.add(4);
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);         
        sensor_coche.add(0);        
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);
        sensor_coche.add(0);
//        sensor_coche.add(4);
        System.out.println(" Tamaño del sensor: " + sensor_coche.size());
        

     
    }    
}
