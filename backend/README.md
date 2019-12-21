## Publisher backend

Build:
```bash
mvn clean package
```

Run locally:
```bash
mvn compile exec:java \
    -Dstorage.path=../.storage \
    -Dserver.port=9090 \
    -Dserver.sockets.admin.port=9090
```