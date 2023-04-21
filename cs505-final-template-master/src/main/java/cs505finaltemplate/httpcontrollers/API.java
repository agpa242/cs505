package cs505finaltemplate.httpcontrollers;

import com.google.gson.Gson;
import cs505finaltemplate.Launcher;
import cs505finaltemplate.CEP.OutputSubscriber;
import cs505finaltemplate.Topics.HospitalStatusData;
import cs505finaltemplate.graphDB.GraphDBEngine;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api")
public class API {

    @Inject
    private javax.inject.Provider<org.glassfish.grizzly.http.server.Request> request;

    private Gson gson;

    public API() {
        gson = new Gson();
    }


    @GET
    @Path("/getteam")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getteam() {
        String responseString = "{}";
        try {
            System.out.println("WHAT");
            Map<String,String> responseMap = new HashMap<>();
            responseMap.put("team_name", "Incognito Mode");
            responseMap.put("Team_members_sids", "[912653262, 912228415]");
            responseMap.put("app_status_code","1");

            responseString = gson.toJson(responseMap);
        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }


    @GET
    @Path("/getlastcep")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessCount(@HeaderParam("X-Auth-API-Key") String authKey) {
        String responseString = "{}";
        try {
            //generate a response
            Map<String,String> responseMap = new HashMap<>();
            responseMap.put("lastoutput",Launcher.lastCEPOutput);
            responseString = gson.toJson(responseMap);

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }


    //Resets the whole database
    @GET
    @Path("/reset")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reset() {
        String responseString = "{}";
        try {
            
            int result = GraphDBEngine.reset();
            Map<String,Integer> responseMap = new HashMap<>();
            responseMap.put("reset_status_code", result);
            responseString = gson.toJson(responseMap);

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }


    
    public static int[] alertZipList; //this is populated in OutputSubscriber.java
    //Get a zipped list of all zipcodes currently experiencing alert status.
    @GET
    @Path("/zipalertlist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response zipAlertList() {
        String responseString = "{}";
        try {
            Map<String,int[]> responseMap = new HashMap<>();
            responseMap.put("ziplist", alertZipList);

            responseString = gson.toJson(responseMap);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }


    public static Integer numAlertedZips = 0; //this is populated in OutputSubscriber.java
    //Set reset code so that the database tables can be dropped and recreated.
    @GET
    @Path("/alertlist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alertList() {
        String responseString = "{}";
        try {
            Map<String, Integer> responseMap = new HashMap<>();
            
            if (numAlertedZips >= 5) { //Check if state is on alert
                responseMap.put("state_status", 1); //Alert = 1, State is on alert
            }
            else {
                responseMap.put("state_status", 0); //Alert = 0, State is not on alert
            }
            responseString = gson.toJson(responseMap);

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    /*
     *  Accepts patient mrn and finds confirmed contacts
     */
    @GET
    @Path("/getconfirmedcontacts/{mrn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfirmedContacts(@PathParam("mrn") String mrn) {
        String responseString = "{}";
        try {

            //generate a response
            Map<String, List<String>> responseMap = new HashMap<>();
            List<String> confirmedContacts = GraphDBEngine.getConfirmedContacts(mrn);

            responseMap.put("contact-list", confirmedContacts);
            responseString = gson.toJson(responseMap);

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    /*
     *  Accepts patient mrn and finds possible contacts by checking common events
     */
    @GET
    @Path("/getpossiblecontacts/{mrn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPossibleContacts(@PathParam("mrn") String mrn) {
        String responseString = "{}";
        try {
            Map<String, Map<String,List<String>>> responseMap = new HashMap<>();
            Map<String, List<String>> result = GraphDBEngine.getPotentialContacts(mrn);
            responseMap.put("contactlist", result);

            responseString = gson.toJson(responseMap);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    /*
     *  gets the status of patients in a specific hospital provided by hospital_id
     */
    @GET
    @Path("/getpatientstatus/{hospital_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientStatusByHospitalID(@PathParam("hospital_id") Integer hospital_id) {
       String responseString = "{}";
        try {
            HospitalStatusData dataObj = GraphDBEngine.getPatientStatusByHospitalID(hospital_id);
            responseString = gson.toJson(dataObj);
            System.out.println(responseString);

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }

    //Gets the status of all patients regardless of hospital
    @GET
    @Path("/getpatientstatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientStatus() {
        String responseString = "{}";
        try {
            HospitalStatusData dataObj = GraphDBEngine.getPatientStatus();
            responseString = gson.toJson(dataObj);
            System.out.println(responseString);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            ex.printStackTrace();

            return Response.status(500).entity(exceptionAsString).build();
        }
        return Response.ok(responseString).header("Access-Control-Allow-Origin", "*").build();
    }
}

