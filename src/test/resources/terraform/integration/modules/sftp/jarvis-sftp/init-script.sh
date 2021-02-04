#!/bin/bash
sed -i 's/#\?\(PubkeyAuthentication\s*\).*$/\1 no/' /etc/ssh/sshd_config
sed -i 's/#\?\(PasswordAuthentication\s*\).*$/\1 yes/' /etc/ssh/sshd_config

service ssh reload

useradd -m -p sftp-password -s /bin/bash sftp-user
