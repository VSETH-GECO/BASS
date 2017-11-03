# BASS
Byro Audio Speaker System, short BASS, is a "simple" audio player used to manage the speakers in our office.
Since it's only controllable over web socket, it's only useful in combination with a control interface such as our [Web Interface](https://github.com/VSETH-GECO/BASSctrl).

## Why
Because we had the problem that only one person could control the speakers in our office at the same time and everyone else had to listen to their stuff.
With BASS you can "drop" your music into a web interface and everyone who is connected can vote on the tracks in the playlist.

## Supported formats
To see which audio formats are supported go take a look at [lavaplayer](https://github.com/sedmelluq/lavaplayer#supported-formats) since we are using it to fetch the audio streams.

## Running
To start BASS, simply run:
```
java -jar bass.jar
```
Then you can connect to it via web socket on port `8445` using our [Web Interface](https://github.com/VSETH-GECO/BASSctrl)
or you can implement your own control interface using the [API](/API-Docs.md).

## Configuration
Coming soon...

## API
[Docs](/API-Docs.md)

## Nginx setup
This is for enabling wss - [Config file](/nginx.conf)