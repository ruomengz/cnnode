# cnnode

rz2357
Ruomeng Zhang

## Makefile
Make a new project:
```bash
make new
```

Compile the project:
```bash
make build
```

Create executable jar file:
```bash
make jar
```

## Run program
To run `gbnnode.jar`, use jar file:
```bash
java -jar gbnnode.jar <self-port> <peer-port> <window-size> [-d <value-of-n> | -p <value-of-p>]
```

To run `dvnode.jar`, use jar file:
```bash
java -jar dvnode.jar <local-port> <neighbor1-port> <loss-rate-1> <neighbor2-port> <loss-rate-2> ... [last]
```

To run cnnode.java, use jar file:
```bash
java -jar cnnode.jar <local-port> receive <neighbor1-port> <loss-rate-1> <neighbor2-port> <loss-rate-
2> ... <neighborM-port> <loss-rate-M> send <neighbor(M+1)-port> <neighbor(M+2)-
port> ... <neighborN-port> [last]
```

## Features





