#! /bin/sh

tmp=$(tempfile)
M64P_COMPONENTS="core ui-console rsp-hle audio-sdl video-rice video-glide64mk2"

for i in $M64P_COMPONENTS; do
    git remote|grep "^m64p-${i}$" > /dev/null
    if [ "$?" != "0" ]; then
        git remote add --no-tags "m64p-${i}" "https://github.com/mupen64plus/mupen64plus-${i}.git"
    fi
done

set -e

git remote update --prune

cd ..

for i in $M64P_COMPONENTS; do

    case "$i" in
    "ui-console")
        ae_module="front-end"
        ;;
    "video-rice")
        ae_module="gles2rice"
        ;;
    "video-glide64mk2")
        ae_module="gles2glide64"
        ;;
    *)
        ae_module="${i}"
        ;;
    esac

    git diff -w m64p-"${i}"/master: HEAD:jni/"${ae_module}" > "${tmp}"
    git rm -r jni/"${ae_module}"
    git read-tree -u --prefix=jni/"${ae_module}" m64p-"${i}"/master:
    git apply --index --reject --directory=jni/"${ae_module}"/ --ignore-whitespace "${tmp}"
    git commit -a -m "${ae_module} :Remove the whitespace differences between upstream" || true
done

rm -f "${tmp}"

echo "Finished"
