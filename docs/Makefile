.PHONY: serve build install clean clone push

serve: install
	@./node_modules/.bin/gitbook serve

build: install
	@./node_modules/.bin/gitbook build

install:
	@npm install
	@./node_modules/.bin/gitbook install .

clean:
	@rm -rf _book

clone: clean
	@git clone git@github.com:v5analytics/docs.visallo.org _book && rm -rf _book/*

push: clone build
	@read -p "Enter commit message: " message && \
		pushd _book && \
		git add -A . && \
		git commit -m "$$message" && \
		git push && \
		popd
