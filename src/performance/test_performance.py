from locust import HttpUser, task, between
import boto3



class Sampletest(HttpUser):
    wait_time = between(0.5, 1)

    @task
    def suspended_patient_status(self):
        api_key = boto3.client('ssm').get_parameter(Name='/repo/dev/user-input/api-keys/pds-adaptor/e2e-test', WithDecryption=True)
        self.client.headers = {"Authorization": "API_KEY_HERE"}
        self.client.get("/suspended-patient-status/9693797523")