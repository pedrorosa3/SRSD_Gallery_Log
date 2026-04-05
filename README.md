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

