/*
 *   NodeIF: Node InterFace, a software for tinyOS(c) to command telosB-family nodes 
 *   as radio interfaces from one Java process.
 *
 *   Copyright (C) 2014  M. Onur Ergin - monurergin@gmail.com
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program, in the file <license-gpl-3.0.txt>.  
 *   If not, see <http://www.gnu.org/licenses/>.
 *   
 */

/*
 * Onur Ergin
 */


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Calendar;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import sun.misc.*;
import java.util.Scanner;

import net.tinyos.message.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;


public class NodeIf implements MessageListener, SignalHandler {


  private MoteIF moteIF;
  private static final int APPID = 132;
  private static File outfile;
  private static FileOutputStream outputstream, logstream;
  static Signal sig = new Signal("INT");
  private static boolean fileused = false;
  

  private static final short chn_offset = 10;
  private static final short num_channels = 16;
  private static final short response_delay = 40; //ms
  private static short txpower = 31;
  private static short[] TxPowers;
  private static short channel = chn_offset + 1 + num_channels;
  private static short[] Channels;
  private static int nodeID = 32767;
  private static int[] IDs;
  private static int num_nodes;
  private static ArrayList<Measurement> Experiment; //from Measurement.java

  //private static Calendar cal;
  
  public static final short  SETTX = 0 , GETTX = 1, SETFREQ = 2, GETFREQ = 3, DATA = 4, IDQUERY = 5;  
  public static final int RSS_SET = 40;	// Number of experiment iterations
  public static final int DATA_SET = RSS_SET * num_channels;
  
  public static final short FAIL = -1, SUCCESS = 255;
  
  private static Scanner in;
  
  public NodeIf(MoteIF moteIF) {
    this.moteIF = moteIF;
    this.moteIF.registerListener(new NodeIfMsg(), this);
    Signal.handle(sig, this);
  }
    
  public void messageReceived(int to, Message message) {
    NodeIfMsg msg = (NodeIfMsg) message;
    if (msg.get_appid() != APPID) { System.out.println("Wrong packet"); return; }	// Wrong packet
    
    this.nodeID = msg.get_nodeid();	//set the node id.
    
    int source = message.getSerialPacket().get_header_src();
    String str= "";
    //System.out.println("node: \tcounter: \tr_rssi: \ts_rssi: \tchannel: \tpower");
    //String str = msg.get_r_nodeid() + " | " + msg.get_counter() + " | " + msg.get_r_rssi()  + " | " + msg.get_s_rssi() + " | " +  msg.get_channel()  + " | " + msg.get_txpower();
    //System.out.println(str);
    //System.out.println("Message received from node:" + msg.get_nodeid() + " source=" + source);
    //System.out.println(msg.toString());
    /*System.out.println(this.toString() + " :Msg Received. Node: " + msg.get_nodeid() + "(" + source + ")" +
    					" Command: " + msg.get_pcktype() + " Value: " + msg.get_value() + 
    					((msg.get_pcktype() == DATA) ? (" Sender:" + msg.get_senderid() +  
    					 " Receiver: " + msg.get_receiverid() + "\n") : "\n")
    				   );
    */
    
    //System.out.println("Moteif Source index: " + findMoteIFix(MoteIFs, this.moteIF.getSource());
    this.nodeID = msg.get_nodeid();
    
    int nodeix = findIxFromId(IDs, (int) msg.get_nodeid(), num_nodes);
    
    switch (msg.get_pcktype()) {
    	case GETTX:
    		TxPowers[nodeix] = (short) msg.get_value();
    		txpower = (short) msg.get_value();
    		str = "TxPowers[" + nodeix + "] = " + TxPowers[nodeix];
    		break;
    	case SETTX:
    		str = "ACK: TxPower of node " + msg.get_nodeid() + " is set to " + msg.get_value();
    		break;
    	case GETFREQ:
    		Channels[nodeix] = (short) msg.get_value();
    		channel = (short) msg.get_value(); 
    		str = "Channels["+nodeix+"] = " + Channels[nodeix];
    		break;
    	case SETFREQ:
    		str = "ACK: Channel of node " + msg.get_nodeid() + " is set to " + msg.get_value();
    		break;
    	case DATA:
    		processReceivedData(msg);	
		
		//str = "Data: (" + msg.get_nodeid() + ") " + msg.get_senderid() + "--(" + msg.get_value() + ")->" + msg.get_receiverid();
    		//System.out.println(msg.toString());
    		//cal = Calendar.getInstance();
		
    		break;
    	case IDQUERY:
	    	IDs[msg.get_value()] = msg.get_nodeid(); 
	    	//System.out.println("Received ID at:" +System.currentTimeMillis());
	    	str = "IDs[" + msg.get_value() + "] = " + msg.get_nodeid() + " This: NodeID = " + nodeID;
	    	//System.out.println(msg.toString());
	    	//for (int i=0; i<IDs.length; i++) { System.out.println("--> IDs["+i+"] = " + IDs[i]); }	    	
	    	break;
	    default:
	    	break;
    }
        
    
    
    //say(str);
    
	
  }
  
  private static void usage(int len) {
    System.err.println(len + " args. usage: NodeIf [-comm <source>] [-file <output filename>]");
  }
  
  public void handle(Signal sig) {
    System.out.println("SIGNAL " + sig + " is captured");
    if (fileused)
    {
    	try {
    		outputstream.flush();
    		outputstream.close();
    	} catch (IOException e) {System.out.println("Error closing file " + outputstream.toString());};
    }
    System.exit(0);            
 }
  
  public static void main(String[] args) throws Exception {
    String source = null;
    ArrayList<String> sources = new ArrayList<String>();
    ArrayList<PhoenixSource> phoenices = new ArrayList<PhoenixSource>();
    ArrayList<MoteIF> MoteIFs = new ArrayList<MoteIF>();
    ArrayList<NodeIf> NodeIfs = new ArrayList<NodeIf>(); 
    Experiment = new ArrayList<Measurement>();
     
    String License = "NodeIf  Copyright (C) 2014 M. Onur Ergin\n" + 
			"This program comes with ABSOLUTELY NO WARRANTY;\n" + 
                        "for details refer to file: license-gpl-3.0.txt in the root directory.\n" +
			"This is free software, and you are welcome to redistribute it\n"+
			"under GPL conditions.\n\n";
    System.out.println(License);
    
    String Usage = "Usage:\n" +
                   "java NodeIf -nonode\t => reserved for testing\n" +
                   "java NodeIf -direct \t => nodes.txt must contain output of \'motelist -c\'\n" +
                   "java NodeIf \t\t => nodes.txt must contain space-separated IDs of Twist nodes\n"+
                   "If Twist IDs are used, first run \'sh connect\' (remember to put password in 'tunnel.exp' file)\n" +
                   "Note: nodes.txt must NOT contain an empty line.\n\n";
    System.out.println(Usage);

    if (args.length == 2 && args[0].equals("-file")){
    	System.out.println("Outputing into file: " + args[1]);
			   outfile = new File (args[1]);
		try {
			 boolean success = outfile.createNewFile();
			 if (!success)
			 {
			   System.err.println("File " + args[2] + " already exists!");
			   System.exit(1);
			 }
			 outputstream = new FileOutputStream(outfile);
			
			 fileused = true;
    	} catch (IOException e) {}
    }
    
    if (args.length == 2 && false) {
      if (!args[0].equals("-comm")) {
			usage(args.length);
			System.exit(1);
      }
      source = args[1];
	} else if (args.length == 1 && args[0].equals("-nonode")) say("NO NODE MODE!");   
    else {	// Read ports from file
    	File file = new File("./nodes.txt");
		FileReader frd = null;
		String line, tokens[];
		String ports="";
		int portnum;
		BufferedReader brd = null;
		
		try {
			  frd = new FileReader(file);
					  
			  brd = new BufferedReader(frd);
			 
			  if (args.length == 1 && args[0].equals("-direct")) {
				  while ((line = brd.readLine()) != null) {
						tokens = line.split(",");
						//for (int ii = 0; ii<tokens.length; ii++) 
							//ports = tokens[ii];
						ports = tokens[1];
						source = "serial@" + ports + ":tmote"; // For directly plugged in nodes
						//source = "sf@" + "localhost:" + ports;	// For serial forwarder connected nodes
						say(source);
						sources.add(source); 
						//System.out.println(source);				
					  }
			  } else {
				  Scanner sc = new Scanner(file);
				  sc.useDelimiter(" ");
				  while(sc.hasNext()){
				  	ports = sc.next().trim();
				  	try {
                                             portnum = Integer.parseInt(ports) + 9000;
					     ports = Integer.toString(portnum);
				  	} catch (Exception e) {
						System.out.println("Error parsing nodes.txt");
						System.exit(0);
					}
				  	source = "sf@" + "localhost:" + ports;	// For serial forwarder connected nodes
				  	say(source);
					sources.add(source); 
				  }
			  }
			  if (sources.isEmpty())
			  {
			  	System.out.println("No Source! not quitting.");
				//System.exit(1);
			  }
			  System.out.println(sources.toString() + " Total: " + sources.size() + " nodes ");
			  
		}
		catch (FileNotFoundException e) {
		  e.printStackTrace();
		  System.out.println("Source file not found");
		  //System.exit(1);
		} catch (IOException e) {
		  e.printStackTrace();
		  System.out.println("FileIO exception");
		  //System.exit(1);
		}
    }
    PhoenixSource phoenix;
    MoteIF mif;
    NodeIf serial;
    	
    for (int i = 0; i < sources.size(); i++){
    	source = sources.get(i);
		if (source == null) {
		  phoenix = BuildSource.makePhoenix(PrintStreamMessenger.err);
		}
		else {
		  phoenix = BuildSource.makePhoenix(source, PrintStreamMessenger.err);
		}
	
		mif = new MoteIF(phoenix);
		MoteIFs.add(mif);
		serial = new NodeIf(mif);
		NodeIfs.add(serial);
	} //for
    
    num_nodes = sources.size();
    TxPowers = new short[sources.size()];
	Channels = new short[sources.size()];
	IDs = new int[sources.size()];
	for (int k=0; k < sources.size(); k++) {
		IDs[k] = -1;
		TxPowers[k] = -1;
		Channels[k] = -1;
	}
	
	updateIDs(NodeIfs);
 

    in = new Scanner(System.in);
    readCmd(NodeIfs);
    
  }
  
  public static void readCmd(ArrayList<NodeIf> nodes) {
  
  	NodeIfMsg tMsg = new NodeIfMsg();
  	int n, cmd, val;
  	//Scanner in = new Scanner(System.in);
  	tMsg.set_appid(APPID);
  	while(true){
		System.out.println("Enter: Node Command [Value] ");
		System.out.println("COMMANDS "+GETTX+":GETTX "+GETFREQ+":GETFREQ "+
							SETTX+":SETTX "+SETFREQ+":SETFREQ "+DATA+":DATA " +
							IDQUERY+":IDQUERY " + " 100:START " + "200:UPDATEIDs "							
							);
		n=-1;
		try {
			n = in.nextInt();
		} catch(Exception e){
			e.printStackTrace();
			say("No more input possible, exiting."); System.exit(-1);
		}
		
		if(n==-1) System.exit(0);
		else
		 if (n == 200) {		
			updateIDs(nodes);
			continue;
		} 		
		else if (n == 100) { 
			perform(nodes);
			continue;
		} 
		else if (n >= nodes.size())
		{
			System.out.println("Node doesn't exist!");
			continue;
		}
		
		cmd = in.nextInt();
		
		
		switch(cmd) {
			case GETTX:	// Read Power
				System.out.println("\nQuerying Tx Power: ");
				nodes.get(n).getNodeTxPower();
				break;
			case GETFREQ:	// Read Freq
				System.out.println("\nQuerying Tx Power: ");
				nodes.get(n).getNodeChannel();
				break;
			case SETTX:	// Set Power
				val = in.nextInt();
				nodes.get(n).setNodetxPower(val);
				break;
			case SETFREQ:	// Set Freq
				val = in.nextInt();
				nodes.get(n).setNodeChannel(val);
				break;
			case DATA: // Send data
				val = in.nextInt();
				System.out.println("Sending data " +val+ " to IDs["+n+"]="+IDs[n]);
				nodes.get(n).sendNodeData(val);
				break;			
			case IDQUERY:
				System.out.println("ID Querying node " +n+ " while IDs["+n+"]="+IDs[n]);
				nodes.get(n).sendNodeIdQuery(n);
			default:
				break;
		}
	} //while
  
  }// readCmd()
  
  private static void updateIDs(ArrayList<NodeIf> nodes) {
	  int oldId, val;
	  
	  for (int i=0; i < nodes.size(); i++) {
			
		  oldId = IDs[i];
		  
		  nodes.get(i).sendNodeIdQuery(i);
			
			val = 5;
			while (IDs[i] == oldId && val-- > 0) {	// wait until node in turn is updated but only val times					
				try { 
					Thread.sleep(response_delay); // response expected in ~15ms
				}
				catch (Exception e) {;}
			}
			if (val == 0) System.out.println("ID Response is not coming from " +i);
		}
		
		//for (int j=0; j < nodes.size(); j++)
		//	System.out.println("the ID["+j+"]=["+IDs[j]+"]" + "\tthe node.ID["+j+"]=["+nodes.get(j).IDs[j]+"]");
					
		System.out.println("IDs updated.");
  }
  
  private static void updateTxPowers(ArrayList<NodeIf> nodes, short tPower) {
	  short oldTx, val;
	  
	  for (int i=0; i < nodes.size(); i++) {
		  
		  oldTx = TxPowers[i];
		  
		  nodes.get(i).setNodetxPower(tPower);
		  
		  val = 5;
			while (TxPowers[i] == oldTx && val-- > 0) {	// wait until node in turn is updated but only val times					
				try { 
					Thread.sleep(response_delay); // response expected in ~15ms
					}
					catch (Exception e) {;}
			}
			if (val == 0) System.out.println("Txpower couldn't set for node: " +i);
	  }
  }

  private static void updateChannels(ArrayList<NodeIf> nodes, short tChannel) {
      short oldCh, val;
      
	  for (int i=0; i < nodes.size(); i++) {
		  
		  oldCh = Channels[i];
		  
		  nodes.get(i).setNodeChannel(tChannel);
		  
		  val = 5;
			while (Channels[i] == oldCh && val-- > 0) {	// wait until node in turn is updated but only val times					
				try { 
					Thread.sleep(response_delay); // response expected in ~15ms
					}
					catch (Exception e) {;}
			}
			if (val == 0) System.out.println("Channel couldn't set for node: " +i);
	  }
  }
  
  private static int findIxFromId(int[] arrID, int id, int size) {
	  int pos = FAIL;
	  for (int i=0; i < size; i++)
		  if (arrID[i] == id) {
			  pos = i;
			  break;
		  }
	  return pos;
  }
  
  private static void sendNodeMsg (NodeIf node, NodeIfMsg tMsg) {
	  tMsg.set_appid(APPID);
	  
	  //System.out.println("Sending to node " + node.nodeID);
	  try{
			node.moteIF.send(MoteIF.TOS_BCAST_ADDR,tMsg);
		}
		catch (IOException e) {
			System.out.println("Cannot send message to node" + node.nodeID);
	    }
  }
  
  private void getNodeTxPower () {
	  NodeIfMsg tMsg = new NodeIfMsg();
	  tMsg.set_pcktype(GETTX);	  
	  sendNodeMsg(this, tMsg);
  }
  
  private void getNodeChannel () {
	  NodeIfMsg tMsg = new NodeIfMsg();
	  tMsg.set_pcktype(GETFREQ);	  
	  sendNodeMsg(this, tMsg);
  }
  
  private void setNodeChannel (int nChannel) {
	  NodeIfMsg tMsg = new NodeIfMsg();
	  int val = nChannel + chn_offset;
	  tMsg.set_pcktype(SETFREQ);
	  tMsg.set_value(val);
	  
	  System.out.println("Set Node: " + this.nodeID + " channel to " + val + "(" + nChannel + ")");
	  
	  sendNodeMsg(this, tMsg);	  
  }
  
  private void setNodetxPower (int nTxPower) {
	  NodeIfMsg tMsg = new NodeIfMsg();
	  int val = nTxPower;
	  tMsg.set_pcktype(SETTX);
	  tMsg.set_value(val);
	  
	  System.out.println("Set Node: " + this.nodeID + " TxPower to " + val + "(" + nTxPower + ")");
	  
	  sendNodeMsg(this, tMsg);	  
  }
  
  private  void sendNodeData (int data) {
	  NodeIfMsg tMsg = new NodeIfMsg();
	  int val = data;
	  tMsg.set_pcktype(DATA);
	  tMsg.set_value(val);
	  
	  //System.out.println("Set Node: " + node.nodeID + " TxPower to " + val + "(" + nTxPower + ")");
	  
	  sendNodeMsg(this, tMsg);	  
  }
  
  private void sendNodeIdQuery (int data) {
	  NodeIfMsg tMsg = new NodeIfMsg();
	  int val = data;
	  tMsg.set_pcktype(IDQUERY);
	  tMsg.set_value(val);
	  
	  sendNodeMsg(this, tMsg);	  
  }
  
  public static void say (String tStr) {
  		System.out.println(tStr);
  }
  
  public static int findMoteIFix(ArrayList<MoteIF> moteifs, PhoenixSource source) {
	  
	  for (int i = 0; i < moteifs.size(); i++)
		  if (moteifs.get(i).getSource() == source)
			  return i;
	  
	  return -255;
  }
  
	/* Implement Below */  
  public void processReceivedData (NodeIfMsg msg) {

	long now = System.currentTimeMillis();
	Measurement tMsr = new Measurement();
	tMsr.set(msg.get_receiverid(), msg.get_senderid(), msg.get_channel(), msg.get_rssi(), msg.get_txpower(), now, msg.get_value(), msg.get_lqi());
	Experiment.add(tMsr);
	
	String str = now + "," + msg.get_senderid() + "," + msg.get_receiverid() + "," + msg.get_channel() + "," + msg.get_rssi() + " " + msg.get_value();
	    str = str + '\n';	
	if (fileused)
		  try{	  		
				outputstream.write (str.getBytes());
		  } catch (IOException e) {}
		else ;System.out.print(".");//say("RECEIVE SAYS:"+str);
  }

  public static void perform (ArrayList<NodeIf> nodes) 
  {
	  boolean allSet = false;
	  NodeIfMsg tMsg = new NodeIfMsg();
	  int n, cmd, val, j, d=20;
	  int iterations = 30;
	  short cur_channel = 1;  // 1 through 16
	  int txInterval = 40, //ms
	  	  pktPerCh = 40;//*num_channels;
	  	  //chSweepRounds = 10000,
	  	  //chSweepPeriod = 20000;
	  	  
	  //String filedir = "/lhome/ergin/chOuterLoop4thFl-2ndRow10Nodes/";
	  String filedir = "./output/";
	  String filenamebase = "seq" + num_channels + "ch_";
	  
	  tMsg.set_appid(APPID);

	  for (int round=1; round <= iterations; round++)  // repeat the many rounds
	  {
	  	say("Iteration " + round);
	  	clearExperiment();
	    //for (int i=0; i < nodes.size(); i++){ // each node to transmit
	  
		  	//  say("Node " + nodes.get(i).nodeID + " will transmit" );
		  	for (cur_channel = 1; cur_channel <= num_channels; cur_channel++)  // for each channel
			{
				/* Change all channels */
				say ("Now for channel " + cur_channel);
			 	for (j=0; j < nodes.size(); j++) // set freqs of each node
				{
					nodes.get(j).setNodeChannel(cur_channel);
					say("Setting Node " + nodes.get(j).nodeID + " channel to " + cur_channel);
					try {
							Thread.sleep(10);
						} catch (Exception e) {;}  
				}// set freqs of each node
				  
				val = 10;
				for (j=0; j<nodes.size(); j++)  //Check channel
				{
					while (Channels[j] != cur_channel && val-- > 0) 	// wait until node in turn is updated but only val times
					{
						try { 
								//System.out.println("Sleeping at:" +System.currentTimeMillis());
								Thread.sleep(response_delay); // response expected in ~15ms
								//System.out.println("Resuming at:" +System.currentTimeMillis());
							} catch (Exception e) {;}
					} 
					if (val == 0)  
					{
						say("Channel couldn't set for " +j);
						j--;
					}
				}// for nodes to check channel
							
			    System.out.println("Channels set to: " + cur_channel);			  
				  
				for (int i=0; i < nodes.size(); i++) // each node to transmit
				{ //int i=0;
					say ("\nNode " + IDs[i] + " is transmitting");
					for (int jj = 0; jj < pktPerCh ; jj++) 		// Send Node Data "iteration" times
					{
						nodes.get(i).sendNodeData(jj);
						try { 
								Thread.sleep(txInterval); // 40ms => 25pkt/sec
								//if(jj%(pktPerCh/num_channels)==0) // bu ne, hatirlamiyorum. kapattim.
									//Thread.sleep(10*num_nodes);
							} 	catch (Exception e) {;}
					} //for pktPerCh
					try {
							Thread.sleep(40); //(chSweepPeriod - (pktPerCh*txInterval + num_channels*10));
						} catch (Exception e) {say(e.toString());}
				} // each node inner loop  
				
			} // for each channel
				  
		//} //for nodes
	 
	 	try {
				Thread.sleep(1000); // wait a second to collect remaining messages
			}	catch (Exception e) {e.toString();}
		say ("Iteration " + round + " completed, saving measurements.");
	 	printExperimentToFile( filedir + filenamebase + round + ".txt");
	  
	  }//for rounds
	  
	  /*if (fileused)
			try {
					outputstream.flush();
					outputstream.close();
				} catch (IOException e){say(e.toString());}
	  */
	say ("FINISHED");
	  
  } // perform()
 
  private static void printExperiment() {
	  System.out.println(Experiment.size() + " measurements will be shown.");
	  System.out.println ("Node\tSender\tChan\tRSSI\tTXPOWER");
	  for (int i=0; i < Experiment.size(); i++)
	  try{
		  System.out.println(Experiment.get(i).nodeid + "\t" + Experiment.get(i).senderid + "\t" +
				  			 Experiment.get(i).channel + "\t" + Experiment.get(i).rssi + "\t" +
				  			 Experiment.get(i).txpower + "\t" + Experiment.get(i).lqi + "\t" + Experiment.get(i).packetSequence
				  			 );
		}
		catch(Exception e) {
			System.out.println("Exception caught at i="+i+"\n"+e.toString());
		
		}
		System.out.println(Experiment.size() + " measurements printed.");
  }
  
  private static void clearExperiment() {
	  Experiment.clear();
	  System.out.println ("Experiment data cleared.");
  }
  
  private static void printExperimentToFile(String filename) {
	  
	  //String filename, measurement_dir = "measurements/";
	  
	  //String delimiter = "\t"; use if needed
	  
	  String str;
	  
	  boolean success = false;
	  
		try {			 
			 while (!success)
			 {
			   outfile = new File (filename);
			   success = outfile.createNewFile();
			   
			   if (!success)
				   System.err.println("File " + filename + " already exists!");			   
			 } // while
		} catch (IOException e) { System.out.print("Error: " + e.toString()); return; }
	
	  try{
		  FileOutputStream stream = new FileOutputStream(outfile);
				  
		  for (int i=0; i < Experiment.size(); i++) {
			  str = Experiment.get(i).nodeid + "\t" + Experiment.get(i).senderid + "\t" +
					  			 Experiment.get(i).channel + "\t" + Experiment.get(i).rssi + "\t" +
					  			 Experiment.get(i).txpower + "\t" + Experiment.get(i).timestamp + "\t" + Experiment.get(i).packetSequence + "\t" +  Experiment.get(i).lqi + "\n" ;
					  			 
			  	  		
					stream.write (str.getBytes());
			  
		  }
		  System.out.println(Experiment.size() + " experiments printed to file ");
		  stream.flush();
		  stream.close();
	  } catch (IOException e) {e.toString();}
	  catch (NullPointerException e) {e.toString();}
	  
  } // printExperimentToFile()
  
  private static boolean readExperimentFromFile(String filename) {
  	
  	String str;
	  
	  boolean success = false;
	  
	  Experiment.clear();
	  
	  
	  FileReader frd = null;
	  String line, tokens[];
	  String ports;
	  BufferedReader brd = null;
	  
	  /*System.out.print("Enter File Name: " + measurement_dir);
	   File infile; 
	   filename = in.next();
	   infile = new File (measurement_dir+filename); */
		
		try {
			  frd = new FileReader(filename);	  
			  brd = new BufferedReader(frd);
			 
			  while ((line = brd.readLine()) != null) {
				tokens = line.split("\t");
				
				Measurement tMsr = new Measurement();
				tMsr.set((short)Integer.parseInt(tokens[0]),
						 (short)Integer.parseInt(tokens[1]),
						 (short)Integer.parseInt(tokens[2]),
						 (short)Integer.parseInt(tokens[3]),
						 (short)Integer.parseInt(tokens[4]),
						 (long)Long.parseLong(tokens[5]),
						 (int)Integer.parseInt(tokens[6]),
						 (short)Integer.parseInt(tokens[7]));
						 // 0); // time stampi olmayan dosyalar icin.
 				Experiment.add(tMsr);
				//source = "serial@" + ports + ":tmote"; // For directly plugged in nodes
				
			  }
			  
			  System.out.println(" Total: " + Experiment.size() + " measurements read.");
			  frd.close();
		}
		catch (FileNotFoundException e) {
		  e.printStackTrace();
		  System.out.println("Input file not found");
		  return false;
		} catch (IOException e) {
		  e.printStackTrace();
		  System.out.println("FileIO exception");
		  System.exit(1);
		}
	  return true;
  } // readExperimentFromFile()
  
}
