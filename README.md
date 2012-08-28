
AIOTrade
========

### Requirement
    
* Java 1.7+ (Java 1.6 is not supported any more)
* JavaFX 2.0
* Maven 2.x/3.x
* NetBeans 7.1

### Setting javafx.home property for maven

Make sure you've installed JavaFX 2.0. Then set 'javafx.home' property in your maven settings.xml (.m2/settings.xml) to point to this installation. For example:

    <profiles>
        <profile>
            <id>javafx</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <javafx.home>${user.home}/myapps/javafx-sdk2.1.0-beta</javafx.home>
            </properties>
        </profile>
    </profiles>

### Source code --- Git

    > mkdir aiotrade.git
    > cd aiotrade.git
    > git clone git://github.com/dcaoyuan/aiotrade.git
    > git clone git://github.com/dcaoyuan/circumflex.git
    > git clone git://github.com/dcaoyuan/configgy.git

### Directory Structure:
    +-- aiotrade.git
        +-- aiotrade
        +-- circumflex
        +-- configgy

### Build --- Maven

    > cd aiotrade.git

    # Build opensource aiotrade client
    > cd ../aiotrade
    > mvn clean install  

### Code -> Build -> Run Cycle

    > cd ../aiotrade
    > mvn install  
    > suite/application/target/aiotrade/bin/aiotrade

### Zipped application package:

    ..../aiotrade.git/aiotrade/suite/application/target/platform-application-1.0-SNAPSHOT.zip
