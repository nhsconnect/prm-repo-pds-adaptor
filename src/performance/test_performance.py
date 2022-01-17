from base64 import b64encode
from locust import task, between, FastHttpUser
import boto3, os

RECORD_ETAG_NAME='recordETag'

def extract_etag(response_data):
    return response_data[RECORD_ETAG_NAME]

class PdsAdaptorUser(FastHttpUser):
    wait_time = between(0.5, 1)
    connection_timeout = 10
    network_timeout = 10

    @task
    def suspended_patient_status(self):
        patient_id = "9693797523"

        status_data = self._get_suspended_patient_status(patient_id)
        print('status_data', status_data)

    @task
    def organisation_field_update(self):
        patient_id = "9693797523"

        status_data = self._get_suspended_patient_status(patient_id)
        last_etag = extract_etag(status_data)

        previous_gp = "OldGP"
        headers = { "Authorization" : self.generate_api_key() }
        data = { 
            "previousGp": previous_gp, 
            RECORD_ETAG_NAME: last_etag 
        }
        self.client.put(f"/suspended-patient-status/{patient_id}", json=data, headers=headers)


    def _get_suspended_patient_status(self, patient_id):
        headers = { "Authorization" : self.generate_api_key() }
        response = self.client.get(f"/suspended-patient-status/{patient_id}", headers=headers)

        response_data = response.json()
        print('response_data', response_data)

        return response_data

    def generate_api_key(self):
        api_key_ssm_parameter = self.get_api_key_from_ssm()
        username = "performance-test"
        password = api_key_ssm_parameter["Parameter"]["Value"]
        encoded_key = b64encode(f"{username}:{password}".encode("utf8")).decode("ascii")
        return f"Basic {encoded_key}"

    def get_api_key_from_ssm(self):
        env = os.environ["NHS_ENVIRONMENT"]
        ssm_parameter_name = f"/repo/{env}/user-input/api-keys/pds-adaptor/performance-test"
        return boto3.client('ssm').get_parameter(Name=ssm_parameter_name, WithDecryption=True)
