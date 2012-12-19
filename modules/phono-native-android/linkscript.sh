rm -rf lsrc
mkdir lsrc
HP=`pwd` export HP

echo audio
(cd ../phono-java-audio/src/java; find . -type d  -exec echo "mkdir lsrc/{}" ";") | sh 
rm -rf lsrc/com/phono/applet
rm -rf lsrc/com/phono/android/phonegap
(cd ../phono-java-audio/src/java; find . -name "*.java" -exec echo "ln -s ${HP}/../phono-java-audio/src/java/{} lsrc/{}" ";") | sh 

echo xmpp
(cd ../phono-java/src; find . -type d  -exec echo "mkdir lsrc/{}" ";") | sh 
rm -rf lsrc/com/phono/applet
(cd ../phono-java/src; find . -name "*.java" -exec echo "ln -s ${HP}/../phono-java/src/{} lsrc/{}" ";") | sh 


