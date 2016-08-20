export TOSROOT="/opt/tinyos-2.x"
export TOSDIR="$TOSROOT/tos"
export CLASSPATH=`cygpath -w $TOSROOT/support/sdk/java/tinyos.jar`
export CLASSPATH="$CLASSPATH;."
export MAKERULES="$TOSROOT/support/make/Makerules"
make micaz sim
