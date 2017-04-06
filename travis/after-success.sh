#!/bin/bash

set -e

if [ "$BUILD_DOCS" ]; then

  if [ -d "docs/_book" ]; then

    # Cloudfront is in preview for the CLI, enable it
    aws configure set preview.cloudfront true

    if [ "$TRAVIS_BRANCH" = "$VERSION_ROOT" ]; then
      # Copy to root => docs.visallo.org/*
      aws s3 sync docs/_book s3://docs-visallo-com --delete --exclude "versions/*"
      # Copy to versioned => docs.visallo.org/versions/$BRANCH
      aws s3 sync s3://docs-visallo-com "s3://docs-visallo-com/versions/$TRAVIS_BRANCH" --delete --exclude "versions/*"
      # Invalidate cloudfront distribution (whole deployment)
      aws cloudfront create-invalidation --distribution-id E1FQ5XMTOW2BH8 --paths "/*"
    else
      # Copy to versioned => docs.visallo.org/versions/$BRANCH
      aws s3 sync docs/_book "s3://docs-visallo-com/versions/$TRAVIS_BRANCH" --delete
      # Invalidate cloudfront distribution (just this version)
      aws cloudfront create-invalidation --distribution-id E1FQ5XMTOW2BH8 --paths "/versions/$TRAVIS_BRANCH/*"
    fi
  fi
else
  test $TRAVIS_REPO_SLUG == "visallo/visallo" && test $TRAVIS_BRANCH == "master" || test 'echo $TRAVIS_BRANCH | grep -Eq /^release-.*$/' && test $TRAVIS_PULL_REQUEST == "false" && echo "<settings><servers><server><id>sonatype-nexus-snapshots</id><username>\${env.DEPLOY_USERNAME}</username><password>\${env.DEPLOY_PASSWORD}</password></server></servers></settings>" > ~/settings.xml
  test -f ~/settings.xml && mvn -B --settings ~/settings.xml -f root/pom.xml deploy
  test -f ~/settings.xml && mvn -B --settings ~/settings.xml -DskipTests deploy
  CREATED_BY="$TRAVIS_REPO_SLUG commit "`git rev-parse --short HEAD`""
  if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then ./travis-request.sh --repo $TRAVIS_DOWNSTREAM_REPO --token $TRAVIS_ACCESS_TOKEN_PRO --by "$CREATED_BY" --branch $TRAVIS_BRANCH; fi 
fi
