import json
import requests

from config import read_config_from_env
from create_headers import create_headers
from get_patient import get_patient


def set_patient_mof(config):
    print('Requesting patient details...')
    version = get_patient(config)
    headers = create_headers(config)

    headers['If-Match'] = version
    headers['Content-Type'] = 'application/json-patch+json'

    patches_json = json.dumps([{
      "op": "add",
      "path": "/managingOrganization",
      "value": {
        "type": "Organization",
        "identifier": {
          "system": "https://fhir.nhs.uk/Id/ods-organization-code",
          "value": "M85019"
        }
      }
    }])
    r = requests.patch(
        f"{config.pds_fhir_url}/Patient/{config.patient_nhs_number}",
        headers=headers,
        data=patches_json
    )
    print('Patient update response', r.status_code, r.headers)
    with open('last_patch.json', 'w') as f:
        f.write(r.text)
    return r.status_code


if __name__ == "__main__":
    set_patient_mof(read_config_from_env())
