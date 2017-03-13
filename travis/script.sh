#!/bin/bash

set -e

if [ "$BUILD_DOCS" ]; then

  # only build if merging into real branch
  if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then

    # Check if this branch should be built as part of documentation
    if [[ $VERSION_LIST =~ (^|[[:space:]])$TRAVIS_BRANCH($|[[:space:]]) ]]; then
      source ~/.nvm/nvm.sh
      VERSION_CURRENT="$TRAVIS_BRANCH" make -C docs build
    else
      echo "Branch not found in VERSION_LIST for docs, skipping"
      rm -rf docs/_book
    fi
  fi

else
  mvn -B -fae test -Ptest-exclude-native -DlogQuiet
fi
