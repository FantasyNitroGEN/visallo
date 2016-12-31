#!/usr/bin/env bash

echo "SETTING UP SCRIPT VARIABLES"
JAVA_VERSION=8u111-b14
MAVEN_VERSION=3.3.9
NODE_VERSION=0.10.35
INTELLIJ_EDITION=ultimate
USERNAME=visallo
#USER_PASSWORD=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
USER_PASSWORD=changeit

usage ()
{
    echo "Usage: setup-centos.sh -p user_password [-u]"
    echo "  options:"
    echo "      -p required     Password for the visallo user account you'll login using"
    echo "      -i              IntelliJ IDEA edition to install (community or ultimate)"
}

echo "PARSING SCRIPT ARGUMENTS"
while getopts "p:i" opt; do
  case $opt in
    p)
      USER_PASSWORD=$OPTARG
      ;;
    i)
      INTELLIJ_EDITION=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      usage
      exit 1
      ;;
  esac
done

cd /opt

echo "SETTING UP HOME DIRECTORY VOLUME"
DEVICE=$(lsblk | tail -1 | tr -s ' ' | cut -d ' ' -f 1)
if [ "$(file -s /dev/$DEVICE)" = "/dev/${DEVICE}: data" ]
then
    mkfs -t ext4 /dev/$DEVICE
    mkdir /home/$USERNAME
    mount /dev/$DEVICE /home/$USERNAME
    cp /etc/fstab /etc/fstab.orig
    UUID=$(ls -al /dev/disk/by-uuid/ | grep $DEVICE | tr -s ' ' | cut -d ' ' -f 9)
    echo -e "UUID=${UUID} /home/$USERNAME\text4\tdefaults,nofail\t0 2" >> /etc/fstab
fi

echo "ADDING VISALLO USER ACCOUNT"
adduser $USERNAME
usermod -aG wheel $USERNAME
echo "$USERNAME:$USER_PASSWORD" | chpasswd

echo "ADDING DOCKER YUM REPO"
tee /etc/yum.repos.d/docker.repo <<-'EOF'
[dockerrepo]
name=Docker Repository
baseurl=https://yum.dockerproject.org/repo/main/centos/7/
enabled=1
gpgcheck=1
gpgkey=https://yum.dockerproject.org/gpg
EOF

echo "ADDING V5 ANALYTICS YUM REPO"
tee /etc/yum.repos.d/v5analytics.repo <<-'EOF'
[v5analytics]
name=rpms
baseurl=https://mvn.visallo.com/content/repositories/rpms
enabled=1
protect=0
gpgcheck=0
metadata_expire=30s
autorefresh=1
type=rpm-md
EOF

echo "UPDATING YUM PACKAGES"
yum -y update
yum install -y epel-release bzip2 git

echo "INSTALLING DOCKER"
yum -y install docker-engine
systemctl enable docker.service
systemctl start docker

echo "INSTALLING V5 YUM PACKAGES"
yum install -y v5analytics-ffmpeg

echo "INSTALLING JAVA/JDK"
cd /opt
curl -H 'Cookie: oraclelicense=accept-securebackup-cookie' -f -s -L http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION}/jdk-$(echo ${JAVA_VERSION} | sed -e 's/-.*//')-linux-x64.tar.gz | tar -xzf -
ln -s $(echo ${JAVA_VERSION} | sed -e 's/\(.*\)u\(.*\)-\(.*\)/jdk1.\1.0_\2/') jdk
echo 'export _JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true' >> "/home/$USERNAME/.bash_profile"
echo 'export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8' >> "/home/$USERNAME/.bash_profile"
echo 'export JAVA_HOME=/opt/jdk' >> "/home/$USERNAME/.bash_profile"

echo "INSTALLING MAVEN"
cd /opt
curl -f -s -L http://archive.apache.org/dist/maven/maven-$(echo ${MAVEN_VERSION} | awk -F . '{print $1}')/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar -xzf -
ln -s apache-maven-${MAVEN_VERSION} maven
echo 'export M2_HOME=/opt/maven' >> "/home/$USERNAME/.bash_profile"
echo 'export M2=$M2_HOME/bin' >> "/home/$USERNAME/.bash_profile"

echo "INSTALLING NODEJS"
cd /opt
curl -f -s -L http://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.gz | tar -xzf -
ln -s node-v${NODE_VERSION}-linux-x64 node
echo 'export NODE_HOME=/opt/node' >> "/home/$USERNAME/.bash_profile"
/opt/node/bin/npm install -g inherits bower grunt grunt-cli

echo "INSTALLING INTELLIJ IDEA IDE"
cd /opt
if [ "$INTELLIJ_EDITION" = "ultimate" ]
then
    curl -fsSLo "intellij.tar.gz" "https://data.services.jetbrains.com/products/download?code=IIU&platform=linuxWithoutJDK"
    echo "[Desktop Entry]" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "Version=1.0" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "Type=Application" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "Name=IntelliJ IDEA" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "Icon=/opt/idea/bin/idea.png" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo 'Exec="/opt/idea/bin/idea.sh" %f' >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "Comment=The Drive to Develop" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "Categories=Development;IDE;" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "Terminal=false" >> /usr/local/share/applications/jetbrains-idea.desktop
    echo "StartupWMClass=jetbrains-idea" >> /usr/local/share/applications/jetbrains-idea.desktop
else
    curl -fsSLo "intellij.tar.gz" "https://data.services.jetbrains.com/products/download?code=IIC&platform=linuxWithoutJDK"
    echo "[Desktop Entry]" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "Version=1.0" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "Type=Application" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "Name=IntelliJ IDEA Community Edition" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "Icon=/opt/idea/bin/idea.png" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo 'Exec="/opt/idea/bin/idea.sh" %f' >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "Comment=The Drive to Develop" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "Categories=Development;IDE;" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "Terminal=false" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
    echo "StartupWMClass=jetbrains-idea-ce" >> /usr/local/share/applications/jetbrains-idea-ce.desktop
fi
tar xzf intellij.tar.gz
IDEA_DIR=$(find /opt -type d -name "idea*")
ln -s ${IDEA_DIR#/opt/} idea
rm -f intellij.tar.gz

echo "SETTING PATH"
echo 'export PATH=$JAVA_HOME/bin:$M2:$NODE_HOME/bin:$PATH' >> "/home/$USERNAME/.bash_profile"

echo "INSTALLING THE GNOME DESKTOP"
yum -y groups install "GNOME Desktop"

echo "INSTALLING XRDP"
yum -y install xrdp tigervnc-server
echo "X-GNOME-Autostart-enabled=false" | tee -a /etc/xdg/autostart/gnome-software-service.desktop
systemctl start xrdp.service
systemctl enable xrdp.service
