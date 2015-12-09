.PHONY: serve build check install clean glossary

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

glossary:
	@diff ../../../../../../../docs/GLOSSARY.md ./GLOSSARY.md
