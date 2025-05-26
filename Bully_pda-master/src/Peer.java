
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Larios
 */
public class Peer implements Runnable{
    private MulticastSocket socket;
    private InetAddress host;
    private int port = 5000;
    
    private int id;
    private boolean Coordinador=false;
    private boolean Elector_lock=false;
    private boolean imp=true;
    
    private int count=0;
    
    private JTextArea ar1=null;
    private String mensaje="";
    
    public Peer(int id,boolean c){
        this.id=id;
        this.Coordinador=c;
        
        try {
            socket = new MulticastSocket(port);
            host = InetAddress.getByName("230.0.0.5");
            socket.joinGroup(host);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * Se envia el mensaje para informar a todos los peer que es el coordinador actual
     * @param id identificador del peer que se declara coordinador.
     */
    private void msg_Coordinador(int id){
        byte buffer []= ("Coordinador "+id).getBytes();
        DatagramPacket paquete = new DatagramPacket(buffer,buffer.length,host,port);
        try {
            socket.send(paquete);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * Se envia el mensaje para el proceso de eleccion a todos los peer informando
     * que es un posible candidato a coordinador
     * @param id identificador del peer que pretende ser coordinador
     */
    private void msg_Eleccion(int id){
        byte buffer[] = ("Eleccion "+id).getBytes();
        DatagramPacket paquete = new DatagramPacket(buffer,buffer.length,host,port);
        try {
            socket.send(paquete);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void set_Area(JTextArea ar){
        ar1=ar;
    }
    
    @Override
    public void run() {
        /**
         * Thread-1
         * 
         * Si es coordinador
         * Encargado de enviar el mensaje notificando a todos los peers
         * 
         * Si no es coordinador
         * Encargado de esperar el mensaje del coordinador actual
         * monitorear tanto mensajes Coordinador como mensajes Eleccion 
         */
        Runnable T_coordinador = new Runnable(){
            
            @Override
            public void run() {
                try {
                    DatagramPacket paquete;
                    LinkedList pack;
                    socket.setTimeToLive(0);
                    while(true){
                            if(Coordinador){
                                //System.out.println("Soy el coordinador "+id);
                                msg_Coordinador(id);
                                    mensaje+=("\nSoy el coordinador "+id+"\n");
                                    if(ar1!=null)
                                        ar1.setText(mensaje);
                            }
                            if(!Coordinador){
                                try {
                                
                                    byte buffer[]= new byte[20];
                                    paquete=new DatagramPacket(buffer,buffer.length);
                                    socket.setSoTimeout(3000);
                                    socket.receive(paquete);
                                
                                    pack = to_Split_Datagram(paquete.getData());
                                
                                    String msg = String.valueOf(pack.get(0));
                                    int id_rec = Integer.parseInt(String.valueOf(pack.get(1)));
                                
                                    if(msg.equalsIgnoreCase("Coordinador")){
                                        Elector_lock=false;
                                        //System.out.println("El coordinador actual es: "+id_rec+" soy: "+id);
                                            mensaje+=("El coordinador actual es: "+id_rec+" soy: "+id+"\n");
                                            if(ar1!=null)
                                                ar1.setText(mensaje);
                                            
                                    }
                                    if(msg.equalsIgnoreCase("Eleccion")){
                                        if(count>2){
                                            Coordinador=true;
                                                //System.out.println("Count: "+count+" en: "+id+"Coordinador: "+Coordinador);
                                                mensaje+=("Count: "+count+" en: "+id+"Coordinador: "+Coordinador);
                                                if(ar1!=null)
                                                    ar1.setText(mensaje);                                            
                                        }
                                        if(id<id_rec){
                                            Elector_lock=true;
                                        }
                                        if(id==id_rec){
                                            count++;
                                        }
                                    }
                                } catch (IOException ex) {
                                    
                                    if(!Elector_lock){
                                        
                                        //System.out.println("Se envia mensaje eleccion: "+id);
                                        msg_Eleccion(id);
                                            mensaje+=("Se envia mensaje eleccion: "+id+"\n");
                                            if(ar1!=null)
                                                ar1.setText(mensaje);
                                    }
                                }
                            }
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };new Thread(T_coordinador).start();
        
        /**
         * Thread-2
         * Encargado de monitorear los mensajes de posibles candidatos a coordinador
         * si recibe un id mayor este se queda a la espera del  nuevo coordinador
         */
    }
    /**
     * Separa la informacion del datagrama
     * @param cad arreglo de bytes 
     * @return LinkedList usada para almacenar el resultado del cast
     */
    private LinkedList to_Split_Datagram(byte []cad){
        LinkedList lista= new LinkedList();
        String alpha = "";
        String num="";
        String cadena=new String(cad);
        for(char c:cadena.toCharArray()){
            if(Character.isAlphabetic(c)){
                alpha+=c;
            }
            if(Character.isDigit(c)){
                num+=c;
            }
        }
        lista.add(alpha);
        lista.add(Integer.parseInt(num));
        return lista;
    }
    
}
