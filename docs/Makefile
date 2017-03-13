###
### Generate Book Makefile
###
### make – Start server
###

.DEFAULT_GOAL: serve

LINK_CHECK_PORT      := 9988
# Should match plugins in book.js
PLUGINS              := ga theme-visallo github-embed
WEBAPP               := ../web/war/src/main/webapp
GITBOOK              := node_modules/gitbook-cli/bin/gitbook.js
CURRENT_BRANCH       := $(shell git symbolic-ref HEAD --short)
GITBOOK_PLUGINS      := $(patsubst %,node_modules/gitbook-plugin-%/package.json, $(PLUGINS))
JAVA_FILES           := $(shell find ../ -type f -name '*.java')
JS_DIR               := $(WEBAPP)/js
JS_FILES             := $(shell find $(JS_DIR) -type f \( \( -iname "*.js" -o -iname "*.jsdoc" \) -o -iname "*.jsx" \))
BOOK_FILES           := $(shell find . \( -path ./node_modules -o -path ./_book \) -prune -o -print)
WEBAPP_JSDOC         := $(WEBAPP)/node_modules/jsdoc/jsdoc.js

###
### Generated File Tasks
###

java/index.html: $(JAVA_FILES)
	@mvn javadoc:aggregate --quiet -f ./../pom.xml
	@mkdir -p "$(@D)"
	@cp -R ../target/site/apidocs/. java

$(WEBAPP_JSDOC):
	cd $(WEBAPP); yarn

javascript/index.html: $(WEBAPP_JSDOC) $(JS_DIR) $(JS_FILES) $(WEBAPP)/README.md
	cd $(WEBAPP); yarn doc
	@mkdir -p "$(@D)"
	@cp -R $(WEBAPP)/build/jsdoc/. javascript

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

.PHONY: javadoc jsdoc gitbook gitbook_install build serve check clean link-check

javadoc: java/index.html

jsdoc: javascript/index.html

gitbook_install:
	@$(GITBOOK) install .

gitbook: $(GITBOOK) $(GITBOOK_PLUGINS)

build: jsdoc javadoc _book/index.html

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

link-check: build
	@cd _book; \
		python -m SimpleHTTPServer $(LINK_CHECK_PORT) > /dev/null & \
		PID="$$!"; \
		../node_modules/.bin/blc \
			http://localhost:$(LINK_CHECK_PORT)/index.html \
			--recursive \
			--ordered \
			--exclude-external \
			--exclude "java/"; \
		EXIT="$$?"; \
		kill "$$PID"; \
		exit "$$EXIT"

link-check-external: build
	@cd _book; \
		python -m SimpleHTTPServer $(LINK_CHECK_PORT) > /dev/null & \
		PID="$$!"; \
		../node_modules/.bin/blc \
			http://localhost:$(LINK_CHECK_PORT)/index.html \
			--recursive \
			--ordered \
			--exclude "java/" \
			--exclude "https://myvisalloinstance/" \
			--exclude "http://localhost:8888/" \
			--exclude "http://visallo.org/sample#" \
			--exclude "http://visallo.org/#" ; \
		EXIT="$$?"; \
		kill "$$PID"; \
		exit "$$EXIT"
