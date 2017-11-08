FROM geco/oracle-java8

WORKDIR /bass
COPY ./target/BASS.jar .
COPY ./target/lib ./lib
RUN mkdir data
VOLUME ["/bass/data"]
ENTRYPOINT ["java", "-jar", "BASS.jar"]