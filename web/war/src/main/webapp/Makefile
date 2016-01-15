.PHONY: clean all

all: node_modules/npm-install
	./grunt.sh

node_modules/npm-install: package.json
	npm install
	touch node_modules/npm-install

clean:
	rm -rf node_modules libs jsc css build
