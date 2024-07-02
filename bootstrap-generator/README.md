##packages required to run build.sh:
`apk add binutils tar xz`

##packages to build:
`bash xfce4 xfce4-goodies pacman pulseaudio zenity wget xorg-xrandr`

how to use:
`1 - install #packages required to run build.sh.`
`2 - build #packages to build using termux-packages repo (dont forget to change package name).`
`3 - extract all .deb files to home dir and copy build.sh to home dir and run it.`
`4 - move 'libbootstrap.so to jniLibs (only arm64 currently supported).`