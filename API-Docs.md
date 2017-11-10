# API Documentation

Every interaction between BASS and it's users works over one standardized json format:
```json
{
  "resource": "",
  "action": "",
  "data": {}
}
```
Whereby `data` can be an arbitrary object or `null`.

## Connection
On connect
```json
{
  "resource": "app",
  "action": "success",
  "data": {
    "apiVersion": "v1"
  }
}
```


## Player

### Control
Request change of player state (playing/paused) \
*login required*
```json
{
  "resource": "player",
  "action": "set",
  "data": {
    "state": "playing"
  }
}
```


### Update
Request update on current player state or track
```json
{
  "resource": "player",
  "action": "get",
  "data": null
}
```
Response:
```json
{
  "resource": "player",
  "action": "data",
  "data": {
    "status": "playing",
    "track": {
      "id": 0,
      "uri": "https://www.youtube.com/watch?v=xEYftmh4wz0",
      "userID": "1",
      "userName": "generic user",
      "title": "Russian Hardbass",
      "voters": [],
      "votes": 0,
      "length": 1642000,
      "position": 648420
    }
  }
}
```
The same package will be broadcasted when the player changes state and/or track with the track possibly being `null`.

## Queue
Request update on current queue
```json
{
  "resource": "queue",
  "action": "get",
  "data": null
}
```

Response:
```json
{
  "resource": "queue",
  "action": "data",
  "data": [{
      "id": 1,
      "uri": "https://www.youtube.com/watch?v=xEYftmh4wz0",
      "userID": "1",
      "userName": "generic user",
      "title": "Really hard russian hardbass",
      "voters": [],
      "votes": 0,
      "length": 1246000,
      "position": 0
    }, {
      "id": 2,
      "uri": "https://www.youtube.com/watch?v=xEYftmh4wz0",
      "userID": "2",
      "userName": "another user",
      "title": "The hardes of russian hardbass",
      "voters": [],
      "votes": 0,
      "length": 1005000,
      "position": 0
    }]
}
```

Request new track \
*login required*
```json
{
  "resource": "queue",
  "action": "uri",
  "data": {
    "uri": "https://youtube.com/watch?v=abcdef"
  }
}
```


## Track
Vote on track, id is the track's id and vote can be 1/-1/0 being a up-/down- or neutral vote. \
*login required*
```json
{
  "resource": "track",
  "action": "vote",
  "data": {
    "id": "1",
    "vote": 1
  }
}
```

## User
Logging in:
```json
{
  "resource": "user",
  "action": "login",
  "data": {
    "username": "generic user",
    "password": "supersecurepassword"
  }
}
```

Logging out:
```json
{
  "resource": "user",
  "action": "logout",
  "data": null
}
```

*All following request require admin rights and being logged in* \
Register a new user:
```json
{
  "resource": "user",
  "action": "register",
  "data": {
    "username": "another user",
    "password": "thesecurestpassword"
  }
}
```

Update a users admin privilege
```json
{
  "resource": "user",
  "action": "setadmin",
  "data": {
    "username": "another user",
    "admin": false
  }
}
```

Delete a user
```json
{
  "resource": "user",
  "action": "delete",
  "data": {
    "username": "another user"
  }
}
```

## Favorites

*all following requests require being logged in* \
Retrieve your favorites:
```json
{
  "resource": "favorites",
  "action": "get",
  "data": null
}
```

Add a favorite to your user
```json
{
  "resource": "favorites",
  "action": "add",
  "data": {
    "uri": "https://www.youtube.com/watch?v=xEYftmh4wz0",
    "title": "Russian hardbass"
  }
}
```

Remove a track from your users favorites:
```json
{
  "resource": "favorites",
  "action": "remove",
  "data": {
    "uri": "https://www.youtube.com/watch?v=xEYftmh4wz0"
  }
}
```

## Responses
Responses to requests that don't require specific data to be sent back will look like this:
```json
{
  "resource": "the resource you requested",
  "action": "success",
  "data": null
}
```

If your request failed for some reason the response will look like this:
```json
{
  "resource": "the resource you requested",
  "action": "error",
  "data": {
    "action": "the action you wanted to perform",
    "message": "human-readable error description"
  }
}
```