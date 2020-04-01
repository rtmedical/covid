#!/bin/sh

# Install Java-8
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
echo oracle-java9-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo apt-get install oracle-java8-installer oracle-java9-installer oracle-java9-set-default

# Install Maven-3
cd ~/Downloads/ 
wget http://apache.mirror.digitalpacific.com.au/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz
cd /opt/ && sudo tar -xzvf ~/Downloads/apache-maven-3.5.2-bin.tar.gz
sudo update-alternatives --install /usr/bin/mvn maven /opt/apache-maven-3.5.2/bin/mvn 1001

# Install Eclipse

cd ~/Downloads/ && wget http://mirror.internode.on.net/pub/eclipse/technology/epp/downloads/release/oxygen/1a/eclipse-java-oxygen-1a-linux-gtk-x86_64.tar.gz
cd /opt/ && sudo tar -xzvf ~/Downloads/eclipse-java-oxygen-1a-linux-gtk-x86_64.tar.gz
sudo chown -R root:root eclipse
sudo update-alternatives --install /usr/bin/eclipse eclipse /opt/eclipse/eclipse 1001
sudo cat <<EOF >> /usr/share/applications/eclipse.desktop
[Desktop Entry]
Name=Eclipse Oxygen
Type=Application
Exec=/opt/eclipse/eclipse
Terminal=false
Icon=/opt/eclipse/icon.xpm
Comment=Integrated Development Environment
NoDisplay=false
Categories=Development;IDE;
Name[en]=Eclipse
EOF

# If you need Kotlin, run the following:
cd ~/Downloads/ 
wget https://github.com/JetBrains/kotlin/releases/download/v1.1.51/kotlin-compiler-1.1.51.zip
cd /opt/ && sudo unzip ~/Downloads/kotlin-compiler-1.1.51.zip
sudo update-alternatives --install /usr/bin/kotlin kotlin /opt/kotlinc/bin/kotlin 1001
sudo update-alternatives --install /usr/bin/kotlinc kotlinc /opt/kotlinc/bin/kotlinc 1001

# If you need Gradle, run the following:
cd ~/Downloads/ 
wget https://downloads.gradle.org/distributions/gradle-4.3-bin.zip
cd /opt/ && sudo unzip ~/Downloads/gradle-4.3-bin.zip
sudo update-alternatives --install /usr/bin/gradle gradle /opt/gradle-4.3/bin/gradle 1001

sudo apt-get install -y maven 