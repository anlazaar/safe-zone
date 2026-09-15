FROM jenkins/jenkins:lts

COPY configuration/plugins.txt /usr/share/jenkins/ref/plugins.txt
COPY configuration/jenkins.yaml /var/jenkins_home/casc_configs/jenkins.yaml

RUN jenkins-plugin-cli \
    --plugin-file /usr/share/jenkins/ref/plugins.txt

ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc_configs/jenkins.yaml