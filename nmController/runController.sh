#!/bin/bash
# Runs on Port 5001

controllerdir=/opt/MockFog/NodeManager/nmController
echo "[Nodemanager Controller] starting Flask Service from $controllerdir..."
pkill -f flask
export FLASK_APP=$controllerdir/controller.py
python -m flask run --host=0.0.0.0 --port=5001

