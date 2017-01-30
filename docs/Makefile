.PHONY: serve build check install javadoc clean clone push link-check

serve: install javadoc
	@./node_modules/.bin/gitbook serve

build: install javadoc
	@./node_modules/.bin/gitbook build

check: build
	@find _book -name '*.md' | sed 's|^_book/||'

install:
	@yarn
	@./node_modules/.bin/gitbook install .

javadoc:
	@mvn javadoc:aggregate -f ./../pom.xml
	@mkdir -p ./java
	@cp -R ../target/site/apidocs/ ./java

clean:
	@rm -rf _book
	@rm -rf java
	@rm -rf node_modules

clone:
	@rm -rf docs.visallo.org
	@git clone -n git@github.com:v5analytics/docs.visallo.org

push: build clone
	@mv docs.visallo.org/.git _book
	@read -p "Enter commit message: " message && \
		pushd _book && \
		git add -A . && \
		git commit -m "$$message" && \
		git push && \
		popd

link-check:
	linkchecker --check-extern --ignore-url 'http://visallo-dev.*' http://localhost:4000/index.html
