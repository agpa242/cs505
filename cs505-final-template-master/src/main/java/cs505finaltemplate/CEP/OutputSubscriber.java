package cs505finaltemplate.CEP;

import cs505finaltemplate.Launcher;
import cs505finaltemplate.httpcontrollers.API;
import io.siddhi.core.util.transport.InMemoryBroker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputSubscriber implements InMemoryBroker.Subscriber {

    private String topic;
    private Map<Integer, Integer> prevZipAlert;

    public OutputSubscriber(String topic, String streamName) {
        this.topic = topic;
        this.prevZipAlert = new HashMap<>();
    }

    @Override
    public void onMessage(Object msg) {
        try {
            //Create list for holding alerted zipcodes
            List<Integer> alertZipList = new ArrayList<>();

            System.out.println("OUTPUT CEP EVENT: " + msg);
            System.out.println("");

            //You will need to parse output and do other logic,
            //but this sticks the last output value in main
            Launcher.lastCEPOutput = String.valueOf(msg);

            //Parse Event Message and split into list
            String parsedMsg = String.valueOf(msg).replaceAll("\\[|\\{|}|]|\"event\":", "");
            parsedMsg = parsedMsg.replaceAll("\"zip_code\":|\"count\":|\"", "");
            String[] splitMsg = parsedMsg.split(",");

            //Build Alert String for each zip
            for (int i=0; i < splitMsg.length-1; i+=2){
                //separate zip and count from split message into separate variables
                Integer zip = Integer.parseInt(splitMsg[i]);
                Integer newCount = Integer.parseInt(splitMsg[i+1]);

                //Check if zip was in previous event
                if (prevZipAlert.containsKey(zip)) {
                    //If zipcode's count has doubled in size, add zip to alert list
                    if (prevZipAlert.get(zip) * 2 <= newCount) {
                        alertZipList.add(zip); 
                    }
                    //Set prevZipAlert to current event info
                    prevZipAlert.replace(zip, newCount);
                }
                else {
                    //If zipcode wasn't in the last event. Add it to the list.
                    prevZipAlert.put(zip, newCount);
                }
            }
            //Send alert list and count to API for RTR functions
            API.alertZipList = alertZipList.stream().mapToInt(Integer::intValue).toArray();
            API.numAlertedZips = alertZipList.size();
            
            

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getTopic() {
        return topic;
    }

}
