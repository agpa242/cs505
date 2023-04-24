package cs505finaltemplate.graphDB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OEdge;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

import cs505finaltemplate.Topics.HospitalData;
import cs505finaltemplate.Topics.HospitalStatusData;
import cs505finaltemplate.Topics.PatientData;
import cs505finaltemplate.Topics.VaccinationData;

public class GraphDBEngine {
    private static String dbName = "BasingTheData";
    private static String user = "root";
    private static String pass = "rootpwd";


    //!!! CODE HERE IS FOR EXAMPLE ONLY, YOU MUST CHECK AND MODIFY!!!
    public GraphDBEngine() {

        //launch a docker container for orientdb, don't expect your data to be saved unless you configure a volume
        //docker run -d --name orientdb -p 2424:2424 -p 2480:2480 -e ORIENTDB_ROOT_PASSWORD=rootpwd orientdb:3.0.0

        //use the orientdb dashboard to create a new database
        //see class notes for how to use the dashboard
        OrientDB client = new OrientDB("remote:localhost", user, pass, OrientDBConfig.defaultConfig());

        // Reset database and start new one
        resetDB(client);
        client.close();

    }

    //Will take patient data and set up the graphDB appropriately, making vertices for provided information and drawing edges between them
    public static boolean handlePatientData(PatientData patient) {
        try {
            OrientDB client = new OrientDB("remote:localhost", "root", "rootpwd", OrientDBConfig.defaultConfig());
            ODatabaseSession db = client.open(dbName, user, pass, OrientDBConfig.defaultConfig());
            //Add the patient to the database
            Optional<OVertex> temp = getPatientByMRN(db, patient.patient_mrn);
            OVertex patientVertex = null;
            if (temp.isPresent())
                patientVertex = updatePatient(patient, temp.get());
            else
                patientVertex = createPatient(db, patient);

            addVaccinationToPatient(db, patient, patientVertex); //creates vaccination vertices and edges as needed to conenct patient vertices
            addContactsToPatient(db, patient, patientVertex); //creates contact vertices and edges as needed to connect patient vertices
            addEventsToPatient(db, patient, patientVertex); //creates event vertices and edges as needed to connect patient vertices

            //close database connection
            db.close();
            client.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return false;
        }
        return true;
    }

    //Will take hospital data and set up the graphDB appropriately, making vertices for provided information and drawing edges between them
    public static boolean handleHospitalData(HospitalData hospital) {
        try {
            OrientDB orient = new OrientDB("remote:localhost", "root", "rootpwd", OrientDBConfig.defaultConfig());
            ODatabaseSession db = orient.open(dbName, user, pass, OrientDBConfig.defaultConfig());
            //get patient record or add to database
            Optional<OVertex> tempPatient = getPatientByMRN(db, hospital.patient_mrn);
            OVertex patientVertex = null;
            
            if (tempPatient.isPresent()) //if the patient is already there, get the relevant data from that vertex
                patientVertex = updatePatient(hospital, tempPatient.get());
            else                         //if the patient is not already there, we create a new patient using the HospitalData object
                patientVertex = createPatient(db, hospital);

            addHospitalToPatient(db, hospital, patientVertex); //add an edge between the patient and hospital vertices
            
            //Close the connection to the database
            db.close();
            orient.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return false;
        }
        return true;
    }

    //Will take vaccination data and set up the graphDB appropriately, making vertices for provided information and drawing edges between them
    public static boolean handleVaccinationData(VaccinationData vaccination) {
        try {
            OrientDB client = new OrientDB("remote:localhost", "root", "rootpwd", OrientDBConfig.defaultConfig());
            ODatabaseSession db = client.open(dbName, user, pass, OrientDBConfig.defaultConfig());
            
            Optional<OVertex> tempPatient = getPatientByMRN(db, vaccination.patient_mrn);
            OVertex patientVertex = null;
            if (tempPatient.isPresent())
                patientVertex = updatePatient(vaccination, tempPatient.get());
            else
                patientVertex = createPatient(db, vaccination);

            //update hospital connection
            addVaccinationToPatient(db, vaccination, patientVertex);

            //Close Connection to Database
            db.close();
            client.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return false;
        }
        return true;
    }

    //Accepts a hospital data object and a patient vertex and builds an edge between then patient and hospital
    private static void addHospitalToPatient(ODatabaseSession db, HospitalData data, OVertex patient) {
        Optional<OVertex> newHospital = getHospitalByID(db, data.hospital_id); //adds the hospital to the database
        OVertex hospital = null; //create a null vertex to populate with the hospital's data
        
        if (newHospital.isPresent()) //if the hospital exists, then we simply get the relevant information
            hospital = newHospital.get();
        else                         //if the hospital does not exist, we create a new one using the information in the Hospital object
            hospital = createHospital(db, data.hospital_id);

        //Draw an edge between a patient and a hospital
        OEdge edge = patient.addEdge(hospital, "patient_to_hospital");
        edge.save();
    }

    //Accepts a vaccination data object and a patient vertex and builds an edge between then patient and vaccination site
    private static void addVaccinationToPatient(ODatabaseSession db, VaccinationData data, OVertex patient) {
        Optional<OVertex> tempVaccination = getVaccinationByID(db, data.vaccination_id); //adds the hospital to the database
        OVertex vaccination = null; //create a null vertex to populate with the hospital's data
        
        if (tempVaccination.isPresent()) //if the hospital exists, then we simply get the relevant information
            vaccination = tempVaccination.get();
        else                             //if the hospital does not exist, we create a new one using the information in the Hospital object
            vaccination = createVaccination(db, data.vaccination_id);

        //Draw an edge between a patient and a hospital
        OEdge edge = patient.addEdge(vaccination, "patient_to_vaccination");
        edge.save();
    }

    //Accepts a patient data object and a patient vertex and draws an edge betwen the two, or builds a vertex as needed
    private static void addVaccinationToPatient(ODatabaseSession db, PatientData patient, OVertex patientVertex) {
        
        //Add vaccination facility to database
        Optional<OVertex> tempVaccination = getVaccinationByID(db, patient.testing_id);
        OVertex vaccination = null;

        if (tempVaccination.isPresent()) //if the vaccination facility exists, then we simply get the relevant information
            vaccination = tempVaccination.get();
        else                         //if the vaccination facility does not exist, we create a new one using the information in the Vaccination object
            vaccination = createVaccination(db, patient.testing_id);

        //Draw an edge between a patient and a vaccine facility
        OEdge edge = patientVertex.addEdge(vaccination, "patient_to_vaccination");
        edge.save();
    }

    //Updates a patient vertex with data derived from a PatientData object
    private static OVertex updatePatient(PatientData patientData, OVertex patient) {
        patient.setProperty("patient_name", patientData.patient_name);
        patient.setProperty("patient_zipcode", patientData.patient_zipcode);
        patient.setProperty("patient_covid_status", patientData.patient_status);
        patient.setProperty("testing_id", patientData.testing_id);
        patient.save();
        return patient;
    }

    //Updates a patient vertex with data derived from a HospitalData object
    private static OVertex updatePatient(HospitalData hospitalData, OVertex patient) {
        patient.setProperty("patient_name", hospitalData.patient_name);
        patient.setProperty("hospital_id", hospitalData.hospital_id);
        patient.setProperty("patient_hospital_status", hospitalData.patient_status);
        patient.save();
        return patient;
    }

    //Updates a patient vertex with data derived from a VaccinationData object
    private static OVertex updatePatient(VaccinationData vaccinationData, OVertex patient) {
        patient.setProperty("patient_name", vaccinationData.patient_name);
        patient.setProperty("vaccination_id", vaccinationData.vaccination_id);
        patient.setProperty("patient_vaccination_status", 1);
        patient.save();
        return patient;
    }

    private static void addContactsToPatient(ODatabaseSession db, PatientData patientData, OVertex patient) {
            Map<String, Integer> seenContacts = new HashMap<>();
            
            for (String contact : patientData.contact_list) {
                if (!seenContacts.containsKey(contact)) {
                    seenContacts.put(contact, 1);
                    
                    Optional<OVertex> tempContact = getPatientByMRN(db, contact); //Get the contact patient by MRN number, turn them into a temporary patient
                    OVertex contact_patient = null; //set up a new null vertex to populate with conact's data
                    if (tempContact.isPresent())
                        contact_patient = tempContact.get(); //if the tempContact is is present, we go ahead and get the information from that vertex and apply it to contact_patient
                    else
                        contact_patient = createPatient(db, contact); //if the tempContact is not present, we take that information and create a new contact for it altogether

                    //Draw an edge between the patient and the contact
                    OEdge edge = patient.addEdge(contact_patient, "patient_to_patient");
                    edge.save();
                }

            }
        }

    //Creates event objects based on ids found in patientData, then creates edges from the patient vertex represented by patientData to the event
    private static void addEventsToPatient(ODatabaseSession db, PatientData patientData, OVertex patientVertex) {
        Map<String, Integer> seenEvents = new HashMap<>(); //keep track of seen events to prevent duplicates
        
        for (String event : patientData.event_list) {    
            if (!seenEvents.containsKey(event)) { //if we haven't already seen this event
                seenEvents.put(event, 1);
                Optional<OVertex> tempEventVertex = getEventByID(db, event); //check if event already exists in database
                OVertex eventVertex = null;
                if (tempEventVertex.isPresent())
                    eventVertex = tempEventVertex.get();
                else
                    eventVertex = createEvent(db, event); //create new event vertex if it doesn't exist

                OEdge edge = patientVertex.addEdge(eventVertex, "patient_to_event"); // create edge between patient and event
                edge.save(); //save edge to database
            }
        }
    }

    //DEFUNCT silly way to do things, but leaving as it might be useful somewhere down the line
    //Takes a database session and creates a patient using inputs to fill in the Vertex.
    private static OVertex createPatient(ODatabaseSession db, Integer testing_id, String patient_name, String patient_mrn, Boolean patient_status) {
        OVertex result = db.newVertex("patient");
        result.setProperty("testing_id", testing_id);
        result.setProperty("patient_name", patient_name);
        result.setProperty("patient_mrn", patient_mrn);
        result.setProperty("patient_status", patient_status); 
        result.save();
        return result;
    }

    //Creates a patient vertex based on an already existing PatientData object
    private static OVertex createPatient(ODatabaseSession db, PatientData patient) {
        OVertex result = db.newVertex("patient");
        result.setProperty("testing_id",patient.testing_id);
        result.setProperty("patient_name", patient.patient_name);
        result.setProperty("patient_mrn", patient.patient_mrn);
        result.setProperty("patient_status", patient.patient_status); 
        result.save();
        return result;
    }

    //Creates a patient vertex based on HospitalData object
    private static OVertex createPatient(ODatabaseSession db, HospitalData hospital) {
        OVertex result = db.newVertex("patient");
        result.setProperty("patient_mrn", hospital.patient_mrn);
        result.setProperty("patient_name", hospital.patient_name);
        result.setProperty("patient_hospital_status", hospital.patient_status);
        result.setProperty("hospital_id", hospital.hospital_id);
        result.setProperty("patient_vaccination_status", 0);
        result.save();
        return result;
    }

    //Creates a patient vertex based on a VaccinationData object
    private static OVertex createPatient(ODatabaseSession db, VaccinationData vax) {
        OVertex result = db.newVertex("patient");
        result.setProperty("patient_mrn", vax.patient_mrn);
        result.setProperty("patient_name", vax.patient_name);
        result.setProperty("hospital_id", vax.vaccination_id);
        result.setProperty("patient_vaccination_status", 1); //since there is vaccination data for a patient, we know they were vaccinated
        result.save();
        return result;
    }

    //Alternate create patient for if we only have minimal data
    private static OVertex createPatient(ODatabaseSession db, String patient_mrn) {
        OVertex result = db.newVertex("patient");
        result.setProperty("patient_mrn", patient_mrn); //this is the only confirmed info we have in this case
        result.setProperty("patient_vaccination_status", 0); //until we know better, they are negative
        return result;
    }

    //Creates a new hospital vertex, does not include patient information as that will be on a patient node, which will be connected by an edge
    private static OVertex createHospital(ODatabaseSession db, Integer hospital_id) {
        OVertex result = db.newVertex("hospital");
        result.setProperty("hospital_id", hospital_id);
        result.save();
        return result;
    }

    //Creates a new vaccination vertex, does not include patient information as it will be loaded into an edge
    public static OVertex createVaccination(ODatabaseSession db, Integer vaccination_id){
        OVertex result = db.newVertex("vaccination");
        result.setProperty("vaccination_id", vaccination_id);
        result.save();
        return result;
    } 
    
    //Creates a new event vertex, does not include patient information as it will be loaded into an edge
    private static OVertex createEvent(ODatabaseSession db, String event_id) {
        OVertex result = db.newVertex("event");
        result.setProperty("event_id", event_id);
        result.save();
        return result;
    }

    //Searches the database for confirmed contacts between patients and returns a list of the MRNs of contacts
    public static List<String> getConfirmedContacts(String patient_mrn) {
        try {
            //Create database connection
            OrientDB orient = new OrientDB("remote:localhost", "root", "rootpwd", OrientDBConfig.defaultConfig());
            ODatabaseSession db = orient.open(dbName, user, pass, OrientDBConfig.defaultConfig());

            //Query to find contacts
            String query = "TRAVERSE inE(), outE(), inV(), outV() " +
                    "FROM (select from patient where patient_mrn = ?) " +
                    "WHILE $depth <= 2";
            OResultSet rs = db.query(query, patient_mrn); //create a result set of the results from the query

            List<String> results = new ArrayList<>(); //create a list to hold resulting MRNs to be returned
            while (rs.hasNext()) {
                OResult item = rs.next();
                if (item.hasProperty("patient_mrn")) {
                    String contact = item.getProperty("patient_mrn"); //get the string value of a single contact in the result set
                    
                    if (!patient_mrn.equals(contact)) { //if it is not the current patient's mrn, add the contact MRN to list
                        results.add(contact);
                    }

                }
            }
            rs.close(); //close the result set

            //Close connection to database
            db.close();
            orient.close();

            return results; //returns list of contacts

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return new ArrayList<>();
        }
    }

    //Accepts a patient's mrn and searches for possible contacts based on events that patients attended
    //Output is a HashMap containing event_id in the string and a List<String> containing the MRNs of all patients who attended the event
    public static Map<String, List<String>> getPotentialContacts(String patient_mrn) {
        try {
            //Open a connection to the client
            OrientDB client = new OrientDB("remote:localhost", "root", "rootpwd", OrientDBConfig.defaultConfig());
            ODatabaseSession db = client.open(dbName, user, pass, OrientDBConfig.defaultConfig());

            String query = "Traverse inE(), outE(), inV(), outV() FROM " +
                    "(select * from patient where patient_mrn = ?) " +
                    "while $depth <= 2";
            OResultSet rs = db.query(query, patient_mrn);

            Map<String, List<String>> possibleContacts = new HashMap<>(); //Create a HashMap that contains the patient under review's MRN and the list containing possible contacts
            String event_id = "";
            while (rs.hasNext()) { //while there are still results in the ResultSet
                OResult item1 = rs.next();

                if (item1.hasProperty("event_id")) { //check to see if item1 is an event
                    event_id = item1.getProperty("event_id");
                }
                
                    
                if (event_id != "") {
                    String query2 = "TRAVERSE inE(), outE(), inV(), outV() FROM " +
                            "(select * from event where event_id = ?) " +
                            "WHILE $depth <= 2";
                    OResultSet rs2 = db.query(query2, event_id);

                    String currentEventID = null; //variable to contain an event_id as we move through result sets
                    List<String> currentMRNs = new ArrayList<>(); //list of patient MRNs who were present at a given event
                    while (rs2.hasNext()) {
                        OResult item = rs2.next();
                        if (item.hasProperty("event_id")) {
                            //Check if first event in the result set
                            String temp = item.getProperty("event_id");
                            
                            if (currentEventID == null)  { //if currentEventID is empty, populate with the event_id of item
                                currentEventID = temp;
                            }
                            else if (temp.equals(currentEventID)) { //if the event_id of item matches the currentEventID, we can count this as a contact
                                possibleContacts.put(currentEventID, currentMRNs);
                            }
                        }
                        if (item.hasProperty("patient_mrn")) { //if the item is a patient
                            String temp = item.getProperty("patient_mrn"); 
                            if (!temp.equals(patient_mrn)) //if the patient's mrn doesn't match the patient passed into the function
                                currentMRNs.add(item.getProperty("patient_mrn")); //add the patient's MRN to currentMRN's (list of MRNs present at event)
                        }
                    }
                    if (currentEventID != null && currentMRNs.size() > 0)
                        possibleContacts.put(currentEventID, currentMRNs); //add the event_id of the event as well as the list of MRNs of patients who were present
                    rs2.close();
                }
            }
            rs.close();

            //Close databse connection
            db.close();
            client.close();

            return possibleContacts; //return the list of possible contacts

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return new HashMap<>();
        }
    }

    //Creates a query to select a patient from the database given patient_mrn
    public static Optional<OVertex> getPatientByMRN(ODatabaseSession db, String patient_mrn) {
        String query = "select * from patient where patient_mrn = ?"; //using * here is good in case there is duplicate data, might be useful for db cleansing later
        OResultSet rs = db.query(query, patient_mrn);
        
        Optional<OVertex> result = null;
        if (rs.hasNext()) {
            OResult patient = rs.next();
            result = patient.getVertex();
            System.out.println("contact: " + patient.getProperty("patient_mrn"));
        }
        rs.close(); //close resultset

        return result;
    }

    //Creates a query to select a patient from the database given the event_id
    private static Optional<OVertex> getEventByID(ODatabaseSession db, String event_id) {
        String query = "select * from patient where event_id = ?"; //using * here is good in case there is duplicate data, might be useful for db cleansing later
        OResultSet rs = db.query(query, event_id); //create a result set out of the query results

        Optional<OVertex> event = Optional.empty();
        if (rs.hasNext()) {
            OResult result = rs.next();
            event = result.getVertex();
        }

        rs.close(); //close result set
        
        return event;
    }

    //Creates a query to select a hospital by it's ID number
    private static Optional<OVertex> getHospitalByID(ODatabaseSession db, Integer hospital_id) {
        String query = "select * from hospital where hospital_id = ?"; //using * here is good in case there is duplicate data, might be useful for db cleansing later
        OResultSet rs = db.query(query, hospital_id); //make a result set out of the query results

        Optional<OVertex> hospital = Optional.empty();
        if (rs.hasNext()) {
            OResult result = rs.next();
            hospital = result.getVertex();
        }

        rs.close(); //close result set

        return hospital;
    }

    //Creates a query to select a vaccination facility by it's ID number
    private static Optional<OVertex> getVaccinationByID(ODatabaseSession db, Integer vaccination_id) {
        String query = "select * from vaccination where vaccination_id = ?"; //using * here is good in case there is duplicate data, might be useful for db cleansing later
        OResultSet rs = db.query(query, vaccination_id); //make a result set out of the query results

        Optional<OVertex> vaccination = Optional.empty();
        if (rs.hasNext()) {
            OResult result = rs.next();
            vaccination = result.getVertex();
        }

        rs.close(); //close result set

        return vaccination;
    }    

    //Get patient status of all hospitals 
    public static HospitalStatusData getPatientStatus() {
        
        try {
            //Make connection with database
            OrientDB orient = new OrientDB("remote:localhost", "root", "rootpwd", OrientDBConfig.defaultConfig());
            ODatabaseSession db = orient.open(dbName, user, pass, OrientDBConfig.defaultConfig());
            
            // Get patients that attend hospital
            String query = "select hospital_id, patient_hospital_status, patient_vaccination_status from patient where hospital_id is not null order by hospital_id";
            OResultSet rs = db.query(query);

            List<Integer> vaxStatus = new ArrayList<>();
            List<Integer> patStatus = new ArrayList<>();
            while (rs.hasNext()) {
                OResult item = rs.next();
                if (item.hasProperty("patient_vaccination_status"))
                    vaxStatus.add(item.getProperty("patient_vaccination_status"));
                if (item.hasProperty("patient_hospital_status"))
                    patStatus.add(item.getProperty("patient_hospital_status"));
            }
            rs.close();
            HospitalStatusData result = getHospitalStatus(vaxStatus, patStatus);
            
            //Close the database connection 
            db.close();
            orient.close();

            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return new HospitalStatusData();
        }
    }

    //Get patient status of a particular location
    public static HospitalStatusData getPatientStatusByHospitalID(Integer hospital_id) {
        try {   
            //Make connection with database
            OrientDB orient = new OrientDB("remote:localhost", "root", "rootpwd", OrientDBConfig.defaultConfig());
            ODatabaseSession db = orient.open(dbName, user, pass, OrientDBConfig.defaultConfig());     

            //Get hospitals current patients
            String query = "select patient_vaccination_status, patient_hospital_status from patient where hospital_id = ?";
            OResultSet rs = db.query(query, hospital_id); //contains results of query

            List<Integer> vaxStatus = new ArrayList<>();
            List<Integer> patStatus = new ArrayList<>();
            while (rs.hasNext()) {
                OResult item = rs.next(); //look through each returned value in the query
                if (item.hasProperty("patient_vaccination_status"))
                    vaxStatus.add(item.getProperty("patient_vaccination_status"));
                if (item.hasProperty("patient_hospital_status"))
                    patStatus.add(item.getProperty("patient_hospital_status"));
            }
            rs.close();

            //Close database connection
            orient.close();
            db.close();

            return getHospitalStatus(vaxStatus, patStatus);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return new HospitalStatusData();
        }
    }

    //Calculates the number of people in a given hospital and their vaccination status as a percentage
    public static HospitalStatusData getHospitalStatus(List<Integer> vaxStatus, List<Integer> patStatus) {
        //Initialize the variables to hold counts for each category
        Integer in_patient_count = 0;
        Integer in_patient_vax_count = 0;
        Integer icu_patient_count = 0;
        Integer icu_vax_count = 0;
        Integer patient_vent_count = 0;
        Integer vent_vax_count = 0;

        //patStatus includes all patients which are relevant here, so loop through them all and count their status
        for (Integer i=0; i<patStatus.size(); i++) {
            
            //Calculate the number of patients in each category, as well as how many of them are vaccinated
            if (patStatus.get(i) == 1) { //1 is in patients
                in_patient_count++;
                if(vaxStatus.get(i) == 1) {
                    in_patient_vax_count++;
                }
            } 
            else if (patStatus.get(i) == 2) { //2 is icu patients
                icu_patient_count++;
                if(vaxStatus.get(i) == 1) {
                    icu_vax_count++;
                }
            }
            else if (patStatus.get(i) == 3) { //3 is patients on vent
                patient_vent_count++;
                if(vaxStatus.get(i) == 1) {
                    vent_vax_count++;
                }
            }
        }

        //Calculate percentaqges of vaccinated patients in accordance with HospitalStatus formatting
        //We only calcualte if the count value is not equal to zero to avoid math errors
        Float in_vax_percentage = 0f;
        Float icu_vax_percentage = 0f;
        Float vent_vax_percentage = 0f;

        if (in_patient_count != 0) { //gets percentage of in patients who are vaccinated
            in_vax_percentage = in_patient_count.floatValue() / in_patient_vax_count.floatValue();
        }
        if (icu_patient_count != 0) { //gets percentage of icu patients who are vaccinated
            icu_vax_percentage = icu_patient_count.floatValue() / icu_vax_count.floatValue();
        }
        if (icu_patient_count != 0) { //gets percentage of icu patients who are vaccinated
            vent_vax_percentage = patient_vent_count.floatValue() / vent_vax_count.floatValue();
        }

        //Create HospitalData object to populate with our counts
        HospitalStatusData status = new HospitalStatusData();
        status.in_patient_count = in_patient_count;
        status.in_patient_vax = in_vax_percentage;
        status.icu_patient_count = icu_patient_count;
        status.icu_patient_vax = icu_vax_percentage;
        status.patient_vent_count = patient_vent_count;
        status.patient_vent_vax = vent_vax_percentage;

        return status; //output the HospitalData object so it can later be returned as a JSON object 
    }

    //Removes values from database, but does not destroy the architecture
    private void clearDB(ODatabaseSession db) {

        String query = "DELETE VERTEX FROM patient";
        db.command(query);

    }

    //Rebuilds the database
    private static int rebuild(OrientDB client) {
        try {
            if (client.exists(dbName)) {
                client.drop(dbName); //if the database exists, we drop it
                System.out.println(dbName + " has been dropped. It will now be reset.");
            }
            client.create(dbName, ODatabaseType.PLOCAL, OrientDBConfig.defaultConfig()); //creates a new DB
            ODatabaseSession db = client.open(dbName, user, pass, OrientDBConfig.defaultConfig()); //open DB session
            build(db); //build DB
            db.close(); //close DB connection
            return 1;
        } catch (Exception ex) {
            System.out.println(ex);
            return 0;
        }
        
    }

    //Builds the database architecture needed to insert data using the createX functions which are written above.
    private static void build(ODatabaseSession db) {
        try {

            //Set up a vertex class for patient data JSON as specified in assignment
            OClass patient = db.getClass("patient"); //creates class based on specifications
            if (patient == null) {
                patient = db.createVertexClass("patient");
            }

            if (patient.getProperty("patient_mrn") == null) { //if mrn is null, we can populate
                patient.createProperty("testing_id", OType.INTEGER); //ID number of patient's testing facility
                patient.createProperty("patient_name", OType.STRING);
                patient.createProperty("patient_mrn", OType.STRING); //patient medical record number, should be unique                patient.createProperty("patient_zipcode", OType.INTEGER);
                patient.createProperty("patient_status", OType.BOOLEAN); //will either be 1 (positive) or 0 (negative)
                patient.createProperty("contact_list", OType.STRING); //contains MRNs of other patients that have been in known contact with this patient
                patient.createProperty("event_list", OType.STRING); //contains list of event_id that the person visited
            }

            //Set up a vertex class for hospital data JSON as specified in assignment
            OClass hospital = db.getClass("hospital");
            if (hospital == null) {
                hospital = db.createVertexClass("hospital");
            }

            if (hospital.getProperty("hospital_id") == null) { //if id is null, we can populate
                hospital.createProperty("hospital_id", OType.INTEGER); //ID number of hospital which is housing patient
                hospital.createProperty("patient_name", OType.STRING); //should correspond with a name in the patient class
                hospital.createProperty("patient_mrn", OType.STRING); //patient medical record number, should match one present in the patient class
                hospital.createProperty("patient_status", OType.INTEGER); //values can be 1 (in-patient), 2 (icu), or 3 (vent)
            }

            //Set up a vertex class for vaccination data JSON as specified in assignment
            OClass vaccination = db.getClass("hospital");
            if (vaccination == null) {
                vaccination = db.createVertexClass("vaccination");
            }

            if (vaccination.getProperty("vaccination_id") == null) { //if id is null, we can populate
                hospital.createProperty("vaccination_id", OType.INTEGER); //ID number of vaccine testing facility
                hospital.createProperty("patient_name", OType.STRING); //should correspond with a name in the patient class
                hospital.createProperty("patient_mrn", OType.STRING); //patient medical record number, should match one present in the patient class
            }

            //Create a vertex class for the tracked events
            OClass event = db.getClass("event");
            if (event == null) {
                event = db.createVertexClass("event");
            }

            if (event.getProperty("event_id") == null) {
                event.createProperty("event_id", OType.STRING);
            }

            //Create an edge class for patients that the patient was in contact with
            if (db.getClass("contact_with") == null) {
                db.createEdgeClass("contact_with");
            }

            //Create an edge class for events that the patient visited
            if  (db.getClass("event_visited") == null) {
                db.createEdgeClass("event_visited");
            }

            //Create an edge class for the hospital a patient is in
            if (db.getClass("hospital_at") == null) {
                db.createEdgeClass("hospital_at");
            }

            //Create an edge class for the vaccination station that a patient visited
            if (db.getClass("vaccination_at") == null) {
                db.createEdgeClass("vaccination_at");
            }
            
        } catch (Exception ex) { 
            System.out.println(ex);
        }
    }

    //Called in API to flush and rebuild the DB
    public static int reset() {
        int result = -1; //set to a value that can't be handled so we don't have an accidental reset

        try {
            OrientDB orient = new OrientDB("remote:localhost", OrientDBConfig.defaultConfig());
            result = rebuild(orient);
            orient.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return result;
    }


}
