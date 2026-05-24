# HR Relay Server Protocol

The Wear OS app sends individual heart rate samples to a configurable HTTP endpoint.

## Endpoint

```
POST /hr
```

## Request

### Headers

```
Content-Type: application/json
```

### Body

```json
{
  "bpm": 72,
  "ts": 1712345678901
}
```

| Field | Type | Description |
|---|---|---|
| `bpm` | integer | Heart rate in beats per minute |
| `ts` | integer | Unix millisecond timestamp (UTC) |

## Response

### Success

```
HTTP/1.1 200 OK
Content-Type: application/json

{"ok": true}
```

### Failure

```
HTTP/1.1 4xx/5xx
```

On any non-2xx response, the app logs a warning and continues. There is no retry logic — missed samples are dropped.

## Rate

Each sample is sent immediately as it arrives from the Health Services sensor callback. Under typical resting conditions expect **0.5–3 POSTs/second**. During exercise this may rise slightly. There is no client-side batching or aggregation.

## Network

The app connects to plain HTTP endpoints on the local network (no TLS required). The watch must be on the same LAN as the server.

## Example Server (minimum)

A minimal Python server that receives and logs heart rate data:

```python
from http.server import HTTPServer, BaseHTTPRequestHandler
import json

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(length))
        print(f"HR: {body['bpm']} bpm at {body['ts']}")
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'{"ok":true}')

HTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
```
