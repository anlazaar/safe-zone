FROM jenkins/jenkins:lts

USER root

RUN apt-get update \
    && apt-get install -y docker.io curl \
    && rm -rf /var/lib/apt/lists/*

# Compose V2
RUN mkdir -p /usr/local/lib/docker/cli-plugins \
    && curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
       -o /usr/local/lib/docker/cli-plugins/docker-compose \
    && chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Buildx
RUN mkdir -p /usr/local/lib/docker/cli-plugins \
    && curl -SL https://github.com/docker/buildx/releases/download/v0.29.1/buildx-v0.29.1.linux-amd64 \
       -o /usr/local/lib/docker/cli-plugins/docker-buildx \
    && chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx

ARG DOCKER_GID=125

RUN groupadd -g ${DOCKER_GID} docker-host \
    && usermod -aG docker-host jenkins

USER jenkins

COPY configuration/plugins.txt /usr/share/jenkins/ref/plugins.txt
COPY configuration/jenkins.yaml /var/jenkins_home/casc_configs/jenkins.yaml

RUN jenkins-plugin-cli \
    --plugin-file /usr/share/jenkins/ref/plugins.txt

ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc_configs/jenkins.yaml