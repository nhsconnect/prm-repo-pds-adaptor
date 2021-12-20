import os
import sys

from dataclasses import MISSING, Field, dataclass, fields
from typing import Optional

@dataclass
class Config:
    api_key: Optional[str] = os.environ['PDS_API_KEY']
    pds_fhir_url: Optional[str] = 'https://int.api.service.nhs.uk/personal-demographics/FHIR/R4/'
    nhs_env: Optional[str] = 'int'
    patient_nhs_number: str = os.environ['NHS_NUMBER'] 


def read_config_from_env():
    return Config()
