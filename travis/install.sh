#!/bin/bash

set -e

if [ "$BUILD_DOCS" ]; then
  if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
    if [[ $VERSION_LIST =~ (^|[[:space:]])$TRAVIS_BRANCH($|[[:space:]]) ]]; then
      rm -rf ~/.nvm 
      git clone https://github.com/creationix/nvm.git ~/.nvm
      (cd ~/.nvm && git checkout `git describe --abbrev=0 --tags`)
      source ~/.nvm/nvm.sh
      nvm install 6
      node --version
      npm install -g yarn
      if [ -f "awscli-bundle/install" ]; then
        echo "awscli exists"
      else 
        echo "Downloading AWS..."
        curl https://s3.amazonaws.com/aws-cli/awscli-bundle.zip -O
        unzip awscli-bundle.zip
      fi
      ./awscli-bundle/install -b $HOME/bin/aws
    fi
  fi
fi
