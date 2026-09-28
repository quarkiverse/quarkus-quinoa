#!/usr/bin/env bash
#---------------------------------------------------------------------
# Installs the latest pre-built Quarkus snapshot (999-SNAPSHOT) of the
# given Quarkus branch into the local Maven repository.
#
# The snapshots are published daily by the Quarkus Ecosystem CI:
# https://github.com/quarkusio/quarkus-ecosystem-ci/releases
#
# Requires the GitHub CLI with GH_TOKEN set.
#---------------------------------------------------------------------
set -euo pipefail

QUARKUS_BRANCH="${1:-main}"
SNAPSHOT_REPO="quarkusio/quarkus-ecosystem-ci"
DOWNLOAD_DIR="${RUNNER_TEMP:-/tmp}"

TAG=$(gh release list --repo "${SNAPSHOT_REPO}" --limit 50 --json tagName,publishedAt \
  --jq "[.[] | select(.tagName | startswith(\"maven-repo-${QUARKUS_BRANCH}-\"))] | first | .tagName // empty")

if [ -z "${TAG}" ]; then
  echo "No Quarkus snapshot release found for branch '${QUARKUS_BRANCH}'"
  exit 1
fi

echo "Downloading Quarkus snapshot ${TAG}"
gh release view "${TAG}" --repo "${SNAPSHOT_REPO}" --json body --jq .body
gh release download "${TAG}" --repo "${SNAPSHOT_REPO}" --pattern maven-repo.tar.gz --dir "${DOWNLOAD_DIR}" --clobber

mkdir -p ~/.m2/repository
tar -xzf "${DOWNLOAD_DIR}/maven-repo.tar.gz" -C ~/.m2/repository
rm -f "${DOWNLOAD_DIR}/maven-repo.tar.gz"

echo "Quarkus snapshot for branch '${QUARKUS_BRANCH}' installed"
