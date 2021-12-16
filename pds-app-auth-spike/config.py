import os
import sys

from dataclasses import MISSING, Field, dataclass, fields
from typing import Optional

@dataclass
class Config:
    api_key: Optional[str] = os.getenv('PDS_API_KEY')
    pds_fhir_url: Optional[str] = 'https://int.api.service.nhs.uk/personal-demographics/FHIR/R4/'
    nhs_env: Optional[str] = 'int'
    patient_nhs_number: str = '9692295966'


def read_config_from_env():
    return Config()
