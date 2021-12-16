import requests

from config import read_config_from_env
from create_headers import create_headers


def get_patient(config):
    print('Requesting patient details...')

    r = requests.get(
        f"{config.pds_fhir_url}/Patient/{config.patient_nhs_number}",
        headers=create_headers(config)
    )
    print('Patient details response', r.status_code, r.headers)
    version = r.headers['ETag']
    with open('last_get.json', 'w') as f:
        f.write(r.text)
    return version


if __name__ == "__main__":
    get_patient(read_config_from_env())
