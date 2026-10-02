#!/usr/bin/env bash
# Cornelius.AI - Sincronização GitHub (100% Java 21)
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
java -Dfile.encoding=UTF-8 -cp "$DIR/cornelius.jar" com.cornelius.system.GitHubSyncEngine "$@"
