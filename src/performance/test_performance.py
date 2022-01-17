import os, random
from base64 import b64encode

import boto3
from locust import task, between, FastHttpUser

RECORD_ETAG_NAME='recordETag'

def extract_etag(response_data):
    return response_data[RECORD_ETAG_NAME]

def random_gp_ods_code():
    return "PERF" + str(random.randint(10000, 99999))

class PdsAdaptorUser(FastHttpUser):
    wait_time = between(4, 5)
    connection_timeout = 10
    network_timeout = 10
    test_patient_nhs_numbers = [
        '9694180309',
        '9694180317',
        '9694180325',
        '9694180333',
        '9694180341',
        '9694180368',
        '9694180376',
        '9694180384',
        '9694180392',
        '9694180406'
    ]

    @classmethod
    def next_patient_id(cls):
        if len(cls.test_patient_nhs_numbers) == 0:
            raise SystemExit('Ran out of test patient nhs number ids, too many users spawned')
        return cls.test_patient_nhs_numbers.pop()

    def on_start(self):
        self.patient_id = PdsAdaptorUser.next_patient_id()

    @task
    def suspended_patient_status(self):
        status_data = self._get_suspended_patient_status(self.patient_id)
        print('status_data', status_data)

    @task
    def organisation_field_update(self):
        status_data = self._get_suspended_patient_status(self.patient_id)
        last_etag = extract_etag(status_data)

        previous_gp = random_gp_ods_code()
        headers = { "Authorization" : self.generate_api_key() }
        data = { 
            "previousGp": previous_gp, 
            RECORD_ETAG_NAME: last_etag 
        }
        self.client.put(f"/suspended-patient-status/{self.patient_id}", json=data, headers=headers)


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
