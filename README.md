# Quarkus Quinoa extension

[![Build](https://github.com/quarkiverse/quarkus-quinoa/workflows/Build/badge.svg)](https://github.com/quarkiverse/quarkus-quinoa/actions?query=workflow%3ABuild)
[![Maven Central](https://img.shields.io/maven-central/v/io.quarkiverse.quinoa/quarkus-quinoa.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.quarkiverse.quinoa/quarkus-quinoa)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-7-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

Quinoa is a Quarkus extension which eases the development, the build and serving of single page apps or web components (built with NodeJS: React, Angular, Vue, Lit, …) alongside Quarkus. It is possible to use it with a Quarkus backend in a single project.

You will be able to do live coding of the backend and frontend together nearly out of the box. In Quarkus dev mode, Quinoa will start the node live coding server provided by the target framework and forward relevant requests to it.

## Getting started

Read the full [Quinoa documentation](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/).

### Prerequisite

* Create or use an existing Quarkus application
* Add the Quinoa extension
* Install NodeJS (https://nodejs.org/) or make sure Quinoa is [configured](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/#package-manager-install) to install it.

### Installation

Create a new Quinoa project (with a base Quinoa starter code):

* With [code.quarkus.io](https://code.quarkus.io/?a=quinoa-bowl&j=17&e=io.quarkiverse.quinoa%3Aquarkus-quinoa)
* With the [Quarkus CLI](https://quarkus.io/guides/cli-tooling):
```bash
quarkus create app quinoa-app -x=io.quarkiverse.quinoa:quarkus-quinoa
```

Then start the live-coding:
```bash
quarkus dev
```

And navigate to http://0.0.0.0:8080/quinoa.html

You could also just add the extension (but you won't get the starter code):

* With the [Quarkus CLI](https://quarkus.io/guides/cli-tooling):
```bash
quarkus ext add io.quarkiverse.quinoa:quarkus-quinoa
```

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ia3andy"><img src="https://avatars.githubusercontent.com/u/2223984?v=4?s=100" width="100px;" alt="Andy Damevin"/><br /><sub><b>Andy Damevin</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=ia3andy" title="Code">💻</a> <a href="#maintenance-ia3andy" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://www.phillip-kruger.com"><img src="https://avatars.githubusercontent.com/u/6836179?v=4?s=100" width="100px;" alt="Phillip Krüger"/><br /><sub><b>Phillip Krüger</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=phillip-kruger" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/rvansa"><img src="https://avatars.githubusercontent.com/u/2167869?v=4?s=100" width="100px;" alt="Radim Vansa"/><br /><sub><b>Radim Vansa</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=rvansa" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/fblan"><img src="https://avatars.githubusercontent.com/u/13745480?v=4?s=100" width="100px;" alt="Blanc Frederic"/><br /><sub><b>Blanc Frederic</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=fblan" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/andyhan"><img src="https://avatars.githubusercontent.com/u/142950?v=4?s=100" width="100px;" alt="andyhan"/><br /><sub><b>andyhan</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=andyhan" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/computerlove"><img src="https://avatars.githubusercontent.com/u/769579?v=4?s=100" width="100px;" alt="Marvin Bredal Lillehaug"/><br /><sub><b>Marvin Bredal Lillehaug</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=computerlove" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/johnaohara"><img src="https://avatars.githubusercontent.com/u/959822?v=4?s=100" width="100px;" alt="John O'Hara"/><br /><sub><b>John O'Hara</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=johnaohara" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
