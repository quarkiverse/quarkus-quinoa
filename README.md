<div align="center">
<img src="https://github.com/quarkiverse/quarkus-quinoa/blob/main/docs/modules/ROOT/assets/images/quarkus.svg" width="67" height="70" ><img src="https://github.com/quarkiverse/quarkus-quinoa/blob/main/docs/modules/ROOT/assets/images/plus-sign.svg" height="70" ><img src="https://github.com/quarkiverse/quarkus-quinoa/blob/main/docs/modules/ROOT/assets/images/npm-logo.svg" height="70" >
 
# Quarkus Quinoa
</div>
<br>

[![Maven Central](https://img.shields.io/maven-central/v/io.quarkiverse.quinoa/quarkus-quinoa.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.quarkiverse.quinoa/quarkus-quinoa)
[![Build](https://github.com/quarkiverse/quarkus-quinoa/actions/workflows/build.yml/badge.svg)](https://github.com/quarkiverse/quarkus-quinoa/actions/workflows/build.yml) 
[![Issues](https://img.shields.io/github/issues/quarkiverse/quarkus-quinoa)](https://github.com/quarkiverse/quarkus-quinoa/issues) 
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-32-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

Quinoa is a Quarkus extension which eases the development, the build and serving of single page apps or web components (built with [npm](https://docs.npmjs.com/) : [React](https://react.dev/learn), [Angular](https://angular.dev/overview), [Vue](https://vuejs.org/guide/introduction.html), [Lit](https://lit.dev/), [Svelte](https://svelte.dev/docs/introduction), [Astro](https://docs.astro.build/en/getting-started/), [SolidJS](https://www.solidjs.com/guides/getting-started) …) alongside [Quarkus](https://quarkus.io/). It is possible to use it with a Quarkus backend in a single project.

You will be able to do live coding of the backend and frontend together nearly out of the box. In Quarkus dev mode, Quinoa will start the node live coding server provided by the target framework and forward relevant requests to it.

## Versioning

There are multiple versions available please check which one for your Quarkus release version.

| Extension Version | Quarkus Version |
| --- | --- |
| ![1.x](https://img.shields.io/maven-central/v/io.quarkiverse.quinoa/quarkus-quinoa?versionPrefix=1.&color=cyan) | [![Quarkus](https://img.shields.io/badge/Quarkus-2.0+-purple.svg)](https://github.com/quarkusio/quarkus/releases/tag/2.16.6.Final) |
| ![2.2.x](https://img.shields.io/maven-central/v/io.quarkiverse.quinoa/quarkus-quinoa?versionPrefix=2.2&color=cyan) | [![Quarkus](https://img.shields.io/badge/Quarkus-3.2+-purple.svg)](https://github.com/quarkusio/quarkus/releases/tag/3.2.0.Final) |
| ![2.3.x](https://img.shields.io/maven-central/v/io.quarkiverse.quinoa/quarkus-quinoa?versionPrefix=2.3&color=cyan) | [![Quarkus](https://img.shields.io/badge/Quarkus-3.8+-purple.svg)](https://github.com/quarkusio/quarkus/releases/tag/3.8.0) |
| ![latest](https://img.shields.io/maven-central/v/io.quarkiverse.quinoa/quarkus-quinoa?&color=cyan) | [![Quarkus](https://img.shields.io/badge/Quarkus-3.15+-purple.svg)](https://github.com/quarkusio/quarkus/releases/tag/3.15.0) |

## Getting started

Read the full [Quinoa documentation](https://docs.quarkiverse.io/quarkus-quinoa/dev/index.html).

### Prerequisite

- Create or use an existing Quarkus application
- Add the Quinoa extension
- Install [NodeJS](https://nodejs.org/) or make sure Quinoa is [configured](https://docs.quarkiverse.io/quarkus-quinoa/dev/advanced-guides.html#package-manager) to install it.

### Installation

Create a new Quinoa project (with a base Quinoa starter code):

- With [code.quarkus.io](https://code.quarkus.io/?a=quinoa-bowl&j=17&e=io.quarkiverse.quinoa%3Aquarkus-quinoa)
- With the [Quarkus CLI](https://quarkus.io/guides/cli-tooling):

```bash
quarkus create app quinoa-app -x=io.quarkiverse.quinoa:quarkus-quinoa
```

Then start the live-coding:

```bash
quarkus dev
```

And navigate to http://0.0.0.0:8080/quinoa.html

You could also just add the extension (but you won't get the starter code):

- With the [Quarkus CLI](https://quarkus.io/guides/cli-tooling):

```bash
quarkus ext add io.quarkiverse.quinoa:quarkus-quinoa
```

## 🧑‍💻 Contributing

- Contribution is the best way to support and get involved in community!
- Please, consult our [Code of Conduct](./CODE_OF_CONDUCT.md) policies for interacting in our community.
- Contributions to `quarkus-quinoa` Please check our [CONTRIBUTING.md](./CONTRIBUTING.md)

### If you have any idea or question 🤷

- [Ask a question](https://github.com/quarkiverse/quarkus-quinoa/discussions)
- [Raise an issue](https://github.com/quarkiverse/quarkus-quinoa/issues)
- [Feature request](https://github.com/quarkiverse/quarkus-quinoa/issues)
- [Code submission](https://github.com/quarkiverse/quarkus-quinoa/pulls)

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ia3andy"><img src="https://avatars.githubusercontent.com/u/2223984?v=4?s=100" width="100px;" alt="Andy Damevin"/><br /><sub><b>Andy Damevin</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=ia3andy" title="Code">💻</a> <a href="#maintenance-ia3andy" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://melloware.com"><img src="https://avatars.githubusercontent.com/u/4399574?v=4?s=100" width="100px;" alt="Melloware"/><br /><sub><b>Melloware</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=melloware" title="Code">💻</a> <a href="#maintenance-melloware" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://www.phillip-kruger.com"><img src="https://avatars.githubusercontent.com/u/6836179?v=4?s=100" width="100px;" alt="Phillip Krüger"/><br /><sub><b>Phillip Krüger</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=phillip-kruger" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/rvansa"><img src="https://avatars.githubusercontent.com/u/2167869?v=4?s=100" width="100px;" alt="Radim Vansa"/><br /><sub><b>Radim Vansa</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=rvansa" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/fblan"><img src="https://avatars.githubusercontent.com/u/13745480?v=4?s=100" width="100px;" alt="Blanc Frederic"/><br /><sub><b>Blanc Frederic</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=fblan" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/andyhan"><img src="https://avatars.githubusercontent.com/u/142950?v=4?s=100" width="100px;" alt="andyhan"/><br /><sub><b>andyhan</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=andyhan" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/computerlove"><img src="https://avatars.githubusercontent.com/u/769579?v=4?s=100" width="100px;" alt="Marvin Bredal Lillehaug"/><br /><sub><b>Marvin Bredal Lillehaug</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=computerlove" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/johnaohara"><img src="https://avatars.githubusercontent.com/u/959822?v=4?s=100" width="100px;" alt="John O'Hara"/><br /><sub><b>John O'Hara</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=johnaohara" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://hollycummins.com"><img src="https://avatars.githubusercontent.com/u/11509290?v=4?s=100" width="100px;" alt="Holly Cummins"/><br /><sub><b>Holly Cummins</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=holly-cummins" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://shivams.bio.link/"><img src="https://avatars.githubusercontent.com/u/91419219?v=4?s=100" width="100px;" alt="Shivam Sharma"/><br /><sub><b>Shivam Sharma</b></sub></a><br /><a href="#infra-shivam-sharma7" title="Infrastructure (Hosting, Build-Tools, etc)">🚇</a> <a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=shivam-sharma7" title="Documentation">📖</a> <a href="#data-shivam-sharma7" title="Data">🔣</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://thejavaguy.org/"><img src="https://avatars.githubusercontent.com/u/11942401?v=4?s=100" width="100px;" alt="Ivan Milosavljević"/><br /><sub><b>Ivan Milosavljević</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=TheJavaGuy" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://sourcespy.com"><img src="https://avatars.githubusercontent.com/u/6850153?v=4?s=100" width="100px;" alt="Alex Karezin"/><br /><sub><b>Alex Karezin</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=alexkarezin" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://twitter.com/TommiSap"><img src="https://avatars.githubusercontent.com/u/6555967?v=4?s=100" width="100px;" alt="Thomas Sapelza"/><br /><sub><b>Thomas Sapelza</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3AThoSap" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/steffenvan"><img src="https://avatars.githubusercontent.com/u/22645031?v=4?s=100" width="100px;" alt="Steffen Van"/><br /><sub><b>Steffen Van</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=steffenvan" title="Documentation">📖</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://www.dubs.tech"><img src="https://avatars.githubusercontent.com/u/509379?v=4?s=100" width="100px;" alt="Paul Dubs"/><br /><sub><b>Paul Dubs</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=treo" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/devpikachu"><img src="https://avatars.githubusercontent.com/u/30475873?v=4?s=100" width="100px;" alt="Andrei Hava"/><br /><sub><b>Andrei Hava</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3Adevpikachu" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/l2c0r3"><img src="https://avatars.githubusercontent.com/u/62983504?v=4?s=100" width="100px;" alt="l2c0r3"/><br /><sub><b>l2c0r3</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3Al2c0r3" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/stevenfuhr"><img src="https://avatars.githubusercontent.com/u/26394575?v=4?s=100" width="100px;" alt="stevenfuhr"/><br /><sub><b>stevenfuhr</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3Astevenfuhr" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/trpouh"><img src="https://avatars.githubusercontent.com/u/19832039?v=4?s=100" width="100px;" alt="Leon Kirschner"/><br /><sub><b>Leon Kirschner</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=trpouh" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/mbouhda"><img src="https://avatars.githubusercontent.com/u/11915506?v=4?s=100" width="100px;" alt="mbouhda"/><br /><sub><b>mbouhda</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3Ambouhda" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/mschorsch"><img src="https://avatars.githubusercontent.com/u/4418363?v=4?s=100" width="100px;" alt="mschorsch"/><br /><sub><b>mschorsch</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3Amschorsch" title="Bug reports">🐛</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ylemoigne"><img src="https://avatars.githubusercontent.com/u/2000657?v=4?s=100" width="100px;" alt="Yann Le Moigne"/><br /><sub><b>Yann Le Moigne</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=ylemoigne" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://meow.liquidnya.art"><img src="https://avatars.githubusercontent.com/u/7364785?v=4?s=100" width="100px;" alt="Alice"/><br /><sub><b>Alice</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=liquidnya" title="Code">💻</a> <a href="#ideas-liquidnya" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/yatho"><img src="https://avatars.githubusercontent.com/u/6213245?v=4?s=100" width="100px;" alt="Yann-Thomas LE MOIGNE"/><br /><sub><b>Yann-Thomas LE MOIGNE</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=yatho" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Blarc"><img src="https://avatars.githubusercontent.com/u/36704759?v=4?s=100" width="100px;" alt="Jakob Maležič"/><br /><sub><b>Jakob Maležič</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3ABlarc" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://shamil.co.uk"><img src="https://avatars.githubusercontent.com/u/5007497?v=4?s=100" width="100px;" alt="Shamil Nunhuck"/><br /><sub><b>Shamil Nunhuck</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3AUbiquitousBear" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/geoand"><img src="https://avatars.githubusercontent.com/u/4374975?v=4?s=100" width="100px;" alt="Georgios Andrianakis"/><br /><sub><b>Georgios Andrianakis</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=geoand" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/lmagnette"><img src="https://avatars.githubusercontent.com/u/6390187?v=4?s=100" width="100px;" alt="Loïc Magnette"/><br /><sub><b>Loïc Magnette</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=lmagnette" title="Documentation">📖</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/threadlock05"><img src="https://avatars.githubusercontent.com/u/173927177?v=4?s=100" width="100px;" alt="threadlock05"/><br /><sub><b>threadlock05</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3Athreadlock05" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://developers.redhat.com/author/eric-deandrea"><img src="https://avatars.githubusercontent.com/u/363447?v=4?s=100" width="100px;" alt="Eric Deandrea"/><br /><sub><b>Eric Deandrea</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3Aedeandrea" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/alex-kovalenko1982"><img src="https://avatars.githubusercontent.com/u/69167029?v=4?s=100" width="100px;" alt="Alex Kovalenko"/><br /><sub><b>Alex Kovalenko</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=alex-kovalenko1982" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/zZHorizonZz"><img src="https://avatars.githubusercontent.com/u/60035933?v=4?s=100" width="100px;" alt="Daniel Fiala"/><br /><sub><b>Daniel Fiala</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-quinoa/commits?author=zZHorizonZz" title="Code">💻</a> <a href="https://github.com/quarkiverse/quarkus-quinoa/issues?q=author%3AzZHorizonZz" title="Bug reports">🐛</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
