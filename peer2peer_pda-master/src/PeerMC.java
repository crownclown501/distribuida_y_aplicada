import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import javax.swing.JTextArea;
/**
 *
 *
 */
public class PeerMC implements Runnable{
    private MulticastSocket socket;
    private InetAddress host;
    private byte [] mensaje;
    private byte [] msj_Enviado;
    private DatagramPacket paquete;
    private int port;
    private JTextArea area;
    //"230.0.0.4"
    public PeerMC(String host, int port){
        
        try {
            this.socket = new MulticastSocket(port);
            this.host = InetAddress.getByName(host);
            this.port = port;
            this.mensaje = new byte[100];
            this.msj_Enviado = new byte[100];
        } catch (UnknownHostException ex) {
        } catch (IOException ex) {
        }
    }
    public void set_Area(JTextArea area){
        this.area=area;
    }
    public void enviar_Mensaje(String msj){
        this.mensaje = msj.getBytes();
        this.paquete = new DatagramPacket(this.mensaje,this.mensaje.length,host,port);
        try {
            this.socket.send(paquete);
            System.out.println("Mensaje enviado");
        } catch (IOException ex) {
        }
    }
    
    public void run() {
        String mensaje_Recibido="";
        try {
                this.socket.joinGroup(host);
                
                while(true){
                    
                    this.paquete = new DatagramPacket(this.msj_Enviado,this.msj_Enviado.length);
                    synchronized(this){
                        this.socket.receive(paquete);
                    }
                    mensaje_Recibido+= new String(paquete.getData()) + "\n";
                    //System.out.println("Recibio: "+mensaje_Recibido);
                    this.area.setText(mensaje_Recibido); 
                
                }
        } catch (IOException ex) {

        }
    }
}
