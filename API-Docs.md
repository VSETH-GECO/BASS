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
Play/Pause
```json
{
  "method": "patch",
  "type": "player/control",
  "data": {
    "state": "play"
  }
}
```


### Update
Play/Pause/Stop
```json
{
  "method": "post",
  "type": "player/control",
  "data": {
    "state": "play"
  }
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
  "data": [
    {
      "uri": 1,
      "userID": "1",
      "titel": "Hadbass from Russia",
      "votes": {
        "userID1": 1,
        "userID2": -2
      },
      "length": 50000,
      "position": 0
    }
  ]
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