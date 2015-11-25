.PHONY: serve build check install clean clone push

serve: install
	@./node_modules/.bin/gitbook serve

build: install
	@./node_modules/.bin/gitbook build

check: build
	@find _book -name '*.md' | sed 's|^_book/||'

install:
	@npm install
	@./node_modules/.bin/gitbook install .

clean:
	@rm -rf _book
	@rm -rf node_modules

clone:
	@rm -rf _book
	@git clone git@github.com:v5analytics/docs.visallo.org _book && rm -rf _book/*

push: clone build
	@read -p "Enter commit message: " message && \
		pushd _book && \
		git add -A . && \
		git commit -m "$$message" && \
		git push && \
		popd
