import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
// import org.apache.commons.io.FileUtils;

public class fileOwner {

	private static final int sPort = 8000;   //The server will be listening on this port number
	private static Map<String, String> usPass = new HashMap<String,String>();//createMap();
    static String folderPath = "server-folder";

	

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running."); 
        	ServerSocket listener = new ServerSocket(sPort);
			int clientNum = 1;
			
            File serverFolder = new File(folderPath);
            
			if (!serverFolder.exists()){
                serverFolder.mkdir();
            }
            //ToDo: copy the file from a static folder to serverFolder

            // var source = new File("~/Desktop/test.pdf");
            // var dest = new File("test.pdf");
            // FileUtils.copyFile(source, dest);
            //end of copy code

            //ToDo: split the test.pdf into smaller chunks of size 100kb and store them in 1.chunk, 2.chunk.. format 
            var source = new File("test1.pdf");
            long f_size = source.length();
            int counter = 0;
            int offset = (int) f_size / 15;

            byte[] mybytearray1 = new byte[(int) f_size];
            try {
                InputStream fis = new FileInputStream(source);
                int i = 0;
                    do {

                        byte[] buf = new byte[102400];
                        i = fis.read(buf);
                        System.out.println("read "+i +" bytes");
                        File file = new File(serverFolder + "/chunk_" + counter);
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(buf);
                        counter++;
                    } while (i != -1);
                fis.close();

            } catch (Exception e) {
                System.out.println("try 2 exception");
                e.printStackTrace();
            }
            //-----------------------------spilt file end----------
            //Waiting for a new connection. Once connected need to get the client number(say 3) and give the client all chunks with filenames from clientNum*(totalNumFiles/5) to (clientNum + 1)*(totalNumFiles/5)
        	try {
            		while(true) {
                		new Handler(listener.accept(),clientNum).start();
						System.out.println("Client "  + clientNum + " is connected!");
						clientNum++;
					}
        	} finally {
				listener.close();
        	} 
 
    	}

	/**
     	* A handler thread class.  Handlers are spawned from the listening
     	* loop and are responsible for dealing with a single client's requests.
     	*/
    	private static class Handler extends Thread {
        	private String message;    //message received from the client
			private String MESSAGE;    //uppercase message send to the client
			private Socket connection;
        	private ObjectInputStream in;	//stream read from the socket
        	private ObjectOutputStream out;    //stream write to the socket
			private int no;		//The index number of the client

			private void newLine(){
				System.out.println("--------------------------------------------");
			}
			
        	public Handler(Socket connection, int no) {
            		this.connection = connection;
	    		this.no = no;
			}
			
		
			private String getFileNames(){
				File folder = new File(folderPath);
				StringBuilder out = new StringBuilder("");
				boolean fileFound = false;
				for (File f: folder.listFiles()){
					fileFound = true;
					out.append(f.getName());
					out.append("\n");
				}
				if (!fileFound){
					out.append("No files found in server. Try again later");
				}
				return out.toString();
			}
		
			private String getFileContent(String fileNmae){
				File folder = new File(folderPath);
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
		
			private String processUpload(String fileDetails){
				String fileName = fileDetails.split("#")[0];
				int buffer_size = Integer.parseInt(fileDetails.split("#")[1]);
				try{
					byte[] buffer = new byte[buffer_size];
					for(int i = 0; i<buffer_size; i++){
						buffer[i] = (byte)in.read();
					}
					FileOutputStream fos = new FileOutputStream(folderPath+"/"+fileName);
					fos.write(buffer);
					fos.close();
				}catch(IOException ie){ 
					System.out.println("error while copy: " + ie.toString());
				}
				return "sent file";
			}
		
			private String processMessage(String msg){
				String[] x = msg.split(":");
				String retMessage = "In Progress";
				if (x.length >= 1){
					switch (x[0]){
                        case "hi":
                            int client_num = Integer.parseInt( x[1] );
                            int totalNumFiles = new File("server-folder").listFiles().length;
                            // getFilesForClient(clientNum);
                            retMessage = "total"+ Integer.toString( totalNumFiles );
                            break;
						// case "usPass":
						// 	if(validateCreds(x[1])){
						// 		retMessage = "you can enter \n\t1. dir \n \t2. get <fileName> \n \t3. upload <fileName> \n\t4. exit";
						// 	}else{
						// 		retMessage = "Invalid Credentials, please contact support team";
						// 	}
						// 	break;
						// case "dir":
						// 	retMessage = getFileNames();
						// 	break;
						case "get":
							if (x.length > 1){
								retMessage = getFileContent(x[1]);
							}else{
								retMessage = "file name not found";
							}
							break;
						case "upload":
							if (x.length > 1){
								retMessage = processUpload(x[1]);
							}else{
								retMessage = "file not found";
							}
							break;
					}
				}
				return retMessage;
            }
            
		

			public void run() {
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
                            System.out.println("Received message: " + message + " from client " + no);

							MESSAGE = processMessage(message);
							if (!MESSAGE.equals("sent file")){
								sendMessage(MESSAGE);
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

			//send a message to the output stream
			public void sendMessage(String msg){
				try{
					out.writeObject(msg);
					out.flush();
					System.out.println("Send message: " + msg + " to Client " + no);
				}
				catch(IOException ioException){
					ioException.printStackTrace();
				}
			}
    }
}
