/*
Author:     Carlos Miguel Fernando
GitHub:     https://github.com/ByteThis
LinkedIn:   https://www.linkedin.com/in/0xcmf
*/


import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // GUI RELATED VARIABLES
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- END OF GUI RELATED VARIABLES

    static Socket socket;
    static DataOutputStream dataOut;
    static BufferedReader dataIn;
    
    // Method used to add a string to the text box
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Constructor
    public ChatClient(	) throws IOException {

        // GUI INITIALIZATION
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                   chatBox.setText("");
                }
            }
        });
        // --- END OF GUI INITIALIZATION


        frame.addWindowListener(new WindowAdapter() {
            public void closeWindow(WindowEvent e) {
                try{
                    newMessage("/bye");
                    socket.close();
                }   catch(Exception exp){
                    System.out.println("Error closing socket when closing the window");
                    exp.printStackTrace();
                }
            }
        });

    }


    // Invoked everytime user inserts something in the message box
    public void newMessage(String message) throws IOException {
    	System.out.println(message);
        dataOut.write(message.getBytes("UTF-8"));
    }

    
    // Main Method
    public void run() throws IOException {
        String fromServer;
        String msg="";
        String event=null;
        String arr[] = null;


        while((fromServer=dataIn.readLine()) != null){

            arr=fromServer.split(" ");
            event=arr[0];
                   

            if(event.equals("MESSAGE")){

                msg= arr[2];
                for(int i=3;i<arr.length;i++){
                    msg= msg+" "+arr[i]; 
                }

                printMessage(arr[1] + ": " + msg + '\n');
                msg="";
            }
            else if(event.equals("NEWNICK")){
                printMessage(arr[1] + " has changed name to " + arr[2] + '\n');
            }
            else{
                printMessage(fromServer + '\n');
            }
        }

    }
    

    // ChatClient Instance and startup by invoking run()
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient();
        socket=new Socket(args[0], Integer.parseInt(args[1]));
        dataOut=new DataOutputStream(socket.getOutputStream());
        dataIn=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        client.run();
    }

}
