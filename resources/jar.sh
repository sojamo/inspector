cd $HOME/Documents/workspace/inspector/target/classes
jar cf ../inspector.jar .
cp ../inspector.jar $HOME/Documents/Processing/libraries/inspector/library
echo "sojamo.inspector compiled on $(date)"