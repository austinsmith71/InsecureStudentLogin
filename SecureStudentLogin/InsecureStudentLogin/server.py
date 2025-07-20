import jwt
import datetime
from flask import Flask, request, jsonify
import secrets
from config import SECRET_KEY, VALID_USERS

app = Flask(__name__)

@app.route('/', methods=['POST'])
def login():
    data = request.get_json(force=True)
    name = data.get("name")
    class_ = data.get("class")
    password = data.get("password")

    if (name, class_, password) in VALID_USERS:
        payload = {
            "sub": name,
            "class": class_,
            "exp": datetime.datetime.utcnow() + datetime.timedelta(hours=1)  # 1 hr expiration
        }
        token = jwt.encode(payload, SECRET_KEY, algorithm="HS256")
        return jsonify(token=token)
    else:
        return jsonify(error="Invalid login"), 401

@app.route('/validate-token', methods=['POST'])
def validate_token():
    auth_header = request.headers.get('Authorization', '')
    if auth_header.startswith('Bearer '):
        token = auth_header[len('Bearer '):]
        try:
            decoded = jwt.decode(token, SECRET_KEY, algorithms=["HS256"])
            return jsonify(valid=True, user=decoded["sub"])
        except jwt.ExpiredSignatureError:
            return jsonify(error="Token expired"), 401
        except jwt.InvalidTokenError:
            return jsonify(error="Invalid token"), 401
    return jsonify(error="Missing token"), 401


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=9999, ssl_context=('cert.pem', 'key.pem'))
