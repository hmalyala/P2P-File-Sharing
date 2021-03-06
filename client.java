import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class ftpClient {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	Boolean authorised = false;
	String clientPath = "client-folder";
	Boolean connected = false;

	ftpClient() {
		File client = new File(clientPath);
		if (!client.exists()){
			client.mkdir();
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

	void run()
	{
		try{
			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			String line = "";
			String getInputQuery = "";
			
			showOptions("Client spawned and waiting for first command...");

			while (!connected){
				line = bufferedReader.readLine();
				if (line.startsWith("ftpClient")){
					String[] ipToken = line.split(" ");
					if (ipToken.length != 3){
						showOptions("Please enter the connection request in the format: ftpClient <IP> <port>");
					}
					try{
						String ip = ipToken[1];
						int port = Integer.parseInt(ipToken[2]);
						//create a socket to connect to the server
						requestSocket = new Socket(ip, port);
						System.out.println("Connected to localhost in port 8000");
						connected = true;
					}catch (ConnectException e) {
						showOptions("Connection refused. You need to initiate a server first. Please try again in the format: ftpClient <IP> <port>");
					} 
					catch(UnknownHostException unknownHost){
						showOptions("You are trying to connect to an unknown host. Please try again in the format: ftpClient <IP> <port>");
					}
					catch (Exception e){
						showOptions("Please enter the connection request in the format: ftpClient <IP> <port>");
					}
					
				}else{
					showOptions("Please enter the connection request in the format: ftpClient <IP> <port>");
				}
			}
			//create a socket to connect to the server
			// requestSocket = new Socket("localhost", 8000);
			// System.out.println("Connected to localhost in port 8000");
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			

			//-------------authentication code start----------------------------------
			while (!authorised){
				System.out.print("Please enter your username: ");
				String userName = bufferedReader.readLine();
				System.out.print("Enter your password: ");
				String pwd = bufferedReader.readLine();
				sendMessage("usPass:"+userName+"/"+pwd);
				MESSAGE = (String)in.readObject();
				if (!MESSAGE.contains("Invalid")){
					authorised = true;
					getInputQuery = MESSAGE;
				}
				System.out.println(MESSAGE);
			}

			//-------------read each line start----------------------------------

			line = bufferedReader.readLine();

			while(!line.equalsIgnoreCase("exit"))
			{
				String message = processMessageBeforeSend(line);
				if (message.contains("get")){
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
						showOptions(getInputQuery);
					}else{
						//handling error for no file on server
						newLine();
						System.out.println((String)fileDetails);
						showOptions(getInputQuery);
					}
					
				}else if(message.contains("upload")){
					String fileName = message.split(":")[1];
					File newFIle = new File(clientPath+"/"+fileName);
					newLine();
					if (!newFIle.exists()){
						System.out.println("File you are trying to upload does not exist. please try again");
						showOptions(getInputQuery);
					}else{
						byte [] mybytearray  = new byte [(int)newFIle.length()];
						try{
							InputStream fis = new FileInputStream(newFIle);
							try{
								
								fis.read(mybytearray);
								System.out.println("Sending file: " + newFIle.getName() + " of size - " + mybytearray.length + " bytes");
								out.writeObject("upload:"+fileName+"#"+mybytearray.length);
								out.flush();

								fis.close();
								out.write(mybytearray);
								out.flush();
								System.out.println("File sent to server for upload");
								showOptions(getInputQuery);
								// bis.close();
							}catch(IOException ie){
								System.out.println("Error: while uploading the file to Server: " + ie.getLocalizedMessage());
							}
						}catch(FileNotFoundException tne){
							System.out.print("error file not found-should not be printed ever");
						}
					}
					

				}else if (!message.startsWith("Error")){
					sendMessage(message);
					MESSAGE = (String)in.readObject();
					newLine();
					System.out.println(MESSAGE);
					showOptions(getInputQuery);
				}else{
					System.out.println("Invalid Entry entered, Please try again \n" + getInputQuery);
				}
				line = bufferedReader.readLine();
			}
		}
		catch (ConnectException e) {
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
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
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
	//main method
	public static void main(String args[])
	{
		ftpClient client = new ftpClient();
		client.run();
	}

}
