import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class peer {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	Boolean authorised = false;
	String clientPath = "client-folder";
    Boolean connected = false;
    int client_num = 0;
    int total_chunks = 0;
    Set<String> my_chunks = new HashSet<String>();


	peer(int client_num, int server_port) {
        clientPath = clientPath + Integer.toString(client_num);
        File client = new File(clientPath);
        
        this.client_num = client_num;
		if (!client.exists()){
			client.mkdir();
        }

        //create a socket to connect to the server
        try{
            requestSocket = new Socket("127.0.0.1", server_port);
            System.out.println("Connected to localhost in port 8000");
            connected = true;
        }catch (ConnectException e) {
            showOptions("Connection refused. You need to initiate a server first. Please try again in the format: java peer <server port> <client port> <neighbour port>");
        } 
        catch(UnknownHostException unknownHost){
            showOptions("You are trying to connect to an unknown host. Please try again in the format: java peer <server port> <client port> <neighbour port>");
        }
        catch (Exception e){
            showOptions("Please enter the connection request in the format: java peer <server port> <client port> <neighbour port>");
        }

        //get the files corresponding to client_num from server socket
        
        System.out.println("connected to server to download files");
        
        try {
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());
            sendMessage("hi:"+ Integer.toString(client_num));
            MESSAGE = (String)in.readObject();

            if (MESSAGE.contains("total")){
                this.total_chunks = Integer.parseInt(MESSAGE.split("total")[1]);
                int numFiles = (int)Math.round(Math.ceil(this.total_chunks/5.0)); 
                int end = (client_num+1)*numFiles;
                for (int start = client_num*numFiles; start < end; start ++){
                    if (start < this.total_chunks){
                        //query for the file name
                        try{
                            queryForAChunkAndCreateIt("get:chunk_"+start);//("get:chunk_"+start);
                            // Thread.sleep(100);
                        }catch(Exception e){
                            System.out.println("adsf");
                        }
                    
                    }
                    
                }
            }
            System.out.println(MESSAGE);
            
        } catch (ConnectException e) {
                System.err.println("Connection refused. You need to initiate a server first.");
        } 
        catch ( ClassNotFoundException e ) {
                    System.err.println("Class not found");
            } 
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        
        
    }
    
    private void queryForAChunkAndCreateIt(String message){
        try{
            String fileName = message.split(":")[1];
					
            sendMessage(message);

            boolean fileFound = false;
            Object fileDetails = in.readObject();
            int buffer_size = 0;
            try{
                buffer_size = (int)fileDetails;
                fileFound = true;
            }catch(Exception e){

            }

            if (fileFound){
                File newFIle = new File(clientPath+"/"+fileName);
                my_chunks.add(fileName);
                if (!newFIle.exists()){
                    newFIle.createNewFile();
                }
                newLine();
                System.out.println("Download starting of file: "+fileName+" and it is of size: " + buffer_size + " bytes");
                byte[] buffer = new byte[buffer_size];
                for(int i = 0; i<buffer_size; i++){
                    buffer[i] = (byte)in.read();
                }

                try{
                    FileOutputStream fos = new FileOutputStream(clientPath+"/"+fileName);
                    fos.write(buffer);
                    fos.close();
                    System.out.println("File downloaded successfully");
                }catch(IOException ie){
                    System.out.println("error while copy: " + ie.toString());
                }
            }else{
                //handling error for no file on server
                newLine();
                System.out.println((String)fileDetails);
            }
        }catch (ConnectException e) {
                System.err.println("Connection refused. You need to initiate a server first.");
        } 
        catch ( ClassNotFoundException e ) {
                    System.err.println("Class not found");
            } 
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        
    }

	private String processMessageBeforeSend(String line){
		if (line.startsWith("dir")){
			return "dir:";
		}else if(line.startsWith("get") && line.split("get ").length > 1){
			String fileName = line.split("get ")[1];
			return "get:"+fileName;
		}else if(line.startsWith("upload") && line.split("upload ").length > 1){
			String fileName = line.split("upload ")[1];
			return "upload:"+fileName;
		}
		return "Error: invalid entry";
	}

	private void newLine(){
		System.out.println("--------------------------------------------");
	}

	private void showOptions(String x){
		newLine();
		System.out.println(x);
	}

	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
    }

    void sendMessage(String msg, ObjectOutputStream out)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
    }
    
    private String getFileContent(String fileNmae){
        File folder = new File(clientPath);
        boolean fileFound = false;
        newLine();
        for (File myFile: folder.listFiles()){
            if (myFile.getName().equals(fileNmae)){
                fileFound = true;
                byte [] mybytearray  = new byte [(int)myFile.length()];
                try{
                    InputStream fis = new FileInputStream(myFile);
                    try{
                        fis.read(mybytearray);
                        System.out.println("Sending " + myFile.getName() + "(" + mybytearray.length + " bytes)");
                        fis.close();
                        out.writeObject(mybytearray.length);
                        out.flush();
                        
                        System.out.println("sending buffer_size: " + mybytearray.length);

                        out.write(mybytearray);
                        out.flush();
                    }catch(IOException ie){
                        System.out.println("IO error");
                    }
                }catch(FileNotFoundException tne){
                    System.out.print("error file not found-should not be printed ever");
                }
                break;
            }
        }
        if (!fileFound){
            sendMessage("requested file: " + fileNmae + " not found in server.");
        }
        newLine();
        return "sent file";
    }


    String processMessage(String msg){
        if (message.equals("giveMeListChunks")){
            return my_chunks.toString();
        }else if (message.contains("get:")){
            String filename = message.split("get:")[1];
            return getFileContent(filename);
        }else{
            return "unsupported operation";
        }
    }
    
    // starte the server of client code
    void startServer(int servePort){
        ServerSocket listener = new ServerSocket(servePort);
        System.out.println("The client " + client_num+ " is serving on " + servePort);
        try {
            while(true) {
                
                Socket connection = listener.accept();
                //initialize Input and Output streams
                try{
					//initialize Input and Output streams
					out = new ObjectOutputStream(connection.getOutputStream());
					out.flush();
					in = new ObjectInputStream(connection.getInputStream());
					try{
						while(true)
						{
							String message = (String)in.readObject();
							//show the message to the user
                            System.out.println("Received message: " + message);
							
                            String MESSAGE = processMessage(message);
							if (!MESSAGE.equals("sent file") || !MESSAGE.equals("unsupported operation")){
								sendMessage(MESSAGE, out);
							}
                            
	
						}
					}
					catch(ClassNotFoundException classnot){
							System.err.println("Data received in unknown format");
						}
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + no);
				}
				finally{
					//Close connections
					try{
						in.close();
						out.close();
						connection.close();
					}
					catch(IOException ioException){
						System.out.println("Disconnect with Client " + no);
					}
				}
			
            }
        } finally {
            listener.close();
        } 

        
    }

    

	//main method
	public static void main(String args[])
	{
        
        for (String arg: args){
            System.out.println(arg.toString());
        }
        if (args.length < -1){
            System.out.println("initialsizing format is java peer <client_num> <server port> <client port> <neighbour port>");
        }else{

            // peer client = new peer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            peer client = new peer(0, 8000);

            // client.run();

            //start two threads one for listening at a given port
            client.startServer(8001);
            // and one for serving at another port
            // connectToNeighbor(8002);
        }
       
    }
    
    

}
