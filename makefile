SOURCE_FILES = \
cnnode/gbnnode.java \
cnnode/dvnode.java \
cnnode/cnnode.java 

# Set your java compiler here:
JAVAC = javac
JFLAGS = -encoding UTF-8
# 用法：
# make new: 在你的工程目录下生成src, bin, res子目录。
# 如果你定义的类包含在某个包里：请自己在src下建立相应的目录层次。
# 最终的目录结构如下：
# ├── a.jar
# ├── bin
# │     └── test
# │             ├── A.class
# │             └── B.class
# ├── makefile
# ├── res
# │     └── doc
# │            └── readme.txt
# └── src
#        └── test
#                ├── A.java
#                └── B.java
# make build: 编译，在bin目录下生成 java classes。
# make clean: 清理编译结果，以便重新编译
# make rebuild: 清理编译结果，重新编译。
# make run: make 之后，可以通过make run查看运行结果。
# make jar: 生成可执行的jar包。
#############下面的内容建议不要修改####################

vpath %.class bin
vpath %.java src

# show help message by default
Default:
	@echo "make new: new project, create src, bin, res dirs."
	@echo "make build: build project."
	@echo "make clean: clear classes generated."
	@echo "make rebuild: rebuild project."
	@echo "make jar: package your project into a executable jar."

build: $(SOURCE_FILES:.java=.class)

%.class: %.java
	$(JAVAC) -cp bin -d bin $(JFLAGS) $<

rebuild: clean build

.PHONY: new clean run jar

new:
	mkdir -pv src bin

clean:
	rm -frv bin/*
jar:
	jar cvfe gbnnode.jar cnnode.gbnnode -C bin . \

	jar cvfe dvnode.jar cnnode.dvnode -C bin . \

	jar cvfe cnnode.jar cnnode.cnnode -C bin .

