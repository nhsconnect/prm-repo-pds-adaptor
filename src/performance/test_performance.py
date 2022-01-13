from base64 import b64encode
from locust import HttpUser, task, between
import boto3, os

class Sampletest(HttpUser):
    wait_time = between(0.5, 1)

    @task
    def suspended_patient_status(self):
        patient_id = "9693797523"
        self.client.headers = {"Authorization": self.generate_api_key()}
        self.client.get(f"/suspended-patient-status/{patient_id}")

    @task
    def organisation_field_update(self):
        patient_id = "9693797523"
        previous_gp = "Old GP"
        record_e_tag = "An ETag"
        self.client.headers = {"Authorization": self.generate_api_key()}
        data = { "previousGp": previous_gp, "recordETag": record_e_tag }
        self.client.put(url=f"/suspended-patient-status/{patient_id}", data=data)

    def generate_api_key(self):
        env = os.environ["NHS_ENVIRONMENT"]
        ssm_parameter_name = f"/repo/{env}/user-input/api-keys/pds-adaptor/e2e-test"
        boto_response = boto3.client('ssm').get_parameter(Name=ssm_parameter_name, WithDecryption=True)
        username = "e2e-test"
        password = boto_response["Parameter"]["Value"]
        return b64encode(f"{username}:{password}".encode("utf8")).decode("ascii")
