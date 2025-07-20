from flask import Flask, request

app = Flask(__name__)

@app.route('/', methods=['POST'])
def handle_request():
    message = request.data.decode('utf-8').strip()
    print(message)
    if message == "NAME:Austin&CLASS:APCSP&PASSWORD:123" or message == "NAME:Colt&CLASS:APCSP&PASSWORD:123" or message == "NAME:Victor&CLASS:APCSP&PASSWORD:123":
        return "pass"
    else:
        return "fail"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=9999)
