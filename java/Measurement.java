public class Measurement {
		public int nodeid;
		public int senderid;
		public short channel;
		public short rssi;
		public short txpower;			
		public long timestamp;
		public int packetSequence;
		public short lqi;
		public Measurement () {
			nodeid = senderid = channel = rssi = txpower = 0;
		}
		public void set(int node, int sender, short chan, short rss, short tx, long time, int val, short tlqi) {
			nodeid = node;
			senderid = sender;
			channel = chan;
			rssi = rss;
			txpower = tx;
			lqi = tlqi;
			timestamp = time;
			packetSequence = val;
		}
	}
