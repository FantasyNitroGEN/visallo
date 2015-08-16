.PHONY: serve build install clean clone push

serve: install
	@gitbook serve

build: install
	@gitbook build

install:
	@npm install gitbook-cli -g

clean:
	@rm -rf _book

clone: clean
	@git clone git@github.com:v5analytics/docs.visallo.org _book

push: clone build
	@read -p "Enter commit message: " message && \
		pushd _book && \
		git add -A . && \
		git commit -m "$$message" && \
		git push && \
		popd
