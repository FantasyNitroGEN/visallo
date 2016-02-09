.PHONY: clean all test

all: node_modules/npm-install
	node_modules/.bin/grunt

node_modules/npm-install: package.json
	npm install
	touch node_modules/npm-install

clean:
	rm -rf node_modules libs jsc css build

test:
	npm test
