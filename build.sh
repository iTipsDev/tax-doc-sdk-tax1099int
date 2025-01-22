echo "Build"

# Modify to align with your java runtime
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 17.0.9-zulu

mvn clean install
if [ $? -eq 0 ]
then
  echo "Success"
else
  echo "Problem"
  echo $?
  exit
fi

echo "Done build.sh"
