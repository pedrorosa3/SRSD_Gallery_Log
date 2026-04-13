# srsd_project


## 🛠 Compile

To compile the project using Maven use:

```bash
mvn clean package
```

## 🛠 Run tests

```bash
.\logread.ps1 -K secret -S .\src\main\java\logs\log2.txt
.\logread.ps1 -K secret -R -E Alice .\src\main\java\logs\log2.txt 
.\logread.ps1 -K secret -R -G Charlie .\src\main\java\logs\log2.txt
.\logread.ps1 -K secret -R -E Bob .\src\main\java\logs\log2.txt
.\logread.ps1 -K secret -R -E Eve .\src\main\java\logs\log2.txt 
.\logread.ps1 -K secret -R -G Diana .\src\main\java\logs\log2.txt
```

# LogAppend

To compile:
```bash
mvn clean package
```
To test:

```bash
.\logappend.ps1 -T 1 -K secret -A -E Fred log1
.\logappend.ps1 -T 2 -K secret -A -G Jill log1
.\logappend.ps1 -T 3 -K secret -A -E Fred -R 1 log1
.\logappend.ps1 -T 4 -K secret -A -E Jill -R 1 log1
```
To force error:
```bash
# 4. Violação de Cronologia: Tentar usar um tempo no passado (Tempo 2 já passou)
.\logappend.ps1 -T 2 -K secret -A -G John log1

# 5. Violação de Estado: Fred tenta sair da Sala 5, mas ele está na Sala 1!
.\logappend.ps1 -T 4 -K secret -L -E Fred -R 5 log1

# 6. Violação de Estado: Jill tenta entrar numa sala sem dizer qual é (falta o -R)
.\logappend.ps1 -T 5 -K secret -A -G Jill log1

# 7. Violação de Integridade: Tentar usar uma password errada no log1
.\logappend.ps1 -T 6 -K passwordFalsa -L -E Fred -R 1 log1
```

Batch Mode:
```bash
.\logappend.ps1 -B src/main/java/batch.txt
```