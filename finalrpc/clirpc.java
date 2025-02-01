package distribuida_y_aplicada.finalrpc;

import java.io.*;
import java.net.*;

public class clirpc {
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws IOException {
        @SuppressWarnings("resource")
        Socket soc = new Socket("localhost",3000);
        DataInputStream dis = new DataInputStream(soc.getInputStream());
        PrintWriter pw= new PrintWriter(soc.getOutputStream(),true);
        DataInputStream kb = new DataInputStream(System.in);
        System.out.println("client ready, type and press Enter key");
        String receiveMessage, sendMessage, temp;
        while (true) {
            System.out.println("\nEnter operation to perform(add,sub,mul,div)...or type 'exit' to quit");
            temp=kb.readLine();
            sendMessage = temp.toLowerCase();
            if (sendMessage.equals("exit")) {
                System.out.println("Exiting...");
                break; 
            }
            pw.println(sendMessage);
            System.out.println("Enter first parameter: ");
            sendMessage = kb.readLine();
            pw.println(sendMessage);
            System.out.println("Enter second parameter: ");
            sendMessage = kb.readLine();
            pw.println(sendMessage);
            System.out.flush();
            if ((receiveMessage=dis.readLine())!=null)
                System.out.println(receiveMessage);
        }
    }
}
