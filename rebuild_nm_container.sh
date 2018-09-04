cd /opt/MockFog/NodeManager
sudo docker stop NodeManager
sudo docker build . -t nmimage
sudo docker rm NodeManager -f
sudo docker run -d --name NodeManager -v /opt/MockFog/iac:/opt/MFog-IaC -v /opt/MockFog/NodeManager/files:/opt/MFog-files -p 7474:7474 -p 7687:7687 nmimage
