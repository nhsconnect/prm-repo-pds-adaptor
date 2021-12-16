import time
import jwt
import uuid
import requests
import json

def create_headers(config):
    jwt = generate_auth_jwt(config)

    params = {
        'grant_type': 'client_credentials',
        'client_assertion_type': 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
        'client_assertion': jwt
    }
    access_token_response = requests.post('https://int.api.service.nhs.uk/oauth2/token', params)
    print('access token request response code', access_token_response.status_code)
    auth_response_data = access_token_response.json()
    print('auth response data', auth_response_data)
    access_token = auth_response_data['access_token']
    print('access token', access_token)

    headers = {
        'Authorization': f'Bearer {access_token}',
        'X-Request-ID': str(uuid.uuid4())
    }
    print('create headers', headers)
    return headers


def generate_auth_jwt(config):
    now = int(time.time())
    five_minutes_from_now = now + 300
    jwt_payload_data = {
        'iss': config.api_key,
        'sub': config.api_key,
        'aud': f'https://int.api.service.nhs.uk/oauth2/token',
        'jti': str(uuid.uuid4()),
        'exp': five_minutes_from_now
    }

    token = jwt.encode(
        payload=jwt_payload_data,
        key=read_client_key(),
        algorithm='RS512',
        headers = {
            'kid': 'test-1'
        }
    )

    print('Created new auth token')
    return token

def read_client_key():
    with open('./certs/int/client-key') as f:
        return f.read()