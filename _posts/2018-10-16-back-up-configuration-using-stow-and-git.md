---
title: Back up configuration using Git and Stow
excerpt: After I got new work laptop, I needed to transfer some configuration files to it from the old one. This introduced an opportunity to finaly start versioning it and also providing more flexible backup handle.
tags:
- git
- stow
- simple hack
---
After I got new work laptop, I needed to transfer some configuration files to it from the old one. This introduced an opportunity to finaly start versioning it and also providing more flexible backup handle. It surely is not a good idea to set version control above whole `$HOME`. When looking after solution to this problem I stumbled upon GNU Stow.

Let's say your `$HOME` looks like this:

```
.bashrc
.bash_profile
.git
.vim
.vimrc
Desktop
Documents
Downloads
...
```

You surely know know that `.bashrc` and `.bash_profile` represent Bash configuration. But how to pass this information to back up solution/versioning system when you want to separate configurations for different software?

## GNU Stow

GNU Stow is basically simple symlink management software. Let's setup directory containing all configuration we want to keep under git as a child of `$HOME` -- `configuration`. Under this directory we can create dicrectories containing per-software configuration:

```
configuration/
├── bash
├── git
└── vim
```

First, let's move configuration to corresponding directory: `mv .bash* configuration/bash/`.

What `stow` command does by default is that it takes the contents of folder which is passed as an argument and creates symlinks to this content in parent directory. When `stow bash` is called in `configuration` directory, it takes the files in `bash` directory and in `$HOME` (which is a parent of `configuration`) creates symlinks to this files.

`$HOME` then looks like this:

```
...
.bash_profile -> configuration/bash/.bash_profile
.bashrc -> configuration/bash/.bashrc
...
```

## Version it!

What lefts is using git or your preffered VCS to version this configuration. Just run `git init`, `git add .` and `git commit -m "Initial commit"` in `configuration` directory and you are all set.
 
