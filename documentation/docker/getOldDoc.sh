#!/bin/bash

if docker pull kvalitetsit/rim-medarbejder-bff-documentation:latest; then
    echo "Copy from old documentation image."
    docker cp $(docker create kvalitetsit/rim-medarbejder-bff-documentation:latest):/usr/share/nginx/html target/old
fi
