import flask
import subprocess
import time
import os
import sys
import json


app = flask.Flask(__name__)

@app.route('/restart/<serviceName>')
def index(serviceName):
   sName = serviceName

   startupScript = ""
   if sName == "neo4j": containerName = "NodeManager"
   elif sName == "swagger": containerName = "swagger"
   elif sName == "nginx": containerName = "frontend"

   else: return flask.Response("service not registered!", mimetype='text/html') 
   
   proc = subprocess.Popen(
      "bash /opt/MockFog/NodeManager/nmController/restartService.sh " + containerName,
      shell=True,
      stdout=subprocess.PIPE,stderr=subprocess.PIPE)

   return flask.Response(pipeStd(proc,html=True), mimetype='text/html') 

@app.route("/")
def test():
        return "Nodemanager Controller is online."

@app.route("/status")
def getServerStatus():
   proc = subprocess.Popen("docker ps",stdout=subprocess.PIPE,stderr=subprocess.PIPE,shell=True)
   return flask.Response(pipeStd(proc,html=True), mimetype='text/html') 

@app.route("/restartNM")
def inner():
   proc = subprocess.Popen("sudo reboot",stdout=subprocess.PIPE,stderr=subprocess.PIPE,shell=True)
   return flask.Response("Nodemanager is rebooting.", mimetype="text/html") 

@app.route("/pingAll")
def pingAllNodes():
   cmd = "/opt/MockFog/NodeManager/nmController/pingshell.sh"
   proc = subprocess.Popen(cmd,
      shell=True,
      stdout=subprocess.PIPE,stderr=subprocess.PIPE,
      universal_newlines=True)
   return flask.Response(pipeStd(proc), mimetype='text/html')

@app.route("/mappingOS")
def listMappingOS():
    file = "/opt/MockFog/NodeManager/files/os_device_to_flavor_map.json"
    data = json.load(open(file))
    return flask.jsonify(data)

@app.route("/getAgentIPs")
def getAgentIPs():
   try:
      agentIPs = json.load(open("/opt/MockFog/iac/agentIPs.json","r"))
      #return flask.Response(agentIPs, mimetype='application/json')
      return flask.jsonify(agentIPs)
   except Exception, e:
      return flask.Response("failed to load agentIPs", mimetype='text/html')

def pipeStd(oProcess,html=False):
   for line in iter(oProcess.stdout.readline,''):
      time.sleep(0.1)
      if html: yield line.rstrip() + '<br/>'
      else: yield line.rstrip()
   for line in iter(oProcess.stderr.readline,''):
      time.sleep(0.1)
      if html: yield line.rstrip() + '<br/>'
      else: yield line.rstrip()

