FROM docker.stammgruppe.eu/ubuntu:1

WORKDIR /bass
COPY ./target/BASS.jar .
COPY ./target/lib ./lib
RUN mkdir data
EXPOSE 8455
VOLUME ["/bass/data"]
ENTRYPOINT ["java", "-jar", "BASS.jar"]