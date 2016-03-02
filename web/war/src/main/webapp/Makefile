.PHONY: clean all test version

NODE_VERSION=$(shell grep '<node.version>' ../../../../../root/pom.xml | awk -F '[<>]' '{print $$3}')
NODEDIR=~/.m2/repository/node/$(NODE_VERSION)/node/
NODE=$(NODEDIR)node
NPM=$(NODEDIR)node $(NODEDIR)node_modules/npm/cli.js

all: node_modules/npm-install
	$(NODE) node_modules/.bin/grunt

node_modules/npm-install: package.json
	$(NPM) install
	touch node_modules/npm-install

clean:
	rm -rf node_modules libs jsc css build

test:
	$(NPM) test

version:
	$(NODE) --version
	$(NPM) --version
