#!/usr/bin/env python3

import sys, os, subprocess

def system(cmd, default):
    args = cmd.split()
    try:
        return subprocess.check_output(args).decode("ascii").strip()
    except:
        return default

if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(1)

    branch = system("git rev-parse --abbrev-ref HEAD", "master")
    hash = system("git log -1 --format=%H", "0" * 40)
    tag = system("git describe --dirty --always --tags", "")
    date = system("git show -s --format=%ci", "")

    # remove hash from git describe output
    tag = tag.split("-")
    if len(tag) > 2 and tag[2][1:] in hash:
        del tag[2]
    tag = "-".join(tag)

    mappings = {
        "GIT_BRANCH": branch,
        "GIT_TAG": tag,
        "GIT_COMMIT_HASH": hash,
        "GIT_COMMIT_DATE": date,
    }

    base_path = os.path.dirname(sys.argv[0])
    in_path = sys.argv[1]
    out_path = sys.argv[2]

    with open(in_path, "r") as f:
        version_str = f.read()

    for mapping, value in mappings.items():
        version_str = version_str.replace("@" + mapping + "@", value)

    with open(out_path, "w") as f:
        f.write(version_str)
