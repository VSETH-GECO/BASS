# API Documentation

## Connection
On connect
```json
{
  "method": "post",
  "type": "app/welcome",
  "data": {
    "message": "Welcome to BASS"
  }
}
```


## Player

### Control
Play
```json
{
  "method": "patch",
  "type": "player/control/play",
  "data": null
}
```

Pause
```json
{
  "method": "patch",
  "type": "player/control/pause",
  "data": null
}
```

### Update
Play
```json
{
  "method": "post",
  "type": "player/control/play",
  "data": null
}
```

Pause
```json
{
  "method": "post",
  "type": "player/control/pause",
  "data": null
}
```

### Track
Get current track
```json
{
  "method": "get",
  "type": "player/current",
  "data": null
}
```

## Queue
Get current queue
```json
{
  "method": "get",
  "type": "queue/all",
  "data": null
}
```

Request new track
```json
{
  "method": "post",
  "type": "queue/uri",
  "data": {
    "uri": "https://youtube.com/watch?v=abcdef",
    "userID": "1"
  }
}
```

Vote on track
```json
{
  "method": "patch",
  "type": "track/vote",
  "data": {
    "id": 1000000,
    "vote": 1,
    "userID": "1"
  }
}
```

Update on track
```json
{
  "method": "patch",
  "type": "queue/track",
  "data": {
    "id": 1000000,
    "uri": "",
    "submitterID": 2,
    "title": "Hardbass",
    "vote": 1
  }
}
```