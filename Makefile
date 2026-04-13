JAVAC = javac
JAVA = java
SRC = src/main/java
OUT = target/classes
MAIN = services.LogAppendTerminal

# default target
all: compile

# compile all sources
compile:
	mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(shell find $(SRC) -name "*.java")

# run with args (usage: make run ARGS="...")
run: compile
	$(JAVA) -cp $(OUT) $(MAIN) $(ARGS)

# clean build
clean:
	rm -rf $(OUT)
