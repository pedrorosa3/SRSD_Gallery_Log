JAVAC = javac
JAVA = java
SRC = src/main/java
OUT = target/classes
MAIN_APPEND = services.LogAppendTerminal
MAIN_READ = services.LogReadTerminal

# default target
all: compile

# compile all sources
compile:
	mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(shell find $(SRC) -name "*.java")

# run logappend with args (usage: make run ARGS="...")
run-append: compile
	$(JAVA) -cp $(OUT) $(MAIN_APPEND) $(ARGS)

# run logread with args (usage: make run-read ARGS="...")
run-read: compile
	$(JAVA) -cp $(OUT) $(MAIN_READ) $(ARGS)
# clean build
clean:
	rm -rf $(OUT)
