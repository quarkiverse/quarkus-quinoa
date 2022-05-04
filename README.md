# Quarkus Quinoa extension

[![Build](https://github.com/quarkiverse/quarkus-quinoa/workflows/Build/badge.svg)](https://github.com/quarkiverse/quarkus-quinoa/actions?query=workflow%3ABuild)
[![Maven Central](https://img.shields.io/maven-central/v/io.quarkiverse.quinoa/quarkus-quinoa.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.quarkiverse.quinoa/quarkus-quinoa)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

Quinoa is a Quarkus extension which eases the development, the build and serving of single page apps or web components (built with NodeJS: React, Angular, Vue, Lit, …) alongside Quarkus. It is possible to use it with a Quarkus backend in a single project.

You will be able to do live coding of the backend and frontend together nearly out of the box. In Quarkus dev mode, Quinoa will start the node live coding server provided by the target framework and forward relevant requests to it.

## User Doc

https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/index.html

## Setup to contribute

Some setup is needed to compile this project. This extension use node package manager (npm) that 
needs to be installed.

### Fedora

* Install npm:

`sudo dnf install npm`

* Install yarn:

`sudo npm install --global yarn`