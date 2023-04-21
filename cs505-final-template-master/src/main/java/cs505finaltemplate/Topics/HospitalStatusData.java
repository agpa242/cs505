package cs505finaltemplate.Topics;

import java.util.List;

public class HospitalStatusData {

    public int in_patient_count;
    public float in_patient_vax; //decimal percentage of vaccinated in-patients
    public int icu_patient_count;
    public float icu_patient_vax; //decimal percentage of vaccinated icu-patients
    public int patient_vent_count;
    public float patient_vent_vax; //decimal percentage of vaccinated vent patients


    public  HospitalStatusData() {
        in_patient_count = 0;
        in_patient_vax = 0f;
        icu_patient_count = 0;
        icu_patient_vax = 0f;
        patient_vent_count = 0;
        patient_vent_vax = 0f;
    }

}