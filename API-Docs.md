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
  "type": "player/current",
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
    "uri": "https://youtube.com/watch?v=abcdef"
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

Update on track
```json
{
  "method": "update",
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