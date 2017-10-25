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
Request change of player state (play/pause)
```json
{
  "method": "patch",
  "type": "player/control",
  "data": {
    "token": "123abc",
    "state": "play"
  }
}
```


### Update
Request update on current player state
```json
{
  "method": "get",
  "type": "player/state",
  "data": null
}
```

On player state change (playing/paused/stopped)
```json
{
  "method": "post",
  "type": "player/control",
  "data": {
    "state": "playing"
  }
}
```

### Track
Request update on current track
```json
{
  "method": "get",
  "type": "player/current",
  "data": null
}
```

## Queue
Request update on current queue
```json
{
  "method": "get",
  "type": "queue/all",
  "data": null
}
```

On update queue
```json
{
  "method": "post",
  "type": "queue/all",
  "data": [
    {
      "uri": 1,
      "userID": "1",
      "titel": "Hadbass from Russia",
      "votes": {
        "userID1": 1,
        "userID2": -1,
        "userID3": 0
      },
      "length": 50000,
      "position": 0
    },
    {
      "another track": "..."
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
    "token": "123abc",
    "uri": "https://youtube.com/watch?v=abcdef"
  }
}
```

Vote on track
```json
{
  "method": "patch",
  "type": "track/vote",
  "data": {
    "token": "123abc",
    "id": 1000000,
    "vote": 1
  }
}
```

On update track
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