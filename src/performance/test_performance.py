from base64 import b64encode
from locust import HttpUser, task, between
import boto3, os

class Sampletest(HttpUser):
    wait_time = between(0.5, 1)

    @task
    def suspended_patient_status(self):
        self.client.headers = {"Authorization": self.generate_api_key()}
        self.client.get("/suspended-patient-status/9693797523")

    def generate_api_key(self):
        env = os.environ["NHS_ENVIRONMENT"]
        ssm_parameter_name = f"/repo/{env}/user-input/api-keys/pds-adaptor/e2e-test"
        boto_response = boto3.client('ssm').get_parameter(Name=ssm_parameter_name, WithDecryption=True)
        username = "e2e-test"
        password = boto_response["Parameter"]["Value"]
        return b64encode(f"{username}:{password}".encode("utf8")).decode("ascii")