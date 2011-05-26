
AIOTrade
========


## Source code --- Git

    > mkdir aiotrade.git
    > cd aiotrade.git
    > git clone git://github.com/dcaoyuan/aiotrade.git
    > git clone git://github.com/dcaoyuan/circumflex.git
    > git clone git://github.com/dcaoyuan/configgy.git

## Directory Structure:
    +-- aiotrade.git
        +-- aiotrade
        +-- circumflex
        +-- configgy

## Build --- Maven

    > cd aiotrade.git

    # Build opensource aiotrade client
    > cd ../aiotrade
    > mvn clean install  

## Code -> Build -> Run Cycle

    > cd ../aiotrade
    > mvn install  
    > suite/application/target/aiotrade/bin/aiotrade

## Zipped application package:

    ..../aiotrade.git/aiotrade/suite/application/target/platform-application-1.0-SNAPSHOT.zip
