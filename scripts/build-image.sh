#!/usr/bin/env sh
set -eu

usage() {
  cat <<'EOF'
Usage:
  ./scripts/build-image.sh [VERSION] [--export]

Options:
  VERSION             Docker image tag to build, e.g. 1.1.0.
                      Defaults to IMAGE_TAG or 0.1.0.
  -e, --export        Export the versioned image as a .tar file after building.
  --export-dir DIR    Directory for the exported .tar file. Defaults to current directory.
  -h, --help          Show this help.

Environment:
  IMAGE_NAME          Docker image name. Defaults to home-inventory.
  IMAGE_TAG           Docker image tag fallback when VERSION is omitted.
  PLATFORM            Docker target platform. Defaults to linux/amd64.
  TAR_FILE            Explicit output path for --export.
EOF
}

VERSION=""
EXPORT_IMAGE=false
EXPORT_DIR="${EXPORT_DIR:-.}"

while [ "$#" -gt 0 ]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    -e|--export)
      EXPORT_IMAGE=true
      shift
      ;;
    --export-dir)
      if [ "$#" -lt 2 ]; then
        echo "Missing value for --export-dir." >&2
        usage >&2
        exit 1
      fi
      EXPORT_DIR="$2"
      shift 2
      ;;
    -*)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
    *)
      if [ -n "${VERSION}" ]; then
        echo "Only one VERSION argument is allowed." >&2
        usage >&2
        exit 1
      fi
      VERSION="$1"
      shift
      ;;
  esac
done

IMAGE_NAME="${IMAGE_NAME:-home-inventory}"
IMAGE_TAG="${VERSION:-${IMAGE_TAG:-0.1.0}}"
PLATFORM="${PLATFORM:-linux/amd64}"
TAR_IMAGE_NAME="$(printf '%s' "${IMAGE_NAME}" | sed 's#[/:]#-#g')"
TAR_FILE="${TAR_FILE:-${EXPORT_DIR%/}/${TAR_IMAGE_NAME}-${IMAGE_TAG}.tar}"

./gradlew clean bootJar
docker build --platform "${PLATFORM}" -t "${IMAGE_NAME}:${IMAGE_TAG}" -t "${IMAGE_NAME}:latest" .

if [ "${EXPORT_IMAGE}" = "true" ]; then
  mkdir -p "$(dirname "${TAR_FILE}")"
  docker save "${IMAGE_NAME}:${IMAGE_TAG}" -o "${TAR_FILE}"
  echo "Exported ${IMAGE_NAME}:${IMAGE_TAG} to ${TAR_FILE}"
fi
