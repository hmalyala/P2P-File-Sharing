# P2P File Sharing

The file owner has a file, and it breaks the file into chunks of 100KB, each stored as a
separate file. The file owner listens on a TCP port. It runs on multiple
threads to serve multiple clients simultaneously.
Each peer connects to the file owner and downloads some chunks. It then
runs two threads of control, one acting as a server that uploads the local chunks to
another peer (referred to as upload neighbor), and the other acting as a client that
downloads chunks from a third peer (referred to as download neighbor). So each peer has
two neighbors, one of which will get chunks from this peer and the other will send
chunks to this peer. 

#Steps to run this program:
1. Start the server through java fileOwner
2. Start the peers through java
