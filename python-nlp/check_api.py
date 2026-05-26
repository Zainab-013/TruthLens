import urllib.request
import json
from analyzer import analyze_text

url = "http://localhost:5000/analyze"
data = {
    "text": "Artificial intelligence is rapidly transforming the global economy and reshaping the future of work. By automating routine tasks, machine learning algorithms allow employees to focus on more complex, creative endeavors. This transition, while promising, also presents significant challenges, particularly regarding job displacement and the need for widespread workforce retraining. To resolve this, governments must step in."
}

# 1. Query the API
req = urllib.request.Request(
    url, 
    data=json.dumps(data).encode("utf-8"), 
    headers={"Content-Type": "application/json"}
)

api_res = None
try:
    with urllib.request.urlopen(req) as response:
        api_res = json.loads(response.read().decode())
        print("API Response Scores:")
        print(api_res["scores"])
except Exception as e:
    print(f"Failed to query local API: {e}")

# 2. Run local analysis
local_res = analyze_text(data["text"])
print("\nLocal Function Scores:")
print(local_res["scores"])
