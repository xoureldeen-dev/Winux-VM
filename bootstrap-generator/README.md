## packages required to run build.sh:
`apk add binutils tar xz`

## packages to build:
`bash proot proot-distro xkeyboard-config pulseaudio`

### how to use:
- `1 - install #packages required to run build.sh.`
- `2 - build #packages to build using termux-packages repo (dont forget to change package name).`
- `3 - extract all .deb files to home dir and copy build.sh to home dir and run it.`
- `4 - move 'bootstrap-*-arm64.tar.gz to obb dir of the app (only arm64 currently supported).`
