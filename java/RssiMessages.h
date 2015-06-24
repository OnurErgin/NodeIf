
/**
 * @author Onur Ergin
 */

#ifndef RSSIMESSAGES_H__
#define RSSIMESSAGES_H__

enum {
  AM_RSSIMSG = 0x06
};

 typedef nx_struct RssiMsg {
  nx_uint16_t appid;	// Application id to avoid processing of the irrelevant packets from other applications
  nx_uint8_t pcktype;	// Type of packet
  nx_uint16_t nodeid;		// ID of the node that sends this packet
  nx_uint16_t senderid;	// Sender node id, of whose rssi is measured
  nx_uint16_t receiverid; // Receiver node id, which measures the rssi of the packet from the sender
  nx_uint16_t counter;
  nx_int8_t channel;	// The channel of the measurement
  nx_int8_t txpower;	// Transmission power of the measurement
  nx_uint16_t sprayIter;	// number of spraying iterations
  nx_int16_t r_rssi;	// rssi that receiver measures
  nx_uint8_t flag; // flag bit for various uses
} RssiMsg;

#endif //RSSIMESSAGES_H__
