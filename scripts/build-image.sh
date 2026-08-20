#!/usr/bin/env sh
set -eu

IMAGE_NAME="${IMAGE_NAME:-home-inventory}"
IMAGE_TAG="${IMAGE_TAG:-0.1.0}"
PLATFORM="${PLATFORM:-linux/amd64}"

./gradlew clean bootJar
docker build --platform "${PLATFORM}" -t "${IMAGE_NAME}:${IMAGE_TAG}" -t "${IMAGE_NAME}:latest" .
