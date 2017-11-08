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

## Docker
You can also run BASS inside Docker. Until we provide the images needed, you have to build them yourself. First you have to build an ubuntu image with alsa installed (see [here]()) named "geco/ubuntu":
```
wget https://raw.githubusercontent.com/VSETH-GECO/Dockerfiles/master/Ubuntu/Dockerfile && sudo docker build -t geco/ubuntu .
```
 
Then you use this image to build a new image with oracle java 8 installed (see [here]()) named "geco/oracle-java8":
```
wget https://raw.githubusercontent.com/VSETH-GECO/Dockerfiles/master/Oracle-Java8/Dockerfile && sudo docker build -t geco/oracle-java8 .
```

Finally, you can build the docker image for BASS calling this in the root of the repository:
```
mvn clean install && sudo docker build -t geco/bass .
```

After you obtained a working docker image, you have to create a docker volume for BASS to store its log files and database:
```
sudo docker volume create bass
```

Then you can run it via:
```
sudo docker run --rm -d -v bass:/bass/data -p 8455:8455 --device /dev/snd geco/bass
```
Which will map the `bass` volume in the host system to `/bass/data` in the container, where BASS puts its log files and database. Additionally it will expose the port `8455` and expose the audio devices to BASS.
## Configuration
Coming soon...

## API
[Docs](/API-Docs.md)

## Nginx setup
This is for enabling wss - [Config file](/nginx.conf)