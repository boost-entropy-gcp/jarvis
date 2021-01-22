#!/bin/bash

echo "Setting permissions..."

function bindmount() {
    if [ -d "$1" ]; then
        mkdir -p "$2"
    fi
    mount --bind $3 "$1" "$2"
}

mkir /data/in

mkir /home/sftp-user/in
mkir /home/client-user/in

chown -R :users /data/in

bindmount /data/in /home/sftp-user/in
bindmount /data/in /home/client-user/in
