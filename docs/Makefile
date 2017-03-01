###
### Generate Book Makefile
###
### make – Start server
### make push – Publishes book
###

.DEFAULT_GOAL: serve

PLUGINS=ga theme-visallo github-embed # Should match plugins in book.js

WEBAPP=../web/war/src/main/webapp
GITBOOK=node_modules/gitbook-cli/bin/gitbook.js
GITBOOK_PLUGINS:=$(patsubst %,node_modules/gitbook-plugin-%/package.json, $(PLUGINS))

# All java files in project
JAVA_FILES=$(shell find ../ -type f -name '*.java')

# All js, jsdoc, and jsx files
JS_DIR=$(WEBAPP)/js
JS_FILES=$(shell find \$(WEBAPP)/js -type f -type f \( \( -iname "*.js" -o -iname "*.jsdoc" \) -o -iname "*.jsx" \))

# All files except the output _book and node_modules
BOOK_FILES=$(shell find . \( -path ./node_modules -o -path ./_book \) -prune -o -print)

# jsdoc executable in webapp
WEBAPP_JSDOC=$(WEBAPP)/node_modules/jsdoc/jsdoc.js

###
### Generated File Tasks
###

java/index.html: $(JAVA_FILES)
	@mvn javadoc:aggregate --quiet -f ./../pom.xml
	@mkdir -p "$(@D)"
	@cp -R ../target/site/apidocs/ ./java

$(WEBAPP_JSDOC):
	@cd $(WEBAPP) && yarn

javascript/index.html: $(WEBAPP_JSDOC) $(JS_DIR) $(JS_FILES) $(WEBAPP)/README.md
	@cd $(WEBAPP) && yarn doc
	@mkdir -p "$(@D)"
	@cp -R $(WEBAPP)/build/jsdoc/ ./javascript

$(GITBOOK): package.json yarn.lock
	@yarn install
	@touch $(GITBOOK)

$(GITBOOK_PLUGINS): $(GITBOOK) book.js gitbook_install
	@touch "$@"

_book/index.html: $(BOOK_FILES) $(GITBOOK_PLUGINS)
	@rm -rf _book
	@$(GITBOOK) build .

###
### Phony Tasks
###

.PHONY: javadoc jsdoc gitbook gitbook_install build serve check clean clone push link-check

javadoc: java/index.html

jsdoc: javascript/index.html

gitbook_install:
	@$(GITBOOK) install .

gitbook: $(GITBOOK) $(GITBOOK_PLUGINS)

build: _book/index.html javadoc jsdoc

serve: gitbook javadoc jsdoc
	@rm -rf _book
	@$(GITBOOK) serve

check: build
	@find _book -name '*.md' | sed 's|^_book/||'

clean:
	@rm -rf _book
	@rm -rf java
	@rm -rf javascript
	@rm -rf node_modules

clone:
	@rm -rf docs.visallo.org
	@git clone -n https://github.com/visallo/docs.visallo.org.git

push: build clone
	@mv docs.visallo.org/.git _book
	@read -p "Enter commit message: " message && \
		pushd _book && \
		git add -A . && \
		git commit -m "$$message" && \
		git push && \
		popd

link-check:
	./node_modules/.bin/blc http://localhost:4000/index.html -roe --exclude "java/"
