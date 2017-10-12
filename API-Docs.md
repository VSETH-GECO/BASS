# API Documentation

## Player

### Control
Play
```json
{
  "method": "update",
  "type": "player/control/play",
  "data": null
}
```

Pause
```json
{
  "method": "update",
  "type": "player/control/pause",
  "data": null
}
```

### Track
Get current track
```json
{
  "method": "retrieve",
  "type": "player/all",
  "data": null
}
```

## Queue
Get current queue
```json
{
  "method": "retrieve",
  "type": "queue/all",
  "data": null
}
```

Request new track
```json
{
  "method": "create",
  "type": "queue/uri",
  "data": {
    "uri": "https://youtube.com/watch?abcdef"
  }
}
```

Vote on track
```json
{
  "method": "update",
  "type": "queue/vote",
  "data": {
    "id": 1000000,
    "vote": 1
  }
}
```
