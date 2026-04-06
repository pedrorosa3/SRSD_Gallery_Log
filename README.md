# srsd_project


## 🛠 Compile

To compile a Java file and put the binary files generated in target folder, use:

```bash
javac -d target src/models/User.java src/services/LogReadTerminal.java src/services/LogReadFile.java
```

## 🛠 Run tests

```bash
.\logread.ps1 -K secret -S .\src\logs\log2.txt 
.\logread.ps1 -K secret -R -E Alice .\src\logs\log2.txt 
.\logread.ps1 -K secret -R -G Charlie .\src\logs\log2.txt
.\logread.ps1 -K secret -R -E Bob .\src\logs\log2.txt
.\logread.ps1 -K secret -R -E Eve .\src\logs\log2.txt 
.\logread.ps1 -K secret -R -G Diana .\src\logs\log2.txt
```

# LogAppend

To compile:
```bash
dir /s /B *.java > sources.txt
javac @sources.txt
```
To test:
In src/main/java:
```bash
java services.LogAppendTerminal -T 1 -K secret -A -E Fred log1
java services.LogAppendTerminal -T 2 -K secret -A -G Jill log1
java services.LogAppendTerminal -T 3 -K secret -A -E Fred -R 1 log1
java services.LogAppendTerminal -T 4 -K secret -A -G Jill -R 1 log1
```
To force error:
```bash
java services.LogAppendTerminal -T 3 -K secret -L -E Fred -R 1 log1
java services.LogAppendTerminal -T 5 -K secret -L -G Jill -R 2 log1
java services.LogAppendTerminal -T 6 -K secret -L -E Fred log1
```

Batch Mode:
```bash
java services.LogAppendTerminal -B batch.txt
```