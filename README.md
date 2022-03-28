# Quarkus Quinoa extension

[![Build](https://github.com/quarkiverse/quarkus-quinoa/workflows/Build/badge.svg)](https://github.com/quarkiverse/quarkus-quinoa/actions?query=workflow%3ABuild)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Quinoa is a Quarkus extension which eases the development, the build and serving of single page apps (built with NodeJS: React, Angular, …) alongside Quarkus . It is possible to use it with a Quarkus backend in a single project.

## Setup

Some setup is needed to compile this project. This extension use node package manager (npm) that 
needs to be installed.
The Angular tests also uses a headless Chrome browser, so you might need to install Chromium or Chrome.


### Fedora

* Install Chromium

`sudo dnf install chromium`

You also need to make use the 'CHROME_BIN' var is set:

`export CHROME_BIN='/usr/bin/chromium-browser'`

(You can get the value by doing `type chromium-browser`)

You can make the above permanent by adding it to your `~/.bashrc` file.

* Install npm:

`sudo dnf install npm`

* Install yarn:

`sudo npm install --global yarn`