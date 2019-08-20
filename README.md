s3425614 - Michael Vincent

s3703700 - Zaed Ahmed


Compile and run with file location as current working directory.


# Compile

javac -cp .:lib/jopt-simple-5.0.2.jar:lib/jsoup-1.12.1.jar *.java


# Run

java -cp .:lib/jopt-simple-5.0.2.jar:lib/jsoup-1.12.1.jar index [-s <path to stoplists>] \[-p] <path to documents>

java -cp .:lib/jopt-simple-5.0.2.jar:lib/jsoup-1.12.1.jar search <path to lexicon> <path to invlists> <path to map> <queryterm> [<additional queryterm>...]
