#!/bin/sh
#-------------------------------------
# Hook invoked by the release workflow
#-------------------------------------

# Install pnpm
npm install -g pnpm@~8.6.12

# Install bun
npm install -g bun
