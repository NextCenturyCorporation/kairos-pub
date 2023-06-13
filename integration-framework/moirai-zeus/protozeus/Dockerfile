FROM amazonlinux:latest
WORKDIR /usr/src
EXPOSE 8000

RUN yum update -y && \
    yum upgrade -y && \
    yum install -y openssh-server && \
    mkdir /var/run/sshd && \
    echo 'root:root' |chpasswd

RUN sed -ri 's/^#?PermitRootLogin\s+.*/PermitRootLogin yes/' /etc/ssh/sshd_config
RUN sed -ri 's/UsePAM yes/#UsePAM yes/g' /etc/ssh/sshd_config

RUN mkdir /root/.ssh

RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

CMD ["/usr/sbin/sshd", "-D"]

RUN yum update -y && \
    yum upgrade -y
RUN yum -y install python3-pip
RUN pip3 install --upgrade pip
RUN pip3 install ansible==2.9.22
RUN pip3 install boto boto3

RUN yum install -y dos2unix
RUN yum install -y openssl
RUN yum install -y which
RUN yum install -y unzip
RUN yum install -y curl
RUN yum install -y wget
RUN yum install -y tar
RUN yum install -y openssh-clients

RUN curl https://releases.hashicorp.com/terraform/0.13.7/terraform_0.13.7_linux_amd64.zip --output terraform_0.13.7_linux_amd64.zip
RUN unzip -j terraform_0.13.7_linux_amd64.zip && rm terraform_0.13.7_linux_amd64.zip
RUN cp -a /usr/src/terraform /bin/terraform
RUN rm -f /usr/src/terraform

RUN amazon-linux-extras install java-openjdk11
RUN yum install -y awscli
RUN aws configure set region us-east-1 --profile kairos-nextcentury
RUN aws configure set default.region us-east-1

COPY ./src/main/resources/prereqs.sh /usr/src/prereqs.sh
COPY ./src/main/resources/enclave-template.template /usr/src/enclave-template
COPY ./src/main/resources/init.sh /usr/src/init.sh

RUN mkdir /usr/src/enclaves
RUN mkdir /usr/src/enclaves/archived
RUN chmod +x /usr/src/prereqs.sh
RUN chmod +x /usr/src/init.sh

RUN rm ~/.ssh/known_hosts
RUN ssh-keyscan -H control.hippodrome.kairos.nextcentury.com >> ~/.ssh/known_hosts

CMD ["./init.sh"]
