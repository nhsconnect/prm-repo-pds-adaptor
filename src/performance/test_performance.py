from locust import HttpUser, task, between

class Sampletest(HttpUser):
    wait_time = between(0.5, 1)

    @task
    def suspended_patient_status(self):
        self.client.headers = {"Authorization": "$AN_AUTH_TOKEN_YOU_SHOULDNT_PUSH"}
        self.client.get("/suspended-patient-status/$A_PATIENT_ID")