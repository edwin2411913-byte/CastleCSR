#!/bin/bash

if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    echo "✅ Variables de entorno cargadas desde .env"
else
    echo "❌ Archivo .env no encontrado"
    exit 1
fi
