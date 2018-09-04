# NodeManager / GUI-REST-Backend

#### Ablauf: NM bereits ausgerollt, Endpoints auf Pfad `:7474/webapi`
1. POST `/yml-config/{platform:aws|os}` (Call der GUI) 
2. POST `/doc` (Call der GUI) (JSON Beispiel-Graph in #netzwerkmodell in Slack)
3. GET `/doc/{docId}/bootstrap/{platform:aws|os}` (Call der GUI -> run ansible)
4. POST `/parseDhcp` (manuell gerade, eigentlich Call von Ansible) (DHCP JSON in #netzwerkmodell in Slack)
5. GET `/ansiblelog` (Abfrage alle x sec.), wenn Status ist DONE ... 
6. GET `/doc/{docId}` (Call der GUI)

#### Check des Ablaufes auf Success:
1. GET `/yml-config/{platform:aws|os}`: returns Content des Files in /MockFog-IaC/os_example_config.yml oder aws_example_config.yml.
2. GET `/doc/{docId}` oder unter Port 7474 im Neo4j-Browser `match (n) return n` abfragen
3. GET `/yml/{platform:aws|os}`: returns neu kreiertes os_example_vars.yml oder aws_example_vars.yml.
4. Port 7474 im Neo4j-Browser `match (n) return n` abfragen: Wenn LINK Relationship vom docNode zu allen andern Nodes existiert, auf welcher die IP aus dem Ansible Output steht
5. (Log einsehbar) ...

## Backend-Komponente: GUI-REST-Service

*Stand:* Kann Graphdokumente/Knoten/Kanten anlegen/löschen, auflisten, serialisieren, de-serialisieren (JSON), in Neo4j speichern. Der wesentliche Code befindet sich in der Klasse **GraphRes.java**.

Der GUI-REST-Service ist leichtgewichtig (nur Jersey/JAX-RS) und läuft derzeit innerhalb des Neo4j Server-Prozesses, um ein leichteres Testen und Ausführen zu ermöglichen. Er kann mit ein paar Änderungen aber auch leicht eigenständig (z.B. in Catalina oder Jetty) laufen. 
Man kann den Graphen in Neo4j sehen und manipulieren während der GUI-REST-Service läuft. Das ist für das Debugging sehr praktisch.

#### Ausführen als Test-Service in der IDE:
1. Das Projekt als Maven-Projekt importieren.
2. JUnit Test über die Klasse Tests.java durchführen (Es öffnet sich ein kleines Fenster. Wenn man es schließt, dann beendet sich der Service.)

#### Ausführen als Test-Service mit Maven
`mvn clean test` (im Hauptverzeichnis des Projektes - Es öffnet sich ein kleines Fenster. Wenn man es schließt, dann beendet sich der Service.)

#### Packaging mit Maven
`mvn clean package` (im Hauptverzeichnis des Projektes)

Dabei entsteht im target-Ordner ein JAR-Package, was man dann in Neo4j als Plugin verwenden kann. Alternativ bestünde die Möglichkeit, dieses Projekt auch eigenständig auszuführen. Dann sieht man die Neo4j-Browseroberfläche beim Testen aber nicht. 

#### Ausführen als Service-Deployment in Docker (Windows)
`install-docker.bat` (im Hauptverzeichnis des Projektes)

Nun läuft der REST-Service in Docker. Beenden: `docker stop neo`

#### Ausführen als Service-Deployment in Docker (Unix/Linux)
`install-docker.sh` (im Hauptverzeichnis des Projektes)
Docker Daemon vor ausführung des Shell Scripts starten.
Nun läuft der REST-Service in Docker. Beenden: `docker stop neo`

## Frontend-Komponente: JS-GUI
Bei der JS-GUI handelt es sich um statische HTML/CSS/JS Dateien im Verzeichnis **src\main\webapp\static**.

#### Testen der JS-GUI:
Die GUI kann man auf drei verschiedene Arten laden (entweder oder): 

1. Einfach nur die Datei **src/main/webapp/static/index.html** mit dem Browser öffnen.
2. Einen beliebigen Webserver installieren und das Verzeichnis **src/main/webapp/static** auf Port 8080 als statischen Inhalt servieren.
3. Folgende URL aufrufen, um den eingebauten Service zu starten: http://localhost:7474/webapi/initJetty (Hierbei ist zu bedenken, dass dann der static-Ordner aus dem JAR-Package und nicht aus dem o.g. Verziechnis serviert wird. 

Dann diese Adresse aufrufen: http://localhost:8080/index.html (=>GUI fragt nach Bezeichnung für neues Dokument)

### Die Features der JS-GUI sind:

- **Hinzufügen von Nodes** durch das "+" in der Symbolleiste, dann Linksklick auf die Leinwand.
- **Nodes verbinden** via Drag und Drop aus der Mitte heraus.
- **Löschen von Kanten** durch Nutzung des Radiergummis in der Symbolleiste (mit gehaltener linker Maustaste die Verbindungen wegradieren)
- **Löschen ausgewählter Nodes** mit der ENTF-Taste
- **Scrollen/Verschieben** der Leinwand mit gehaltener rechter Maustaste im leeren Bereich
- **Mehrfachauswahl** (Auswahl-Rechteck) mit gehaltener linker Maustaste
- **Auswahlerweiterung** mit gehaltener STRG-Taste (wie in der EDV gewohnt auch mit Auswahl-Rechteck)
- **Zoom** funktioniert mit dem Mausrad, wobei sich der Zoom an der aktuellen Position des Mauszeigers orientiert
- **Umbenennen von Nodes** und setzen weitere Eigenschaften geschieht durch Doppelklick auf die Bezeichnung des Nodes

## Vorteile von Neo4j im Vergleich zu anderen Lösungen
- Cypher (deklaratives Operieren auf dem Graphen) gibt uns die Garantie von Korrektheit (wir müssen uns nicht mit Fehlern in der Datenspeicherungsschickt herumschlagen)
- ACID-Transaktionen auf den Graphdaten ermöglichen das Konsistenthalten von Daten z.B. bei pushing NodeAgents (z.B. Edge-Properties)
- Komplexe Operationen mit einer Zeile als Transaktion
- Authentifizierung von Nutzern
- Serialisieren ganzer Netzwerke nach JSON, YML
- De-serialisieren ganzer Netzwerke von JSON in den Datenspeicher
- Schnelles Testen und Visualisierung des Datenbestandes im laufenden Betrieb
- Speichern von (JSON)Maps in den Nodes und Edges (=> es sind keine starren Modellklassen zu pflegen) 
- Filtern von Nodes und Pfaden anhand von JSON-Properties auf Knoten und Kanten
- Dockerfile für das Deployment (ist bei N4J auch schon dabei)
- Aussagekräftige Fehlerobjekte
- Die schöne Browseroberfläche: http://localhost:7474 (erleichtertes Debugging)
- Direktzugriff auf die Daten an Cypher vorbei möglich 
- Mit Hilfe von OGM bestünde die Möglichkeit, Graphen in vorgefertigte Klassen de-serialiseren zu lassen. 
- Neo4j ist sehr leichtgewichtig und flexibel (muss nichtmal installiert werden)

