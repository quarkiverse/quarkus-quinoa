#!/bin/sh
#---------------------------------------------------
# Hook invoked by the Quarkus Ecosystem CI workflow
#---------------------------------------------------

# Install pnpm
npm install -g pnpm@~8.6.12

# Install bun
npm install -g bun

# Ensure browsers are installed
cd ./current-repo/testing
../mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps chromium"
