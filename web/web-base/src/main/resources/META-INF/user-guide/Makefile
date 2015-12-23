.PHONY: serve build check install clean glossary

serve: install
	@./node_modules/.bin/gitbook serve

build: install
	@./node_modules/.bin/gitbooktbook build

check: build
	@find _book -name '*.md' | sed 's|^_book/||'_book

install:
	@./install.sh

clean:
	@rm -rf _book

glossary:
	@diff ../../../../../../../docs/GLOSSARY.md ./GLOSSARY.md
