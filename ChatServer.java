/*
Author: 	Carlos Miguel Fernando
GitHub:		https://github.com/ByteThis
LinkedIn: 	https://www.linkedin.com/in/0xcmf
*/


import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class User{
  String name;
  String currRoom;
  String status;
  Socket socket;

  public User(Socket s){
    socket = s;
    currRoom = null;
    name = "";
    status = "init";
  }
  
}

class Room{
    String roomName;
    LinkedList<User> online = new LinkedList<User>();

    Room(String s){
      roomName = s;
    }

  }

public class ChatServer{

  static LinkedList<User> people = new LinkedList<User>();
  static LinkedList<Room> rooms = new LinkedList<Room>();

  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );
    
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) ==
            SelectionKey.OP_ACCEPT) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
        	people.add(new User(s));
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking(false);

            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ );

          } else if ((key.readyOps() & SelectionKey.OP_READ) ==
            SelectionKey.OP_READ) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel) key.channel();
              boolean ok = processInput(sc);

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                Room room = null;

                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );

                  for(User u : people){
                    if(u.socket.equals(s)){
                      room=checkRoom(u.currRoom);
                      if(room != null){
                        chatEvents(u, room, "", "join"); 
                        room.online.remove(u);
                        if(room.online.isEmpty()){
                          rooms.remove(room);
                        }
                      }
                      people.remove(u);
                      break;
                    }
                  }

                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }

  static void sendMessage(User u, String text) {
    text+='\n';
    try{
      u.socket.getChannel().write(ByteBuffer.wrap(text.getBytes()));
    } catch (IOException e){
      System.out.println("Erro on sendMessage;");
      e.printStackTrace();
    }
  }


  static boolean checkName(String n){
    if(people.isEmpty()){
      return false;
    }
    for(User u : people){
      if (u.name.equals(n)){
        return false;
      }
    }
    return true;
  }

  static Room checkRoom(String n){
    for(Room r : rooms) {
      if (r.roomName.equals(n))
        return r;
    }
    return null;
  }



  static void chatEvents(User u, Room r, String text, String event) {
    switch (event){
      case "msg":
        text="MESSAGE " + u.name + " " + text + "\n";
        break;
      case "join":
        text="JOINED " + u.name + "\n";
        break;
      case "leave":
        text="LEFT  " + u.name + "\n";
        break;
      case "newname":
        text="NEWNICK " + u.name + " " + text + "\n";
        break;
      default:
        System.out.println("ERRO on chatEvents");
        break;
    }

    for(User usr : r.online){

    	if(event=="newname"){
			if(usr.name != u.name){
				try{
				usr.socket.getChannel().write(ByteBuffer.wrap(text.getBytes()));
				} catch(IOException e){
					System.out.println("Erro sending chatEvents;");
					e.printStackTrace();
				}
			}
		}

		else{
			try{
				usr.socket.getChannel().write(ByteBuffer.wrap(text.getBytes()));
			} catch(IOException e){
				System.out.println("Erro sending chatEvents;");
				e.printStackTrace();
			}
		}

    }

  }

  static void pvtMsg(String from, String to, String msg) {
		msg = "PRIVATE " + from + msg + '\n';

		for (User i : people) {
			try {
				if (i.name.equals(to))
					i.socket.getChannel().write(ByteBuffer.wrap(msg.getBytes()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}



// Just read the message from the socket and send it to stdout
  static private boolean processInput(SocketChannel sc) throws IOException {
  	String message;
  	Room temp;

    // Read the message to the buffer
    buffer.clear();
    sc.read( buffer );


    buffer.flip();

    // If no data, close the connection
    if (buffer.limit()==0) {
      return false;
    }

    User us = null;
    for (User i : people){
      if (i.socket.equals(sc.socket())){
        us=i;
        break;
      }
    }


    // Decode and print the message to stdout
    message = decoder.decode(buffer).toString();
    System.out.println( message );

    String arr[] = message.split("\\s+");

   if (message.length()==0){
      return true;
   }


   //Init status
	if (us.status.equals("init")){

		if (arr.length == 2 && arr[0].equals("/nick")) {

			//name available
        	if (checkName(arr[1])) {

	          us.name = arr[1];
	          us.status = "outside";

	          System.out.println("OK");
	          sendMessage(us, "OK");
        	}
			//name in use
	        else {
	          System.out.println("ERROR");
	          sendMessage(us, "ERROR");
	        }

			return true;
   		}


		if (arr.length == 1 && arr[0].equals("/bye")) {
			sendMessage(us, "BYE");
			return false;
		}


		System.out.println("ERROR");
		sendMessage(us, "ERROR");
		return true;
	}



	if (us.status.equals("outside")) {

		if (arr.length == 2 && arr[0].equals("/nick")) {

			//name available
			if (checkName(arr[1])) {
				us.name = arr[1];

				System.out.println("OK");

				sendMessage(us, "OK");
			}
			//name in use
			else {
				System.out.println("ERROR");
				sendMessage(us, "ERROR");
			}

			return true;
		}


		if (arr.length == 2 && arr[0].equals("/join")) {

			Room r = null;
			r = checkRoom(arr[1]);

			//room exists
			if (r != null) {
				us.status = "inside";
				us.currRoom = arr[1];

				sendMessage(us, "OK");

				//notify channel
				chatEvents(us, r, "", "join");
				r.online.add(us);
			}

			//room does not exist
			else {
				r = new Room(arr[1]);
				rooms.add(r);

				us.status = "inside";
				us.currRoom = arr[1];

				sendMessage(us, "OK");

				chatEvents(us, r, "", "join");
				r.online.add(us); 
			}

			return true;
		}


		if (arr.length == 1 && arr[0].equals("/bye")) {
			sendMessage(us, "BYE");
			return false;
		}

		//private messages
		if (arr.length >= 3 && arr[0].equals("/priv")) {

			//person exists
			if (!checkName(arr[1])){

				sendMessage(us, "OK");
				String pvtTxt = "";
			
				for (int i = 2; i < arr.length; i++) {
					pvtTxt = pvtTxt + " " + arr[i];
				}
					pvtMsg(us.name, arr[1], pvtTxt);
			}

			else {
				System.out.println("ERROR");
				sendMessage(us, "ERROR");
			}

			return true;
		}


		System.out.println("ERROR");
		sendMessage(us, "ERROR");
		return true;
	}



	//inside
	if (us.status.equals("inside")) {

		Room r = checkRoom(us.currRoom);

		//leave current room
		if (arr.length == 1 && arr[0].equals("/leave")) {
			sendMessage(us, "OK");

			temp = r;

			r.online.remove(us);
			chatEvents(us, temp, "", "leave");

			us.status = "outside";
			us.currRoom = null;

			return true;
		}

		else if (arr.length == 1 && arr[0].equals("/bye")) {
			sendMessage(us, "BYE");

			temp = r;

			//remove from room
			r.online.remove(us);
			chatEvents(us, temp, "", "leave");

			us.currRoom = null;
			
			return false;
		}


		else if (arr.length == 2 && arr[0].equals("/nick")) {

			//new name
			if (checkName(arr[1])) {
				//user the "x has changed name to y"
				chatEvents(us, r, arr[1], "newname");

				us.name = arr[1];
				System.out.println("OK");
				sendMessage(us, "OK");
			}
			//name exists
			else {
				System.out.println("ERROR");
				sendMessage(us, "ERROR");
			}

			return true;
		}


		else if (arr.length == 2 && arr[0].equals("/join")) {

			Room rchange = null;
			rchange = checkRoom(arr[1]);
			
			//room exists
			if (rchange != null) {
				sendMessage(us, "OK");

				temp = r;

				r.online.remove(us);
				chatEvents(us, temp, "", "leave");
				
				chatEvents(us, rchange, "", "join");
				rchange.online.add(us);
				us.currRoom = arr[1];
			}
			//new room
			else {
				rchange = new Room(arr[1]);
				rooms.add(rchange);
				sendMessage(us, "OK");
				
				temp = r;

				//remove from old room
				r.online.remove(us);
				chatEvents(us, temp, "", "leave");
				
				//add to new room
				chatEvents(us, rchange, "", "join");
				rchange.online.add(us);
				us.currRoom = arr[1];
			}

			return true;

		}


		//private messages
		else if (arr.length >= 3 && arr[0].equals("/priv")) {

			//person exists
			if (!checkName(arr[1])) {

				sendMessage(us, "OK");
				String pvtTxt = "";
			
				for (int i = 2; i < arr.length; i++) {
					pvtTxt = pvtTxt + " " + arr[i];
				}
					pvtMsg(us.name, arr[1], pvtTxt);
			}

			else {
				System.out.println("ERROR");
				sendMessage(us, "ERROR");
			}

			return true;
		}

		// escape the '/'' char
		else if(message.charAt(0) == '/'){
			chatEvents(us, r, message.substring(1), "msg");
		}
		else{
			chatEvents(us, r, message, "msg");
		}

		return true;
	}


		return true;

  }


//fim classe
}
