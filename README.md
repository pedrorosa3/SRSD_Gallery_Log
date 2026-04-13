# srsd_project


## 🛠 Compile

To compile the project using Makefile use:

## 🛠 Run tests
# LogAppend

```bash
make run ARGS="-T 1 -K secret -A -E Alice logteste"
make run ARGS="-T 2 -K secret -A -G Bob logteste"
make run ARGS="-T 3 -K secret -A -E Alice -R 1 logteste"
make run ARGS="-T 4 -K secret -A -G Bob -R 1 logteste"
make run ARGS="-T 5 -K secret -L -E Alice -R 1 logteste"
make run ARGS="-T 6 -K secret -L -E Alice logteste"
```
To force errors:

```bash
# 4. Violação de Cronologia: Tentar usar um tempo no passado (Tempo 2 já passou)
make run ARGS="-T 2 -K secret -A -G Bob logteste"

# 5. Violação de Estado: Bob tenta sair da Sala 5, mas ele está na Sala 1!
make run ARGS="-T 7 -K secret -L -G Bob -R 5 logteste"

# 6. Violação de Estado: Bob tenta entrar numa sala sem dizer qual é (falta o -R)
make run ARGS="-T 8 -K secret -A -G Bob logteste"

# 7. Violação de Integridade: Tentar usar uma password errada no logteste
make run ARGS="-T 9 -K passwordFalsa -A -E Alice logteste
```

Batch mode:
```bash
make run ARGS="-B batch.txt"
```


# LogRead

```bash
make run-read ARGS="-K secret -S logteste"
make run-read ARGS="-K secret -R -E Alice logteste"
make run-read ARGS="-K secret -R -G Bob logteste"
make run-read ARGS="-K secret -I -E Alice -G Bob logteste"
```
