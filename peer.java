import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class peer {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
    ObjectInputStream in;          //stream read from the socket
    ObjectOutputStream serverOut;         //stream write to the socket
    ObjectInputStream serverIn;          //stream read from the socket

    ObjectOutputStream neighOut;         //stream write to the socket
    ObjectInputStream neighIn;          //stream read from the socket
     
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	Boolean authorised = false;
	String clientPath = "client-folder";
    Boolean connected = false;
    int client_num = 0;
    int total_chunks = 0;
    static Set<String> my_chunks = new HashSet<String>();


	peer(int client_num, int server_port, int clientServePort, int neighborPort) {
        clientPath = clientPath + Integer.toString(client_num);
        File client = new File(clientPath);
        
        this.client_num = client_num;
		if (!client.exists()){
			client.mkdir();
        }

        //create a socket to connect to the server
        try{
            requestSocket = new Socket("127.0.0.1", server_port);
            showOptions("Connected to fileOwner on port: " + server_port);
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
        
        try {
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());
            sendMessage("hi:"+ Integer.toString(client_num), out);
            MESSAGE = (String)in.readObject();

            if (MESSAGE.contains("total")){
                this.total_chunks = Integer.parseInt(MESSAGE.split("total")[1]);
                int numFiles = (int)Math.round(Math.ceil(this.total_chunks/5.0)); 
                int end = (client_num+1)*numFiles;
                for (int start = client_num*numFiles; start < end; start ++){
                    if (start < this.total_chunks){
                        //query for the file name
                        try{
                            queryForAChunkAndCreateIt("get:chunk_"+start, in , out);//("get:chunk_"+start);
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
        
        Thread peerServerThread = new Thread(){
            public void run(){
                startServer(clientServePort);
            }
        };
        peerServerThread.start();

        Thread connectToNeighborThread = new Thread(){
            public void run(){
                connectToNeighbor(neighborPort);
            }
        };
        connectToNeighborThread.start();
        
    }
    
    private void queryForAChunkAndCreateIt(String message, ObjectInputStream in, ObjectOutputStream out){
        try{
            String fileName = message.split(":")[1];
					
            sendMessage(message, out);

            boolean fileFound = false;
            Object fileDetails = in.readObject();
            int buffer_size = 0;
            // System.out.println(">>>>>>>>>>>>"+fileDetails);
            try{
                buffer_size = (int)fileDetails;
                fileFound = true;
            }catch(Exception e){
                e.printStackTrace();
            }

            if (fileFound){
                File newFIle = new File(clientPath+"/"+fileName);
                my_chunks.add(fileName);
                if (!newFIle.exists()){
                    newFIle.createNewFile();
                }
                // newLine();
                // System.out.println("Download starting of file: "+fileName+" and it is of size: " + buffer_size + " bytes");
                byte[] buffer = new byte[buffer_size];
                for(int i = 0; i<buffer_size; i++){
                    buffer[i] = (byte)in.read();
                }

                try{
                    FileOutputStream fos = new FileOutputStream(clientPath+"/"+fileName);
                    fos.write(buffer);
                    fos.close();
                    System.out.println("File " + fileName + " downloaded successfully");
                }catch(IOException ie){
                    System.out.println("error while copy: " + ie.toString());
                }
            }else{
                //handling error for no file on server
                newLine();
                System.out.println("NO FILE ON NEIGHBOR/SERVER");
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

	private void newLine(){
		System.out.println("--------------------------------------------");
	}

	private void showOptions(String x){
		newLine();
		System.out.println(x);
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

    private String getFileContent(String fileNmae, ObjectInputStream in, ObjectOutputStream out){
        File folder = new File(clientPath);
        boolean fileFound = false;
        for (File myFile: folder.listFiles()){
            if (myFile.getName().equals(fileNmae)){
                fileFound = true;
                byte [] mybytearray  = new byte [(int)myFile.length()];
                try{
                    InputStream fis = new FileInputStream(myFile);
                    try{
                        fis.read(mybytearray);
                        System.out.println("AS A SERVER: Sending " + myFile.getName() + "(" + mybytearray.length + " bytes) to my neighbor");
                        fis.close();
                        out.writeObject(mybytearray.length);
                        out.flush();
                        
                        out.write(mybytearray);
                        out.flush();
                    }catch(IOException ie){
                        System.out.println("AS A SERVER: IO error");
                    }
                }catch(FileNotFoundException tne){
                    System.out.print("AS A SERVER: error file not found-should not be printed ever");
                }
                break;
            }
        }
        if (!fileFound){
            sendMessage("requested file: " + fileNmae + " not found in server.", out);
        }
        newLine();
        return "sent file";
    }


    String processMessage(String message, ObjectInputStream in, ObjectOutputStream out){
        if (message.equals("giveMeListChunks")){
            return my_chunks.toString();
        }else if (message.contains("get:")){
            String filename = message.split("get:")[1];
            return getFileContent(filename, in , out);
        }else{
            return "unsupported operation";
        }
    }
    
    // starte the server of client code
    void startServer(int servePort){
        
        try {
            ServerSocket listener = new ServerSocket(servePort);
            showOptions("The client " + client_num+ " is serving on " + servePort);
            while(true) {
                
                Socket connection = listener.accept();
                //initialize Input and Output streams
                try{
					//initialize Input and Output streams
					serverOut = new ObjectOutputStream(connection.getOutputStream());
					serverOut.flush();
					serverIn = new ObjectInputStream(connection.getInputStream());
					try{
						while(true)
						{
							String message = (String)serverIn.readObject();
							//show the message to the user
                            // System.out.println("Received message: " + message);
							
                            String MESSAGE = processMessage(message, serverIn, serverOut);
							if (!(MESSAGE.equals("sent file") || MESSAGE.equals("unsupported operation"))){
								sendMessage(MESSAGE, serverOut);
							}
                            
	
						}
					}
					catch(ClassNotFoundException classnot){
							System.err.println("Data received in unknown format");
						}
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client ");
				}
				finally{
					//Close connections
					try{
						serverIn.close();
						serverOut.close();
						connection.close();
					}
					catch(IOException ioException){
						System.out.println("Disconnect with Client ");
					}
				}
			
            }
        } catch(Exception ioException){
            System.out.println("Disconnect with Client ");
        }finally {
            // listener.close();
        } 

        
    }

    void connectToNeighbor(int neighborPort){
        // clientPath = clientPath + Integer.toString(client_num);
        // File client = new File(clientPath);
        
        // this.client_num = client_num;
		// if (!client.exists()){
		// 	client.mkdir();
        // }

        //create a socket to connect to the server
        boolean connected = false;

        while (!connected){
            try{
                requestSocket = new Socket("127.0.0.1", neighborPort);
                System.out.println("---------Connected to neighbor in port "+neighborPort+"---------------------------");
                connected = true;
            }catch (ConnectException e) {
                // showOptions("Connection refused. You need to initiate a server first. Please try again in the format: java peer <server port> <client port> <neighbour port>");
            } 
            catch(UnknownHostException unknownHost){
                showOptions("You are trying to connect to an unknown host. Please try again in the format: java peer <server port> <client port> <neighbour port>");
            }
            catch (Exception e){
                showOptions("Please enter the connection request in the format: java peer <server port> <client port> <neighbour port>");
            }
        }
        

        //connected to neighbor and querying for neighbors chunks and then file contents
        
        try {
            neighOut = new ObjectOutputStream(requestSocket.getOutputStream());
            neighOut.flush();
            neighIn = new ObjectInputStream(requestSocket.getInputStream());
            
            while (my_chunks.size() < total_chunks){
                showOptions("ASKING again after waiting for 1 sec");
                sendMessage("giveMeListChunks", neighOut);
                Object obj = neighIn.readObject();
                // System.out.println(obj+">>>>>>>>>>>>>>>>>>>");
                MESSAGE = (String)obj;
                
                //compare own my_chunks with the returned array and then call the below recursively for all files required
                // teq_files= ["chunk_0", "chunk_1"]
                List<String> chunks = Arrays.asList(MESSAGE.substring(1, MESSAGE.length() - 1).split(", "));
                showOptions("ASKING neighbor for chunks and it returned chunk list of size: "+chunks.size() + " -- Comparing with own my_chunks of length: " + my_chunks.size());
                
                for (String chunk: chunks){
                    if (!my_chunks.contains(chunk)){
                        queryForAChunkAndCreateIt("get:"+chunk, neighIn ,neighOut);
                        // Thread.sleep(1000);
                    }
                }
                Thread.sleep(1000);
            }
            //all chunks received and logic to reconstruct test.pdf
            showOptions("             all chunks received and hence combining them to a single file            ");
            int counter = 0;

		File f = new File(clientPath + "/outputPdf");
		// f.createNewFile();
		FileOutputStream fileOutput = new FileOutputStream(f);
		while (counter < total_chunks) {
			try {
                File chunk_file = new File(clientPath +"/chunk_" + counter);
                
				InputStream fis = new FileInputStream(chunk_file);
				byte[] mybytearray;
				try {
					mybytearray = new byte[(int) chunk_file.length()];
					fis.read(mybytearray);
					fileOutput.write(mybytearray);
					fis.close();
					fileOutput.flush();
					fis = null;
					chunk_file = null;

				} catch (Exception e) {
					System.out.println("try 2 exception");
				}

			} catch (Exception e) {
				System.out.println("try 1 exception");
				e.printStackTrace();
			}

			counter++;
		}
		fileOutput.close();
            
            
            
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
        }catch(Exception e){
            e.printStackTrace();
        }
           
    }

	//main method
	public static void main(String args[])
	{
        
        for (String arg: args){
            System.out.println(arg.toString());
        }
        if (args.length != 4){
            System.out.println("initialsizing format is java peer <client_num> <server port> <client port> <neighbour port>");
        }else{

            // peer client = new peer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            peer client = new peer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));

            // client.run();
            

            //start two threads one for listening at a given port
            // client.startServer(8001);
            // and one for serving at another port
            // connectToNeighbor(8002);
        }
       
    }
    
    

}
