<div align="center">

# 🖼️ SRSD Gallery Log

**Registo seguro de entradas e saídas numa galeria de arte**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Build](https://img.shields.io/badge/build-Makefile-blue)
![Crypto](https://img.shields.io/badge/crypto-AES--CBC%20%2B%20HMAC--SHA256-green)

📅 **Abril de 2026** · Projeto da unidade curricular de **Segurança de Redes e Sistemas Distribuídos (SRSD)** · Ciências ULisboa, 2025/2026

_Inspirado na competição [Build It, Break It, Fix It](https://builditbreakit.org/)_

</div>

---

## 📖 Sobre o projeto

O **SRSD Gallery Log** é composto por duas ferramentas de linha de comandos que permitem manter um registo (log) **cifrado e com integridade verificável** dos movimentos de funcionários e visitantes numa galeria de arte:

| Ferramenta | Função |
|---|---|
| **`logappend`** | Acrescenta eventos ao log (entrada/saída da galeria ou de uma sala) |
| **`logread`** | Consulta o log: estado atual da galeria, percurso de uma pessoa e salas partilhadas |

### 🔐 Segurança

- Cada entrada é cifrada com **AES-128-CBC** (IV aleatório por linha).
- As chaves de cifra e de MAC são derivadas do token (`-K`) via **SHA-256**.
- Cada linha inclui um **HMAC-SHA256 encadeado** com o MAC da linha anterior, pelo que qualquer alteração, remoção ou reordenação de linhas é detetada.
- Uma base de dados auxiliar cifrada (`repository/<log>_db`) guarda o estado atual para acelerar as operações; se estiver em falta ou corrompida, é reconstruída a partir do log.
- Token errado ou log adulterado: o `logappend` imprime `invalid` e o `logread` imprime `integrity violation` (ambos com código de saída `111`).

---

## 👥 Autores

| Autor | GitHub |
|---|---|
| Pedro Rosa | [@pedrorosa3](https://github.com/pedrorosa3) |
| Eduardo Sampaio | [@eduhbao-og](https://github.com/eduhbao-og) |
| Rodrigo Bray | [@RBray9](https://github.com/RBray9) |

---

## ⚙️ Requisitos

- **Java JDK 21** (ou superior) — `javac` e `java` no `PATH`
- **GNU Make** _(opcional)_ — em Windows pode usar-se Git Bash, WSL ou os comandos `java` diretos indicados abaixo

---

## 🚀 Como executar

### 1. Compilar

```bash
make            # ou: make compile
```

As classes compiladas ficam em `target/classes`. Para limpar:

```bash
make clean
```

<details>
<summary><b>Sem Make (compilação manual)</b></summary>

```bash
mkdir -p target/classes
javac -d target/classes $(find src/main/java -name "*.java")

# depois:
java -cp target/classes services.LogAppendTerminal <args>
java -cp target/classes services.LogReadTerminal  <args>
```

</details>

### 2. `logappend` — registar eventos

```bash
make run-append ARGS="-T <timestamp> -K <token> (-E <funcionário> | -G <visitante>) (-A | -L) [-R <sala>] <log>"
```

| Opção | Significado |
|---|---|
| `-T` | Timestamp (inteiro positivo, estritamente crescente) |
| `-K` | Token/chave de autenticação (alfanumérico) |
| `-E` / `-G` | Nome do funcionário (_employee_) / visitante (_guest_) |
| `-A` / `-L` | Chegada (_arrival_) / saída (_leave_) |
| `-R` | Número da sala (omitido = entrada/saída da galeria) |
| `<log>` | Caminho do ficheiro de log (criado se não existir) |

**Exemplo:**

```bash
make run-append ARGS="-T 1 -K secret -A -E Alice logteste"         # Alice entra na galeria
make run-append ARGS="-T 2 -K secret -A -G Bob logteste"           # Bob entra na galeria
make run-append ARGS="-T 3 -K secret -A -E Alice -R 1 logteste"    # Alice entra na sala 1
make run-append ARGS="-T 4 -K secret -A -G Bob -R 1 logteste"      # Bob entra na sala 1
make run-append ARGS="-T 5 -K secret -L -E Alice -R 1 logteste"    # Alice sai da sala 1
make run-append ARGS="-T 6 -K secret -L -E Alice logteste"         # Alice sai da galeria
```

**Casos que devem falhar** (`invalid`, código `111`):

```bash
make run-append ARGS="-T 2 -K secret -A -G Bob logteste"           # timestamp no passado
make run-append ARGS="-T 7 -K secret -L -G Bob -R 5 logteste"      # Bob não está na sala 5
make run-append ARGS="-T 8 -K secret -A -E Carol -R 2 logteste"    # Carol não entrou na galeria
make run-append ARGS="-T 9 -K passwordFalsa -A -E Alice logteste"  # token errado
```

#### 📦 Modo batch

Executa vários comandos `logappend` a partir de um ficheiro (um comando por linha, sem o nome do programa). Linhas inválidas imprimem `invalid` e o processamento continua.

```bash
make run-append ARGS="-B batch.txt"
```

Exemplo de [batch.txt](batch.txt):

```text
-K secret -T 1 -A -E Charlie log_lote
-K secret -T 2 -A -G Dave log_lote
-K secret -T 3 -A -R 5 -E Charlie log_lote
...
```

### 3. `logread` — consultar o log

```bash
make run-read ARGS="-K <token> (-S | -R (-E|-G) <nome> | -I (-E|-G) <nome> (-E|-G) <nome> ...) <log>"
```

| Opção | Resultado |
|---|---|
| `-S` | Estado atual: funcionários e visitantes presentes, e ocupantes de cada sala |
| `-R` | Lista ordenada das salas visitadas por uma pessoa |
| `-I` | Salas onde todas as pessoas indicadas estiveram ao mesmo tempo |

**Exemplo** (após correr o `batch.txt`):

```console
$ make run-read ARGS="-K secret -S log_lote"

Dave
20: Dave

$ make run-read ARGS="-K secret -R -G Dave log_lote"
10,20

$ make run-read ARGS="-K secret -I -E Charlie -G Dave log_lote"
10
```

> No modo `-S`, a 1.ª linha lista os funcionários, a 2.ª os visitantes, e as seguintes `sala: ocupantes` (tudo por ordem alfabética/numérica).

**Erros:**

```console
$ make run-read ARGS="-K tokenErrado -S log_lote"
integrity violation          # código 111 (também para logs adulterados)

$ make run-read ARGS="-K secret -R -E Ninguem log_lote"
                             # pessoa inexistente: não imprime nada, código 0
```

---

## 🗂️ Estrutura do projeto

```text
SRSD_Gallery_Log/
├── Makefile
├── batch.txt                    # exemplo para o modo batch
├── log_chaos, log_exhibition,   # logs de exemplo
│   log_sweep
└── src/main/java/
    ├── services/                # pontos de entrada (LogAppendTerminal, LogReadTerminal) e lógica de leitura
    ├── crypto/                  # cifra AES, HMAC encadeado e verificação de integridade
    ├── repository/              # base de dados auxiliar cifrada
    ├── models/                  # LogEntry, User, DBEntry, ...
    └── Utils/                   # parsing de argumentos e utilitários
```

---

<div align="center">
<sub>SRSD · Abril 2026</sub>
</div>
