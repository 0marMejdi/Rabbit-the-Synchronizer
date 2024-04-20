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
ENV DB_PORT=3306
ENV DB_USER_NAME=root
ENV RABBIT_PORT=5672
CMD java -DDB_HOST=$DB_HOST -DRABBIT_HOST=$RABBIT_HOST -DRABBIT_PORT=$RABBIT_PORT -DDB_PASSWORD=$DB_PASSWORD -DDB_USER_NAME=$DB_USER_NAME -DDB_PORT=$DB_PORT -cp "../lib/*:." App