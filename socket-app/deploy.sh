if [[ -n "$2" ]] ; then
        BRANCH=$2
else
        BRANCH="master"
fi

cd /home/ts/hoylutransfer
git reset --hard origin/$BRANCH
git clean -f
git pull
git checkout $BRANCH
