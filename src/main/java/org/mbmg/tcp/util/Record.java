package org.mbmg.tcp.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by rpomeroy on 4/26/14.
 */
public class Record {

    // stationId_channel value timestamp
    private static String GRAPHITE_FORMAT = "%s.%s.%s %f %s\n";
    private static ZoneId UTC = ZoneId.of("UTC");
    private static Map<String,String> channelCodeToName = new HashMap<>();
    private static List<String> activeChannels = new ArrayList<>();
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

    static {
        channelCodeToName.put("A00", "Humidity");
        //channelCodeToName.put("A01", "Solar_Controller_DC_Voltage"); // commented out because multiplexed
        //channelCodeToName.put("A02", "Battery_Bus_DC_Voltage"); // commented out because multiplexed
        channelCodeToName.put("A03", "Inverter_AC_Voltage");
        channelCodeToName.put("A04", "Inverter_AC_Current");
        channelCodeToName.put("A05", "NA");
        channelCodeToName.put("A06", "NA");
        channelCodeToName.put("A07", "NA");
        channelCodeToName.put("A08", "NA");
        channelCodeToName.put("A09", "Inverter_Active_Power");
        channelCodeToName.put("A10", "Inverter_Reactive_Power");
        channelCodeToName.put("A11", "Inverter_Power_Factor");
        channelCodeToName.put("A12", "Inverter_Frequency");
        channelCodeToName.put("A13", "Temperature_1");
        channelCodeToName.put("A14", "Temperature_2");
        channelCodeToName.put("C", "Record_Number");
        channelCodeToName.put("D", "NA");
        channelCodeToName.put("K01", "NA");
        channelCodeToName.put("L", "NA");
        channelCodeToName.put("P01", "NA");
        channelCodeToName.put("P02", "NA");
        channelCodeToName.put("P03", "NA");
        channelCodeToName.put("P04", "NA");
        channelCodeToName.put("P05", "Energy_Counter");
        channelCodeToName.put("P06", "NA");
        channelCodeToName.put("O01", "NA");
        channelCodeToName.put("T", "NA");
        channelCodeToName.put("TM", "TimeStamp");
        
    }

    static {
    	activeChannels.add("A00");
        //activeChannels.add("A01"); // A01 removed from Active Channel and will be added manually as it is multiplexed
        //activeChannels.add("A02"); // A02 removed from Active Channel and will be added manually as it is multiplexed
        activeChannels.add("A03");
        activeChannels.add("A04");
        activeChannels.add("A09");
    	activeChannels.add("A10");
    	activeChannels.add("A11");
    	activeChannels.add("A12");
    	activeChannels.add("A13");
    	activeChannels.add("A14");
    	activeChannels.add("C");
    	activeChannels.add("P05");

    }
    
    static Logger Logger = LoggerFactory.getLogger("MyRecord");
    
    private Long recordNumber;
    private String recordType;
    private String stationID;
    private LocalDateTime timestamp;
    private Map<String, Double> channelData;

    public Record(Long recordNumber, String recordType, String stationID, LocalDateTime timestamp, Map<String,
            Double> channelData) {
        this.recordNumber = recordNumber;
        this.recordType = recordType;
        this.stationID = stationID;
        this.timestamp = timestamp;
        this.channelData = channelData;
    }
    
    public Record(String recordType, String stationID, LocalDateTime timestamp, Map<String,
            Double> channelData) {
        this.recordType = recordType;
        this.stationID = stationID;
        this.timestamp = timestamp;
        this.channelData = channelData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        if (!channelData.equals(record.channelData)) return false;
        if (!recordNumber.equals(record.recordNumber)) return false;
        if (!recordType.equals(record.recordType)) return false;
        if (!stationID.equals(record.stationID)) return false;
        if (!timestamp.equals(record.timestamp)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = recordNumber.hashCode();
        result = 31 * result + recordType.hashCode();
        result = 31 * result + stationID.hashCode();
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + channelData.hashCode();
        return result;
    }

    public Long getRecordNumber() {
        return recordNumber;
    }

    public String getRecordType() {
        return recordType;
    }

    public String getStationID() {
        return stationID;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String formatTimestamp() {
        return this.getTimestamp().format(formatter);
    }

    public Map<String, Double> getChannelData() {
        return channelData;
    }

    public List<String> toGraphite() {
        // path value timestamp \n
        long epochTime = getEpochTime();
        // K01 channel is actually a bit container. The Parser should probably be updated to take this
        // into account.
        double K01_A = 10000330000000000L;
        double K01_B = 10010330000000000L;
        double K01_value = 0;
        Boolean flagk01_A = false;
        Boolean flagk01_B = false;
        
        // clock time of the server in Nairobi time zone, converted to epochtime.
        // UI used, only accepts UTC timezone. Also the timestamp received from Kenya
        // is set to UTC. As such it will correctly read as time from Kenya. In other words,
        // we don't want to apply any transformation since the timezone of the UI will always be
        // UTC.
        LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Africa/Nairobi"));
        Long currentTimeEpoch = currentTime.atZone(UTC).toEpochSecond();
        String currentTimeFormatTimestamp = currentTime.format(formatter);
  
        List<String> data = new ArrayList();
        // first loop to parse the de-multiplexer coded in channel K01
        for (Map.Entry<String,Double> entry : this.channelData.entrySet()) {
        	if (entry.getKey().equals("K01")) {        		
        		if (entry.getValue() == K01_A) {
        			flagk01_A = true;
        			K01_value = entry.getValue();
        		} else if (entry.getValue() == K01_B) {
        			flagk01_B = true;
        			K01_value = entry.getValue();
        		}
        	}
        }
        
        // second loop to create the list
        for (Map.Entry<String,Double> entry : this.channelData.entrySet()) {
        	if (entry.getKey().equals("A01") && flagk01_A) {
        		data.add(String.format(GRAPHITE_FORMAT,
                        //getStationID(), // hardcoded to 654321 as the datalogger has been changed and the device ID is
                   		"654321",		  // now different from the previous one
                        "A01",
                        "Solar_Controller_DC_Voltage",
                        entry.getValue(),
                        currentTimeEpoch));
        		Logger.info(",654321,A01,Solar_Controller_DC_Voltage"+","+entry.getValue()+","+Double.toString(K01_value)+","+currentTimeFormatTimestamp);
       		} else if (entry.getKey().equals("A01") && flagk01_B) {
        		data.add(String.format(GRAPHITE_FORMAT,
                        //getStationID(), // hardcoded to 654321 as the datalogger has been changed and the device ID is
                    	"654321",		  // now different from the previous one
                        "A01",
                        "Dump_Load_Current",
                        entry.getValue(),
                        currentTimeEpoch));
        		Logger.info(",654321,A01,Dump_Load_Current"+","+entry.getValue()+","+Double.toString(K01_value)+","+currentTimeFormatTimestamp);
       		} else if (entry.getKey().equals("A02") && flagk01_A) {
            		data.add(String.format(GRAPHITE_FORMAT,
                            //getStationID(), // hardcoded to 654321 as the datalogger has been changed and the device ID is
                       		"654321",		  // now different from the previous one
                            "A02",
                            "Wind_Current",
                            entry.getValue(),
                            currentTimeEpoch));
            		Logger.info(",654321,A02,Wind_Current"+","+entry.getValue()+","+Double.toString(K01_value)+","+currentTimeFormatTimestamp);
           	} else if (entry.getKey().equals("A02") && flagk01_B) {
            		data.add(String.format(GRAPHITE_FORMAT,
                            //getStationID(), // hardcoded to 654321 as the datalogger has been changed and the device ID is
                        	"654321",		  // now different from the previous one
                            "A02",
                            "Battery_Bus_DC_Voltage",
                            entry.getValue(),
                            currentTimeEpoch));
            		Logger.info(",654321,A02,Battery_Bus_DC_Voltage"+","+entry.getValue()+","+Double.toString(K01_value)+","+currentTimeFormatTimestamp);
        	} else if (activeChannels.contains(entry.getKey())) {
                data.add(String.format(GRAPHITE_FORMAT,
                        //getStationID(), // hardcoded to 654321 as the datalogger has been changed and the device ID is
                		"654321",		  // now different from the previous one
                        entry.getKey(),
                        channelCodeToName.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue(),
                        currentTimeEpoch));
                Logger.info(",654321,"+entry.getKey()+","+channelCodeToName.getOrDefault(entry.getKey(), entry.getKey())+","+entry.getValue()+","+Double.toString(K01_value)+","+currentTimeFormatTimestamp);
            }
        }
        System.out.println(data);
        return data;
    }

    // UI used, only accepts UTC timezone. Also the timestamp received from Kenya
    // is set to UTC. As such it will correctly read as time from Kenya. In other words,
    // we don't want to apply any transformation since the timezone of the UI will always be
    // UTC.
    private long getEpochTime() {
        return this.getTimestamp().atZone(UTC).toEpochSecond();
    }
}
