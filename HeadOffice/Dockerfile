FROM openjdk:23

# Set the working directory
WORKDIR /ProductManagement

# Copy the compiled classes and libraries
COPY ./lib ./lib
COPY ./src ./src

RUN mkdir ./bin
RUN  javac -cp "lib/*" -d bin src/*.java

WORKDIR /ProductManagement/bin
ENV DB_HOST=172.17.0.1
ENV RABBIT_HOST=172.17.0.1
ENV DB_PASSWORD=kool
ENV DB_PORT=3307
ENV DB_USER_NAME=root
ENV RABBIT_PORT1=5673
ENV RABBIT_PORT2=5674
CMD java -DDB_HOST=$DB_HOST -DRABBIT_HOST=$RABBIT_HOST -DRABBIT_PORT1=$RABBIT_PORT1 -DRABBIT_PORT2=$RABBIT_PORT2 -DDB_PASSWORD=$DB_PASSWORD -DDB_USER_NAME=$DB_USER_NAME -DDB_PORT=$DB_PORT -cp "../lib/*:." App